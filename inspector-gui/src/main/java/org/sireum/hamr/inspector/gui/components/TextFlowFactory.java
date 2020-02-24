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
