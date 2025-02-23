package org.correomqtt.gui.cell;

import org.correomqtt.business.model.Qos;
import org.correomqtt.business.provider.SettingsProvider;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

@SuppressWarnings("java:S110")
public class QosCell extends ListCell<Qos> {

    private static final Logger LOGGER = LoggerFactory.getLogger(QosCell.class);
    private final ListView<Qos> listView;

    @FXML
    private Pane mainNode;
    @FXML
    private Label nameLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private ResourceBundle resources;

    private FXMLLoader loader;

    public QosCell(ListView<Qos> listView) {
        this.listView = listView;
    }

    @Override
    protected void updateItem(Qos qos, boolean empty) {
        super.updateItem(qos, empty);

        if (empty || qos == null) {
            setText(null);
            setGraphic(null);
        } else {

            if (loader == null) {
                try {
                    loader = new FXMLLoader(QosCell.class.getResource("qosCell.fxml"),
                            ResourceBundle.getBundle("org.correomqtt.i18n", SettingsProvider.getInstance().getSettings().getCurrentLocale()));

                    loader.setController(this);
                    loader.load();

                } catch (Exception e) {
                    LOGGER.error("Exception rendering qos cell:", e);
                    setText(resources.getString("commonRowCreationError"));
                    setGraphic(null);
                    return;
                }

            }
            mainNode.prefWidthProperty().bind(listView.widthProperty().subtract(20));
            setQos(qos);
            setText(null);
            setGraphic(mainNode);
        }
    }

    private void setQos(Qos qos) {

        nameLabel.setText(qos.toString());
        descriptionLabel.setText(resources.getString(qos.getDescription()));
    }
}
