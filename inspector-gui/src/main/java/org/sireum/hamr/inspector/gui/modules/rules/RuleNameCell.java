package org.sireum.hamr.inspector.gui.modules.rules;

import javafx.scene.control.TableCell;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class RuleNameCell extends TableCell<SessionRule, String> {

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (item == null) {
            setText(null);
        } else {
            setText(item);
        }
    }
}
