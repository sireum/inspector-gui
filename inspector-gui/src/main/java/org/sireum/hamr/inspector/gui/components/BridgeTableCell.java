package org.sireum.hamr.inspector.gui.components;

import art.Bridge;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import org.sireum.hamr.inspector.common.Msg;
import org.sireum.hamr.inspector.gui.App;

/**
 * A cell which visualizes a {@link Bridge}.
 */
public class BridgeTableCell extends TableCell<Msg, Bridge> {

    @Override
    protected void updateItem(Bridge item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            setGraphic(createBridgeLabel(item));
        }
    }

    private static Label createBridgeLabel(Bridge bridge) {
        final var label = new Label(App.getArtUtils().prettyPrint(bridge));
        final var color = App.getBridgeColoring().getColorOf(bridge);
        label.setTextFill(color);
        return label;
    }
}
