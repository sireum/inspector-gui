package org.sireum.hamr.inspector.gui.components;

import art.DataContent;
import javafx.scene.control.TableCell;
import javafx.scene.text.TextFlow;
import lombok.extern.slf4j.Slf4j;
import org.sireum.hamr.inspector.common.Msg;

/**
 * A cell which visualizes {@link art.DataContent} with matching paren highlighting.
 */
@Slf4j
public class DataContentTableCell extends TableCell<Msg, DataContent> {

    @Override
    protected void updateItem(DataContent item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            setGraphic(createTextFlow(item.toString()));
        }
    }

    private TextFlow createTextFlow(String dataContent) {
        final TextFlow textFlow = TextFlowFactory.createDefaultParenMatchingTextFlow(dataContent);

        // todo see if there is a good way to reduce the large number of listeners this creates
        // make sure height is calculated from table column width
        // https://stackoverflow.com/questions/42855724/textflow-inside-tablecell-not-correct-cell-height
        final int padding = 4;
        setPrefHeight(textFlow.prefHeight(getTableColumn().getWidth()) + padding);
        getTableColumn().widthProperty().addListener((observable, oldValue, newValue) -> {
            setPrefHeight(textFlow.prefHeight(getTableColumn().getWidth()) + padding);
        });

        return textFlow;
    }
}
