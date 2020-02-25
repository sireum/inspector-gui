package org.sireum.hamr.inspector.gui.components;

import art.Bridge;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import org.sireum.hamr.inspector.common.ArtUtils;
import org.sireum.hamr.inspector.common.Msg;
import org.sireum.hamr.inspector.gui.gfx.Coloring;

/**
 * A cell which visualizes a {@link Bridge}.
 */
public class BridgeTableCell extends TableCell<Msg, Bridge> {

    private final ArtUtils artUtils;
    private final Coloring<Bridge> bridgeColoring;

    public BridgeTableCell(ArtUtils artUtils, Coloring<Bridge> bridgeColoring) {
        this.artUtils = artUtils;
        this.bridgeColoring = bridgeColoring;
    }

    @Override
    protected void updateItem(Bridge item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            setGraphic(createBridgeLabel(item));
        }
    }

    private Label createBridgeLabel(Bridge bridge) {
        final var label = new Label(artUtils.prettyPrint(bridge));
        final var color = bridgeColoring.getColorOf(bridge);
        label.setTextFill(color);
        return label;
    }
}
