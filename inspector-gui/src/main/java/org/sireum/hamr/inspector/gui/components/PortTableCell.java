package org.sireum.hamr.inspector.gui.components;

import art.Bridge;
import art.UPort;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import org.sireum.hamr.inspector.common.ArtUtils;
import org.sireum.hamr.inspector.common.Msg;
import org.sireum.hamr.inspector.gui.gfx.Coloring;

/**
 * A cell which visualizes a {@link UPort}.
 */
public class PortTableCell extends TableCell<Msg, UPort> {

    private final ArtUtils artUtils;
    private final Coloring<Bridge> bridgeColoring;

    public PortTableCell(ArtUtils artUtils, Coloring<Bridge> bridgeColoring) {
        this.artUtils = artUtils;
        this.bridgeColoring = bridgeColoring;
    }

    @Override
    protected void updateItem(UPort item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            setGraphic(createPortLabel(item));
        }
    }

    private Label createPortLabel(UPort port) {
        final var label = new Label(artUtils.prettyPrint(port));
        final var color = bridgeColoring.getColorOf(artUtils.getBridge(port));
        label.setTextFill(color);
        return label;
    }
}
