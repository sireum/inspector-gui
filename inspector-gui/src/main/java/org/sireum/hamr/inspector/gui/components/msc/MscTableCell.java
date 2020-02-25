package org.sireum.hamr.inspector.gui.components.msc;

import art.Bridge;
import art.UPort;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.sireum.hamr.inspector.common.ArtUtils;
import org.sireum.hamr.inspector.common.Msg;
import org.sireum.hamr.inspector.gui.components.TextFlowFactory;
import org.sireum.hamr.inspector.gui.gfx.Coloring;

import java.util.List;
import java.util.function.Supplier;

import static javafx.beans.binding.Bindings.add;
import static javafx.beans.binding.Bindings.divide;
import static org.sireum.hamr.inspector.gui.components.TextFlowFactory.getDefaultParenMatchLeftoverColorIndex;

/**
 * A cell which visualizes a {@link UPort}.
 */
@Slf4j
public class MscTableCell extends TableCell<Msg, Msg> {

    private static final Insets ALL_SIDES = new Insets(8.0, 2.0, 2.0, 2.0);
    private static final Insets LEFT_MISSING = new Insets(8.0, 2.0, 2.0, 0.0);
    private static final Insets RIGHT_MISSING = new Insets(8.0, 0.0, 2.0, 2.0);

    private static final double LINE_HEIGHT = 4.0;

    private final ArtUtils artUtils;
    private final Coloring<Bridge> bridgeColoring;

    private boolean areSourceAndDestAdjacentCallback = false;

    public MscTableCell(ArtUtils artUtils, Coloring<Bridge> bridgeColoring) {
        this.artUtils = artUtils;
        this.bridgeColoring = bridgeColoring;
    }

    /**
     * Cannot be called until this tableCell has been added to a tableColumn.
     * In this case, DO NOT CALL FROM CONSTRUCTOR.
     * @return the bridge visualized by this cell.
     */
    @NotNull
    private Bridge getBridge() {
        return (Bridge) super.getTableColumn().getUserData();
    }

