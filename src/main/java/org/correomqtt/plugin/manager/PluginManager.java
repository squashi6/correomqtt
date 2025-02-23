package org.correomqtt.plugin.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.correomqtt.business.model.HooksDTO;
import org.correomqtt.business.provider.PluginConfigProvider;
import org.correomqtt.plugin.spi.BaseExtensionPoint;
import org.correomqtt.plugin.spi.DetailViewManipulatorHook;
import org.correomqtt.plugin.spi.ExtensionId;
import org.correomqtt.plugin.spi.IncomingMessageHook;
import org.correomqtt.plugin.spi.MessageValidatorHook;
import org.correomqtt.plugin.spi.OutgoingMessageHook;
import org.pf4j.ExtensionFactory;
import org.pf4j.JarPluginManager;
import org.pf4j.ManifestPluginDescriptorFinder;
import org.pf4j.PluginDescriptorFinder;
import org.pf4j.PluginFactory;
import org.pf4j.PluginLoader;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class PluginManager extends JarPluginManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginManager.class);

    private static PluginManager instance;

    private PluginManager() {
        // private constructor
        super(Path.of(PluginConfigProvider.getInstance().getPluginPath()));
    }

    @Override
    protected PluginFactory createPluginFactory() {
        return new PermissionPluginFactory();
    }

    @Override
    protected PluginLoader createPluginLoader() {
        // load only jar plugins
        return new PermissionJarPluginLoader(this);
    }

    @Override
    protected PluginDescriptorFinder createPluginDescriptorFinder() {
        // read plugin descriptor from jar's manifest
        return new ManifestPluginDescriptorFinder();
    }

    @Override
    protected ExtensionFactory createExtensionFactory() {
        return new PluginExtensionFactory();
    }

    public static PluginManager getInstance() {
        if (instance == null) {
            instance = new PluginManager();
        }
        return instance;
    }

    // TODO obsolete ?
    public static void resetInstance() {
        instance = new PluginManager();
    }

    public List<OutgoingMessageHook> getOutgoingMessageHooks() {
        return PluginConfigProvider.getInstance().getOutgoingMessageHooks()
                .stream()
                .map(extensionDefinition -> {
                    OutgoingMessageHook extension = getExtensionById(OutgoingMessageHook.class,
                            extensionDefinition.getPluginId(),
                            extensionDefinition.getId());
                    enrichExtensionWithConfig(extension, extensionDefinition.getConfig());
                    return extension;
                })
                .collect(Collectors.toList());
    }

    public List<IncomingMessageHook> getIncomingMessageHooks() {
        return PluginConfigProvider.getInstance().getIncomingMessageHooks()
                .stream()
                .map(extensionDefinition -> {
                    IncomingMessageHook extension = getExtensionById(IncomingMessageHook.class,
                            extensionDefinition.getPluginId(),
                            extensionDefinition.getId());
                    enrichExtensionWithConfig(extension, extensionDefinition.getConfig());
                    return extension;
                })
                .collect(Collectors.toList());
    }

    public List<DetailViewManipulatorTask> getDetailViewManipulatorTasks() {
        return PluginConfigProvider.getInstance().getDetailViewTasks()
                .stream()
                .map(detailViewTaskDefinition -> {
                    List<DetailViewManipulatorHook> hooks = detailViewTaskDefinition.getExtensions().stream()
                            .map(extensionDefinition -> {
                                String pluginId = extensionDefinition.getPluginId();
                                String extensionId = extensionDefinition.getId();
                                DetailViewManipulatorHook extension = getExtensionById(DetailViewManipulatorHook.class, pluginId, extensionId);
                                if (extension == null) {
                                    LOGGER.warn("Plugin extension {}:{} in detailViewTasks is configured, but does not exist.", pluginId, extensionId);
                                    return null;
                                }
                                enrichExtensionWithConfig(extension, extensionDefinition.getConfig());
                                return extension;
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    return DetailViewManipulatorTask.builder()
                            .name(detailViewTaskDefinition.getName())
                            .hooks(hooks)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<MessageValidatorHook<?>> getMessageValidators(String topic) {
        return PluginConfigProvider.getInstance().getMessageValidators()
                .stream()
                .filter(validatorDefinition -> validatorDefinition.getTopic().equals(topic))
                .map(validatorDefinition -> validatorDefinition.getExtensions().stream()
                        .map(extensionDefinition -> {
                            String pluginId = extensionDefinition.getPluginId();
                            String extensionId = extensionDefinition.getId();
                            MessageValidatorHook<?> extension = getExtensionById(MessageValidatorHook.class, pluginId, extensionId);
                            if (extension == null) {
                                LOGGER.warn("Plugin extension {}:{} in messageValidators is configured, but does not exist.", pluginId, extensionId);
                                return null;
                            }
                            enrichExtensionWithConfig(extension, extensionDefinition.getConfig());
                            return extension;
                        }).collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private <T> void enrichExtensionWithConfig(BaseExtensionPoint<T> extension, JsonNode configNode) {
        try {
            Class<T> configClass = extension.getConfigClass();
            if (configClass != null) {
                extension.onConfigReceived(new ObjectMapper().treeToValue(configNode, configClass));
            }
        } catch (JsonProcessingException e) {
            LOGGER.error("Exception parsing plugin configuration object for {}", extension.getConfigClass());
        }
    }

    public <P extends BaseExtensionPoint<T>, T> P getExtensionByIdWithConfig(Class<P> type, String pluginId, String extensionId, T config) {
        P extension = getExtensionById(type, pluginId, extensionId);
        if (extension != null) {
            extension.onConfigReceived(config);
        }
        return extension;
    }

    public <P extends BaseExtensionPoint<T>, T> P getExtensionById(Class<P> type, String pluginId, String extensionId) {
        return super.getExtensions(type, pluginId)
                .stream()
                .filter(e -> isExtensionIdResolved(e, extensionId))
                .findFirst()
                .orElseGet(() -> {
                    logInvalidPluginDeclaration(type, pluginId, extensionId);
                    return null;
                });
    }

    @Override
    public <T> List<T> getExtensions(Class<T> type, String pluginId) {
        return getExtensions(type);
    }

    private <T> boolean isExtensionIdResolved(T e, String id) {
        if (e.getClass().isAnnotationPresent(ExtensionId.class)) {
            return e.getClass().getAnnotation(ExtensionId.class).value().equals(id);
        } else return true;
    }

    private <T> void logInvalidPluginDeclaration(Class<T> type, String pluginId, String extensionId) {
        Optional<T> defaultExtension = super.getExtensions(type, pluginId).stream().findFirst();
        if (defaultExtension.isPresent()) {
            if (extensionId == null) {
                LOGGER.info("Plugin {} declared for {} offers multiple valid extensions, please specify an extensionId", pluginId, type.getSimpleName());
            } else {
                LOGGER.info("Plugin {} declared for {} has no extension named: {}", pluginId, type.getSimpleName(), extensionId);
            }
        } else {
            PluginWrapper pluginWrapper = getPlugin(pluginId);

            if (pluginWrapper != null && getPlugin(pluginId).getPluginState().equals(PluginState.STARTED)) {
                LOGGER.warn("Plugin {} declared for {} has no valid extension", pluginId, type.getSimpleName());
            } else {
                LOGGER.warn("Plugin {} declared for {} is not started", pluginId, type.getSimpleName());
            }
        }
    }

    @Override
    public void unloadPlugins() {
        LOGGER.debug("Unload Plugins");
        List<String> pluginIds = resolvedPlugins.stream().map(PluginWrapper::getPluginId).collect(Collectors.toList());
        for (String pluginId : pluginIds) {
            LOGGER.debug("Unload Plugin \"{}\"", pluginId);
            unloadPlugin(pluginId);
        }

    }

    public <P extends BaseExtensionPoint<T>, T> P getExtensionByDefinition(Class<P> clazz, HooksDTO.Extension extensionDefinition) {

        P extension = PluginManager.getInstance()
                .getExtensionById(clazz, extensionDefinition.getPluginId(), extensionDefinition.getId());
        enrichExtensionWithConfig(extension, extensionDefinition.getConfig());
        return extension;
    }

    @SuppressWarnings("unchecked")
    public <P extends BaseExtensionPoint<T>, T> P getExtensionByDefinition(TypeReference<P> type, HooksDTO.Extension extensionDefinition) {

        // https://stackoverflow.com/a/28615143
        Class<P> clazz = (Class<P>) (type.getType() instanceof ParameterizedType ? ((ParameterizedType) type.getType()).getRawType() : type.getType());

        return getExtensionByDefinition(clazz, extensionDefinition);
    }
}
