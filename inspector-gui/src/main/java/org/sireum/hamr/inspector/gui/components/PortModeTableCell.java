package org.sireum.hamr.inspector.gui.components;

import art.PortMode;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import org.sireum.hamr.inspector.common.Msg;
import org.sireum.hamr.inspector.gui.App;
import org.sireum.hamr.inspector.gui.gfx.Coloring;
import scala.collection.Iterator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A cell which visualizes a {@link PortMode}.
 */
public class PortModeTableCell extends TableCell<Msg, PortMode.Type> {

    private static final List<PortMode.Type> portModeTypes = ((Supplier<List<PortMode.Type>>) () -> {
        final List<PortMode.Type> types = new ArrayList<>();
        final Iterator<PortMode.Type> iterator = PortMode.elements().elements().toIterator();
        while (iterator.hasNext()) {
            types.add(iterator.next());
        }
        return types;
    }).get();

    // repeat after 2 colors
    private static final Coloring<PortMode.Type> itemColoring =
            Coloring.ofUniformlyDistantColors(portModeTypes, App.COLOR_SCHEME_HUE_OFFSET + 0.75f,
                    App.COLOR_SCHEME_SATURATION, App.COLOR_SCHEME_BRIGHTNESS);

    @Override
    protected void updateItem(PortMode.Type item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            setGraphic(createPortModeLabel(item));
        }
    }

    private static Label createPortModeLabel(PortMode.Type type) {
        final var label = new Label(type.toString());
        final var color = itemColoring.getColorOf(type);
        label.setTextFill(color);
        return label;
    }
}
