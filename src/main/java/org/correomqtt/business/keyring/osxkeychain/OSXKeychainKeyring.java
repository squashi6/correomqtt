package org.correomqtt.business.keyring.osxkeychain;

import net.east301.keyring.BackendNotSupportedException;
import net.east301.keyring.PasswordRetrievalException;
import net.east301.keyring.PasswordSaveException;
import net.east301.keyring.osx.OSXKeychainBackend;
import org.correomqtt.business.keyring.BaseKeyring;
import org.correomqtt.business.keyring.KeyringException;
import org.correomqtt.business.provider.SettingsProvider;
import org.correomqtt.plugin.spi.KeyringHook;
import org.pf4j.Extension;

import java.util.ResourceBundle;

@Extension
public class OSXKeychainKeyring extends BaseKeyring implements KeyringHook {

    private static final String SERVICE_NAME = "CorreoMQTT";
    public static final String FAILED_TO_RETRIEVE_PASSWORD_FROM_OSX_KEYCHAIN = "Failed to retrieve password from osx keychain.";

    private ResourceBundle resources = ResourceBundle.getBundle("org.correomqtt.i18n", SettingsProvider.getInstance().getSettings().getCurrentLocale());

    @Override
    public String getPassword(String label) {
        OSXKeychainBackend keychainBackend = new OSXKeychainBackend();
        try {
            keychainBackend.setup();
            return keychainBackend.getPassword(SERVICE_NAME, label);
        } catch (PasswordRetrievalException e) {
            return null;
        } catch (BackendNotSupportedException e) {
            throw new KeyringException(FAILED_TO_RETRIEVE_PASSWORD_FROM_OSX_KEYCHAIN, e);
        }
    }

    @Override
    public void setPassword(String label, String password) {
        OSXKeychainBackend keychainBackend = new OSXKeychainBackend();
        try {
            keychainBackend.setup();
            keychainBackend.setPassword(SERVICE_NAME, label, password);
        } catch (BackendNotSupportedException | PasswordSaveException e) {
            throw new KeyringException(FAILED_TO_RETRIEVE_PASSWORD_FROM_OSX_KEYCHAIN, e);
        }
    }

    @Override
    public boolean isSupported() {
        return new OSXKeychainBackend().isSupported();
    }

    @Override
    public String getIdentifier() {
        return "OSXKeychain";
    }

    @Override
    public String getName() {
        return resources.getString("osxKeychainName");
    }

    @Override
    public String getDescription() {
        return resources.getString("osxKeychainDescription");
    }
}
