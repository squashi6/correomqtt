package org.correomqtt.plugin.spi;

import org.correomqtt.plugin.model.LwtConnectionExtensionDTO;
import javafx.scene.layout.HBox;

public interface LwtSettingsHook extends BaseExtensionPoint<Object> {

    void onAddItemsToLwtSettingsBox(OnSettingsChangedListener settingsChangeListener, HBox pluginWidgetArea);

    LwtConnectionExtensionDTO onLoadConnection(LwtConnectionExtensionDTO activeConnectionConfigDTO);

    LwtConnectionExtensionDTO onShowConnection(LwtConnectionExtensionDTO activeConnectionConfigDTO);

    LwtConnectionExtensionDTO onSaveConnection(LwtConnectionExtensionDTO activeConnectionConfigDTO);

    LwtConnectionExtensionDTO onUnloadConnection(LwtConnectionExtensionDTO activeConnectionConfigDTO);

    interface OnSettingsChangedListener {
        void setDirty(boolean dirty);
    }
}
