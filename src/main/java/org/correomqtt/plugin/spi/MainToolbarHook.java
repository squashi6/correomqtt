package org.correomqtt.plugin.spi;

import javafx.scene.layout.HBox;

public interface MainToolbarHook extends BaseExtensionPoint<Object> {

    void onInstantiateMainToolbar(String connectionId, HBox controllViewButtonHBox, int indexToInsert);

}
