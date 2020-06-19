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

package org.sireum.hamr.inspector.gui.modules.msc;

import art.Bridge;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sireum.hamr.inspector.common.ArtUtils;
import org.sireum.hamr.inspector.common.Filter;
import org.sireum.hamr.inspector.common.Msg;
import org.sireum.hamr.inspector.gui.ViewController;
import org.sireum.hamr.inspector.gui.collections.UnbackedObservableList;
import org.sireum.hamr.inspector.gui.components.msc.MscTableCell;
import org.sireum.hamr.inspector.gui.gfx.Coloring;
import org.sireum.hamr.inspector.gui.modules.DisposableTabController;
import org.sireum.hamr.inspector.services.MsgService;
import org.sireum.hamr.inspector.services.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.Disposable;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@ViewController
@NoArgsConstructor
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection") // <-- protection for generated classes
public final class MscTab implements DisposableTabController {

    static final double ROW_HEIGHT = 80.0;
    static final double COLUMN_WIDTH = 200.0;

    @Getter
    private final double columnWidth = COLUMN_WIDTH;

    @Getter
    private final double rowHeight = ROW_HEIGHT; // accessed by msc.fxml

    @Getter
    @Autowired
    @Qualifier("filters")
    private ObservableList<Filter> filters;

    @Getter
    @Autowired
    @Qualifier("sessions")
    private ObservableList<Session> sessions;

    @Autowired
    private MsgService msgService;

    @Autowired
    @Qualifier("artUtils")
    private ArtUtils artUtils;

    @Autowired
    @Qualifier("bridgeColoring")
    private Coloring<Bridge> bridgeColoring;

    @FXML
    private TableView<Msg> tableView;

    @FXML
    public ComboBox<Session> sessionComboBox;

    @FXML
    public ComboBox<Filter> filterComboBox;

    private final AtomicReference<Disposable> streamDisposable = new AtomicReference<>(null);

//    private final ChangeListener<Disposable> subscriptionChangeListener = (observable, oldValue, newValue) -> {
//        tableView.getItems().clear();
//        if (oldValue != null && !oldValue.isDisposed()) {
//            oldValue.dispose();
//        }
//        streamDisposable.getAndSet(newValue);
//    };

    @SuppressWarnings("FieldCanBeLocal") // this property MUST be a field to avoid being GC'd as a weak reference
    private ObjectBinding<ObservableList<Msg>> itemsBinding = null;

    @FXML
    protected void initialize() {
        initTableStructure();
        initTableContent();
    }

    @Override
    public void dispose() {
        streamDisposable.updateAndGet(disposable -> {
            if (disposable != null && !disposable.isDisposed()) {
                log.info("disposing of mscTab's stream subscription");
                disposable.dispose();
            }
            return null;
        });
    }

    private void initTableStructure() {
        for (Bridge bridge : artUtils.getBridges()) {
            final var column = new TableColumn<Msg, Msg>(artUtils.prettyPrint(bridge));

            column.setUserData(bridge); // user data MUST be set to bridge
            column.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
            column.setCellFactory(col -> new MscTableCell(artUtils, bridgeColoring));
            column.setPrefWidth(columnWidth);

            column.setResizable(false);
            column.setReorderable(true);
            column.setEditable(false);

            column.setStyle("-bridge-color: #" + bridgeColoring.getRgbStringOf(bridge) + ";");

            // if this is changed, must also remove the line: tableView.setSelectionModel(null) below.
            // see: https://stackoverflow.com/questions/27354085/disable-row-selection-in-tableview
            column.setSortable(false);

            tableView.getColumns().add(column);
        }

        tableView.setOpaqueInsets(Insets.EMPTY);

        // this can cause NPE if table is sorted, but column.setSortable(false) is called for each column
        // if sorting is needed, use css from: https://stackoverflow.com/questions/27354085/disable-row-selection-in-tableview
        tableView.setSelectionModel(null);
    }

    private void initTableContent() {
        itemsBinding = Bindings.createObjectBinding(() -> {
            final Session session = sessionComboBox.getValue();
            final Filter filter = filterComboBox.getValue();

            if (session != null && filter != null) {
                return new UnbackedObservableList(artUtils, msgService, session, filter);
            } else {
                return FXCollections.emptyObservableList();
            }
        }, sessionComboBox.valueProperty(), filterComboBox.valueProperty());
        tableView.itemsProperty().bind(itemsBinding);
    }

}
