/*
 * Copyright (c) 2020, Matthew Weis, Kansas State University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