    @Override
    protected void updateItem(Msg item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
        } else {
            final CellType cellType = determineCellType(item);
            setGraphicToCellType(item, cellType);
        }
    }

    private enum CellType {
        R_HEAD, L_HEAD, R_ORIGIN, L_ORIGIN, LINE, EMPTY
    }

    /*
    TODO:
        - add msg id
        - add timestamp
     */
    private void setGraphicToCellType(Msg msg, CellType cellType) {
        if (cellType == CellType.R_HEAD) {
            final Polygon rightArrowHead = createRightArrowHead();
            final String text = artUtils.prettyPrint(msg.dst());
            final String tooltipText = artUtils.informativePrettyPrint(msg.dst());
            setGraphicToLineTerminal(rightArrowHead, true, text, tooltipText, msg);

        } else if (cellType == CellType.L_HEAD) {
            final Polygon leftArrowHead = createLeftArrowHead();
            final String text = artUtils.prettyPrint(msg.dst());
            final String tooltipText = artUtils.informativePrettyPrint(msg.dst());
            setGraphicToLineTerminal(leftArrowHead, false, text, tooltipText, msg);

        } else if (cellType == CellType.R_ORIGIN) {
            final Rectangle rect = createArrowLine(0.50);
            rect.setFill(Color.BLACK);
            final String text = artUtils.prettyPrint(msg.dst());
            final String tooltipText = artUtils.informativePrettyPrint(msg.src());
            setGraphicToLineTerminal(rect, true, text, tooltipText, msg);

        } else if (cellType == CellType.L_ORIGIN) {
            final Rectangle rect = createArrowLine(0.50);
            rect.setFill(Color.BLACK);
            final String text = artUtils.prettyPrint(msg.dst());
            final String tooltipText = artUtils.informativePrettyPrint(msg.src());
            setGraphicToLineTerminal(rect, false, text, tooltipText, msg);

        } else if (cellType == CellType.LINE) {
            setGraphicToLineMiddle(msg);
        } else if (cellType == CellType.EMPTY) {
            setGraphic(null);
        }
    }

    private void setGraphicToLineTerminal(Shape shape, boolean isRight, String text, String tooltipText, Msg msg) {
        final Pos shapePos;
        final Label label = new Label(text);
        label.setTextFill(bridgeColoring.getColorOf(getBridge()));

        if (isRight) {
            shapePos = Pos.CENTER_LEFT;
            label.translateXProperty().bind(add(10.0, divide(label.widthProperty(), 2.0)));
        } else {
            shapePos = Pos.CENTER_RIGHT;
            label.translateXProperty().bind(add(-10.0, divide(label.widthProperty(), -2.0)));
        }

        label.setTooltip(new Tooltip(tooltipText));

        final StackPane pane = new StackPane(shape, label);
        StackPane.setAlignment(shape, shapePos);
        StackPane.setAlignment(label, Pos.CENTER);

        if (areSourceAndDestAdjacentCallback) {
            final Pos pos;
            final Insets insets;
            final String labelText;
            final int textFlowColorOffset;
            final TextAlignment textAlignment;

            final String fullText = msg.data().toString();

            if (isRight) {
                pos = Pos.TOP_LEFT;
                insets = LEFT_MISSING;
                labelText = fullText.substring(fullText.length() / 2);
                final String firstHalf = fullText.substring(0, fullText.length() / 2);
                textFlowColorOffset = TextFlowFactory.getDefaultParenMatchLeftoverColorIndex(firstHalf);
                textAlignment = TextAlignment.LEFT;
            } else {
                pos = Pos.TOP_RIGHT;
                insets = RIGHT_MISSING;
                labelText = fullText.substring(0, fullText.length() / 2);
                textFlowColorOffset = 0;
                textAlignment = TextAlignment.RIGHT;
            }

            addTextFlowToCell(pane, pos, labelText, fullText, insets, textAlignment, textFlowColorOffset, false);
        }

        setGraphic(pane);
    }

    private void setGraphicToLineMiddle(Msg msg) {
        final Rectangle arrowLine = createArrowLine(1.0);

        final List<TableColumn<Msg, ?>> columns = getTableView().getColumns();
        final int bridgeCount = columns.size();

        int selfIndex = -1;
        int senderIndex = -1;
        int receiverIndex = -1;

        for (int i=0; i < bridgeCount; i++) {
            final Bridge bridge = (Bridge) columns.get(i).getUserData();
            if (msg.srcBridge().equals(bridge)) {
                senderIndex = i;
            }
            if (getBridge().equals(bridge)) {
                selfIndex = i;
            }
            if (msg.dstBridge().equals(bridge)) {
                receiverIndex = i;
            }
            if (senderIndex != -1 && selfIndex != -1 && receiverIndex != -1) {
                break;
            }
        }

        final StackPane stackPane = new StackPane();

        stackPane.getChildren().add(arrowLine);
        StackPane.setAlignment(arrowLine, Pos.CENTER_LEFT);

        if (senderIndex != receiverIndex) {
            final int smallerIndex = Math.min(senderIndex, receiverIndex);
            final int biggerIndex = Math.max(senderIndex, receiverIndex);
            final int prefIndex = smallerIndex + (biggerIndex - smallerIndex) / 2;

            final String fullText = msg.data().toString();

            if ((biggerIndex - smallerIndex) % 2 == 1 && biggerIndex - smallerIndex > 2) {
                final boolean left = selfIndex == prefIndex;
                final boolean right = selfIndex == prefIndex + 1;

                if (left || right) {
                    final Pos pos;
                    final String text;
                    final Insets insets;
                    final TextAlignment textAlignment;
                    final int startPos;

                    if (left) {
                        text = fullText.substring(0, fullText.length() / 2);
                        insets = RIGHT_MISSING;
                        pos = Pos.TOP_RIGHT;
                        textAlignment = TextAlignment.RIGHT;
                        startPos = 0;
                    } else {
                        text = fullText.substring(fullText.length() / 2);
                        insets = LEFT_MISSING;
                        pos = Pos.TOP_LEFT;
                        textAlignment = TextAlignment.LEFT;
                        final String firstHalf = fullText.substring(0, fullText.length() / 2);
                        startPos = getDefaultParenMatchLeftoverColorIndex(firstHalf);
                    }

                    addTextFlowToCell(stackPane, pos, text, fullText, insets, textAlignment, startPos, false);
                }
            } else if (selfIndex == prefIndex) {
                addTextFlowToCell(stackPane, Pos.TOP_CENTER, fullText, fullText, Insets.EMPTY, TextAlignment.CENTER, 0, true);
            }
        }

        setGraphic(stackPane);
    }

    private void addTextFlowToCell(StackPane parentPane, Pos pos, String text, String tooltipText, Insets insets, TextAlignment textAlignment, int startPos, boolean scrollable) {
        final TextFlow textFlow = TextFlowFactory.createDefaultParenMatchingTextFlow(text, startPos);
        textFlow.setTextAlignment(textAlignment);
        textFlow.setMinWidth(Region.USE_PREF_SIZE);

        final Tooltip tooltip = new Tooltip(tooltipText);
        Tooltip.install(textFlow, tooltip);

        if (scrollable) {
            final ScrollPane wrapper = new ScrollPane(textFlow);
            wrapper.setFitToWidth(false);
            wrapper.setFitToHeight(false);
            wrapper.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            wrapper.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

            StackPane.setMargin(wrapper, insets);
            StackPane.setAlignment(wrapper, pos);

            // allow scrolling only when focused, and add border to indicate if a cell is focused or not
            wrapper.setFocusTraversable(false);
            wrapper.pannableProperty().bind(wrapper.focusedProperty());

            final var borderObservable = Bindings.when(wrapper.focusedProperty())
                    .then(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.DOTTED, null, BorderStroke.DEFAULT_WIDTHS)))
                    .otherwise(Border.EMPTY);

            final var allowScrollingObservable = Bindings.when(wrapper.focusedProperty())
                    .then(1.0) // 1.0 = 100%
                    .otherwise(wrapper.vminProperty());

            wrapper.hmaxProperty().bind(allowScrollingObservable);
            wrapper.vmaxProperty().bind(allowScrollingObservable);
            wrapper.borderProperty().bind(borderObservable);

            // add pane at position 0 so it is behind everything else (specifically the arrow line)
            parentPane.getChildren().add(0, wrapper);
        } else {
            StackPane.setMargin(textFlow, insets);
            StackPane.setAlignment(textFlow, pos);
            parentPane.getChildren().add(0, textFlow);
        }
    }

    private CellType determineCellType(Msg msg) {
        final List<TableColumn<Msg, ?>> columns = getTableView().getColumns();
        final int bridgeCount = columns.size();

        int selfIndex = -1;
        int senderIndex = -1;
        int receiverIndex = -1;

        for (int i=0; i < bridgeCount; i++) {
            final Bridge bridge = (Bridge) columns.get(i).getUserData();
            if (msg.srcBridge().equals(bridge)) {
                senderIndex = i;
            }
            if (getBridge().equals(bridge)) {
                selfIndex = i;
            }
            if (msg.dstBridge().equals(bridge)) {
                receiverIndex = i;
            }
            if (senderIndex != -1 && selfIndex != -1 && receiverIndex != -1) {
                break;
            }
        }

        areSourceAndDestAdjacentCallback = Math.abs(senderIndex - receiverIndex) == 1;

        if (senderIndex < receiverIndex) { // if left arrow
            if (selfIndex == senderIndex) {
                return CellType.L_ORIGIN;
            } else if (selfIndex == receiverIndex) {
                return CellType.R_HEAD;
            } else if (senderIndex < selfIndex && selfIndex < receiverIndex) {
                return CellType.LINE;
            } else {
                return CellType.EMPTY;
            }
        } else { // if right arrow
            if (selfIndex == senderIndex) {
                return CellType.R_ORIGIN;
            } else if (selfIndex == receiverIndex) {
                return CellType.L_HEAD;
            } else if (receiverIndex < selfIndex && selfIndex < senderIndex) {
                return CellType.LINE;
            } else {
                return CellType.EMPTY;
            }
        }
    }

    /**
     * Creates an arrow body (rectangle) centered in a box of size (height, width).
     *
     * @return
     */
    private Rectangle createArrowLine(double fillX) {
        final double h = LINE_HEIGHT;
        final double w = Msc.COLUMN_WIDTH * fillX;
        final double startY = (getHeight() - h) * 0.5;
        return new Rectangle(0, startY, w, h);
    }

    /*
     *
     *           {          w (total width)         }
     *
     *                                     | \             }
     *            {          lw          } |   \            \
     *           --------------------------      \           |
     *       {   |                                  \        |
     *      lh   |                                   |       h (total height = arrow height)
     *       {   |                                  /        |
     *           --------------------------      /           |
     *                                   { |   /            /
     *                     aj = (h-lh)/2 { | /             }
     *
     *                                     {   aw    }
     */
    private static final double[] RIGHT_ARROW_HEAD_POINTS = ((Supplier<double[]>) () -> {
        final double h = Msc.ROW_HEIGHT * 0.45; // total height
        final double w = Msc.COLUMN_WIDTH * 0.5; // total width
        final double lh = LINE_HEIGHT; // line height (not including arrow part)
        final double lw = w * 0.70; // line width (not including arrow part)
        final double aj = (h - lh) / 2.0; // "arrow jut" or how much sticks from either side compared to the line

        return new double[]{
                0.0, aj,
                lw, aj,
                lw, 0.0,
                w, h / 2.0,
                lw, h,
                lw, h - aj,
                0.0, h - aj
        };
    }).get();

    private static final double[] LEFT_ARROW_HEAD_POINTS = ((Supplier<double[]>) () -> {
        final double h = Msc.ROW_HEIGHT * 0.45; // total height
        final double w = Msc.COLUMN_WIDTH * 0.5; // total width
        final double lh = LINE_HEIGHT; // line height (not including arrow part)
        final double lw = w * 0.70; // line width (not including arrow part)
        final double aw = w - lw; // arrow/head width
        final double aj = (h - lh) / 2.0; // "arrow jut" or how much sticks from either side compared to the line

        return new double[] {
                w, aj,
                aw, aj,
                aw, 0.0,
                0.0, h / 2.0,
                aw, h,
                aw, h - aj,
                w,  h - aj
        };
    }).get();

    /**
     * Creates a right-facing arrow head (triangle) on the lhs of a box of size (height, width).
     *
     * @return
     */
    private static Polygon createRightArrowHead() {
        return new Polygon(RIGHT_ARROW_HEAD_POINTS);
    }

    /**
     * Creates a left-facing arrow head (triangle) on the rhs of a box of size (height, width).
     *
     * @return
     */
    private static Polygon createLeftArrowHead() {
        return new Polygon(LEFT_ARROW_HEAD_POINTS);
    }
}
