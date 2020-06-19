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

import art.DispatchPropertyProtocol;
import javafx.scene.control.TableCell;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.sireum.hamr.inspector.common.Msg;
import org.sireum.hamr.inspector.gui.App;
import org.sireum.hamr.inspector.gui.gfx.Coloring;

import java.util.ArrayList;
import java.util.List;

/**
 * A cell which visualizes a {@link DispatchPropertyProtocol} with matching paren highlighting.
 */
public class DispatchPropertyProtocolTableCell extends TableCell<Msg, DispatchPropertyProtocol> {

    // repeat after 2 colors
    private static final int parensColorCount = 2;
    private static final Coloring<Integer> parensColoring =
            Coloring.ofUniformlyDistantColors(listOfRange(0, parensColorCount-1),
                    App.COLOR_SCHEME_HUE_OFFSET + 0.45f,
                    App.COLOR_SCHEME_SATURATION,
                    App.COLOR_SCHEME_BRIGHTNESS);

    @Override
    protected void updateItem(DispatchPropertyProtocol item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            setGraphic(createTextFlow(item));
        }
    }

    private TextFlow createTextFlow(DispatchPropertyProtocol dataContent) {
        final var textFlow = new TextFlow();
        final var s = dataContent.toString();

        int col = 0;
        int lastIndex = 0;

        for (int i=0; i < s.length(); i++) {
            final char c = s.charAt(i);

            if (c == '(') {
                final var text = s.substring(lastIndex, i + 1);
                final Text node = new Text(text);
                node.setFill(parensColoring.getColorOf(col % parensColorCount));
                textFlow.getChildren().add(node);
                lastIndex = i + 1; // just in case multiple open/close parens
                col++;
            } else if (c == ')') {
                final var text = s.substring(lastIndex, i);
                final Text node = new Text(text);
                node.setFill(parensColoring.getColorOf(col % parensColorCount));
                textFlow.getChildren().add(node);
                lastIndex = i; // just in case multiple open/close parens
                col--;
                col = Math.max(col, 0);
            }
        }
        if (lastIndex < s.length()) {
            final var text = s.substring(lastIndex);
            final Text node = new Text(text);
            node.setFill(parensColoring.getColorOf(0));
            textFlow.getChildren().add(node);
        }

        // todo see if there is a good way to reduce the large number of listeners this creates
        // make sure height is calculated from table column width
        // https://stackoverflow.com/questions/42855724/textflow-inside-tablecell-not-correct-cell-height
        final int padding = 0;
        setPrefHeight(textFlow.prefHeight(getTableColumn().getWidth()) + padding);
        getTableColumn().widthProperty().addListener((observable, oldValue, newValue) -> {
            setPrefHeight(textFlow.prefHeight(getTableColumn().getWidth()) + padding);
        });

        return textFlow;
    }

    private static List<Integer> listOfRange(int start, int end) {
        final List<Integer> list = new ArrayList<>(end - start);
        for (int i=start; i <= end; i++) {
            list.add(i);
        }
        return list;
    }
}
