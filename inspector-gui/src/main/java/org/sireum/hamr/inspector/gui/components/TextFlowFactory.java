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

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.jetbrains.annotations.NotNull;
import org.sireum.hamr.inspector.gui.App;
import org.sireum.hamr.inspector.gui.gfx.Coloring;

import java.util.List;

public class TextFlowFactory {

    // repeat after 4 colors
    public static final Coloring<Integer> PARENS_COLORING =
            Coloring.ofUniformlyDistantColors(List.of(0, 1, 2, 3),
                    App.COLOR_SCHEME_HUE_OFFSET + 0.15f,
                    App.COLOR_SCHEME_SATURATION,
                    App.COLOR_SCHEME_BRIGHTNESS);

    @NotNull
    public static TextFlow createDefaultParenMatchingTextFlow(@NotNull String content) {
        return createParenMatchingTextFlow(content, PARENS_COLORING, 0);
    }

    @NotNull
    public static TextFlow createDefaultParenMatchingTextFlow(@NotNull String content, int numColors) {
        return createParenMatchingTextFlow(content, PARENS_COLORING, numColors);
    }

    @NotNull
    public static TextFlow createParenMatchingTextFlow(@NotNull String content, @NotNull Coloring<Integer> coloring) {
        return createParenMatchingTextFlow(content, coloring, 0);
    }

    public static int getDefaultParenMatchLeftoverColorIndex(@NotNull String content) {
        return getParenMatchLeftoverColorIndex(content, PARENS_COLORING.getColorCount());
    }

    public static int getParenMatchLeftoverColorIndex(@NotNull String content, int numColors) {
        int col = 0;
        int lastIndex = 0;

        for (int i = 0; i < content.length(); i++) {
            final char c = content.charAt(i);
            if (c == '(') {
                lastIndex = i + 1; // just in case multiple open/close parens
                col++;
            } else if (c == ')') {
                final var text = content.substring(lastIndex, i);
                final boolean immediatelyClosedParen = text.length() == 0;
                if (immediatelyClosedParen) {
                    col--;
                    col = Math.max(col, 0); // protection against unmatched parens
                }

                lastIndex = i; // just in case multiple open/close parens

                if (!immediatelyClosedParen) {
                    col--;
                    col = Math.max(col, 0); // protection against unmatched parens
                }
            }
        }

        return col % numColors;
    }

    @NotNull
    public static TextFlow createParenMatchingTextFlow(@NotNull String content, @NotNull Coloring<Integer> coloring, int initialColor) {
        final var textFlow = new TextFlow();
        final int parensColorCount = coloring.getColorCount();

        int col = initialColor % parensColorCount;
        int lastIndex = 0;

        for (int i = 0; i < content.length(); i++) {
            final char c = content.charAt(i);
            if (c == '(') {
                final var text = content.substring(lastIndex, i + 1);
                final Text node = new Text(text);
                node.setFill(coloring.getColorOf(col % parensColorCount));
                textFlow.getChildren().add(node);
                lastIndex = i + 1; // just in case multiple open/close parens
                col++;
            } else if (c == ')') {
                final var text = content.substring(lastIndex, i);
                final boolean immediatelyClosedParen = text.length() == 0;
                if (immediatelyClosedParen) {
                    col--;
                    col = Math.max(col, 0); // protection against unmatched parens
                }

                final Text node = new Text(text);
                node.setFill(coloring.getColorOf(col % parensColorCount));
                textFlow.getChildren().add(node);
                lastIndex = i; // just in case multiple open/close parens

                if (!immediatelyClosedParen) {
                    col--;
                    col = Math.max(col, 0); // protection against unmatched parens
                }
            }
        }
        if (lastIndex < content.length()) {
            final var text = content.substring(lastIndex);
            final Text node = new Text(text);
            node.setFill(coloring.getColorOf(col % parensColorCount));
            textFlow.getChildren().add(node);
        }

        return textFlow;
    }

}
