package org.sireum.hamr.inspector.gui.components;

import art.UPort;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import org.sireum.hamr.inspector.common.Msg;
import org.sireum.hamr.inspector.gui.App;

/**
 * A cell which visualizes a {@link UPort}.
 */
public class PortTableCell extends TableCell<Msg, UPort> {

    @Override
    protected void updateItem(UPort item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            setGraphic(createPortLabel(item));
        }
    }

    private static Label createPortLabel(UPort port) {
        final var label = new Label(App.getArtUtils().prettyPrint(port));
        final var color = App.getBridgeColoring().getColorOf(App.getArtUtils().getBridge(port));
        label.setTextFill(color);
        return label;
    }
}
