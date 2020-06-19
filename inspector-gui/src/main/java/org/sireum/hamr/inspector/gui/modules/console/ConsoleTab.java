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
package org.sireum.hamr.inspector.gui.modules.console;

import art.*;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.CheckTreeView;
import org.controlsfx.control.PopOver;
import org.jetbrains.annotations.NotNull;
import org.sireum.hamr.inspector.common.ArtUtils;
import org.sireum.hamr.inspector.common.Filter;
import org.sireum.hamr.inspector.common.Msg;
import org.sireum.hamr.inspector.gui.ViewController;
import org.sireum.hamr.inspector.gui.collections.UnbackedObservableList;
import org.sireum.hamr.inspector.gui.components.*;
import org.sireum.hamr.inspector.gui.gfx.Coloring;
import org.sireum.hamr.inspector.gui.modules.DisposableTabController;
import org.sireum.hamr.inspector.services.MsgService;
import org.sireum.hamr.inspector.services.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.Disposable;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@Slf4j
@ViewController
@NoArgsConstructor
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection") // <-- protection for generated classes
public final class ConsoleTab implements DisposableTabController {

    // todo add stylesheet coloring, see: https://stackoverflow.com/questions/21113253/how-to-style-a-table-cell-in-javafx2-using-css-without-removing-hover-selection
//    private static final Color SRC_COLOR = Color.LIGHTSTEELBLUE.brighter().brighter().desaturate().desaturate();
//    private static final String SRC_STYLE_PROPERTY = "-fx-control-inner-background: #e5f0ffff; -fx-control-inner-background-alt: #edf5ffff;";
//    private static final Color DST_COLOR = Color.LIGHTYELLOW.brighter().brighter().desaturate().desaturate();
//    private static final String DST_STYLE_PROPERTY = "-fx-control-inner-background: #fffff0ff; -fx-control-inner-background-alt: #fffff4ff;";


    @Getter
    @Autowired
    @Qualifier("filters")
    private ObservableList<Filter> filters;

    @Getter
    @Autowired
    @Qualifier("sessions")
    private ObservableList<Session> sessions;

    @Autowired
    @Qualifier("artUtils")
    private ArtUtils artUtils;

    @Autowired
    @Qualifier("bridgeColoring")
    private Coloring<Bridge> bridgeColoring;

    @Autowired
    private MsgService msgService;

    @FXML
    public TableView<Msg> tableView;

    @FXML
    public TableColumn<Msg, Long> uuidTableColumn;

    @FXML
    public TableColumn<Msg, Bridge> srcBridgeColumn;

    @FXML
    public TableColumn<Msg, UPort> srcPortColumn;

    @FXML
    public TableColumn<Msg, DispatchPropertyProtocol> srcDispatchProtocolColumn;

    @FXML
    public TableColumn<Msg, PortMode.Type> srcColumn;

    @FXML
    public TableColumn<Msg, PortMode.Type> srcPortModeColumn;

    @FXML
    public TableColumn<Msg, PortMode.Type> dstColumn;

    @FXML
    public TableColumn<Msg, Bridge> dstBridgeColumn;

    @FXML
    public TableColumn<Msg, UPort> dstPortColumn;

    @FXML
    public TableColumn<Msg, DispatchPropertyProtocol> dstDispatchProtocolColumn;

    @FXML
    public TableColumn<Msg, PortMode.Type> dstPortModeColumn;

    @FXML
    public TableColumn<Msg, DataContent> dataContentTableColumn;

    @FXML
    public TableColumn<Msg, Long> timestampTableColumn;

    @FXML
    public ComboBox<Session> sessionComboBox;

    @FXML
    public ComboBox<Filter> filterComboBox;

    @FXML
    public Button visPropertyPaneTglBtn;

    private final AtomicReference<Disposable> streamDisposable = new AtomicReference<>(null);

    private ObjectBinding<ObservableList<Msg>> itemsBinding;

    @FXML
    private void initialize() {
        initTableContent();
        initSettingsContent(); // must occur after initTableContent()
    }

    @Override
    public void dispose() {
        streamDisposable.updateAndGet(disposable -> {
            if (disposable != null && !disposable.isDisposed()) {
                log.info("disposing of consoleTab's stream subscription");
                disposable.dispose();
            }
            return null;
        });
    }

    private void initTableContent() {
        // factories for cell values (information within each cell)
        uuidTableColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().sequence()));
        timestampTableColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().timestamp()));
        srcPortColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().src()));
        srcBridgeColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().srcBridge()));
        srcDispatchProtocolColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().srcBridge().dispatchProtocol()));
        srcPortModeColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().src().mode()));
        dstPortColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().dst()));
        dstBridgeColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().dstBridge()));
        dstDispatchProtocolColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().dstBridge().dispatchProtocol()));
        dstPortModeColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().dst().mode()));
        dataContentTableColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().data()));

        // factories for cell content (how cells are presented in the table)
        timestampTableColumn.setCellFactory(col -> new TimestampTableCell());
        srcPortColumn.setCellFactory(col -> makeSrcBg(new PortTableCell(artUtils, bridgeColoring)));
        srcBridgeColumn.setCellFactory(col -> makeSrcBg(new BridgeTableCell(artUtils, bridgeColoring)));
        srcDispatchProtocolColumn.setCellFactory(col -> makeSrcBg(new DispatchPropertyProtocolTableCell()));
        srcPortModeColumn.setCellFactory(col -> makeSrcBg(new PortModeTableCell()));
        dstPortColumn.setCellFactory(col -> makeDstBg(new PortTableCell(artUtils, bridgeColoring)));
        dstBridgeColumn.setCellFactory(col -> makeDstBg(new BridgeTableCell(artUtils, bridgeColoring)));
        dstDispatchProtocolColumn.setCellFactory(col -> makeDstBg(new DispatchPropertyProtocolTableCell()));
        dstPortModeColumn.setCellFactory(col -> makeDstBg(new PortModeTableCell()));
        dataContentTableColumn.setCellFactory(col -> new DataContentTableCell());

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

    private void initSettingsContent() {
        final PopOver visPopOver = new PopOver(createPopUpView());
        visPopOver.setTitle("Table Properties");

        visPropertyPaneTglBtn.setOnAction(event -> {
            if (visPopOver.isShowing()) {
                visPopOver.hide();
            } else {
                visPopOver.show(visPropertyPaneTglBtn);
            }
        });
    }

    private Node createPopUpView() {
        final CheckBox groupSources = createGroupSourcePropertiesToggle();
        final CheckBox groupDsts = createGroupDestinationPropertiesToggle();
        final var visPropView = createColumnVisibilityCheckTree();

        final VBox vBox = new VBox(4.0, groupSources, groupDsts, visPropView);
        vBox.setPadding(new Insets(2.0));

        return vBox;
    }

    @NotNull
    private CheckBox createGroupSourcePropertiesToggle() {
        final CheckBox groupSources = new CheckBox("Group source properties");
        groupSources.setAllowIndeterminate(false);
        groupSources.setSelected(true);

        groupSources.selectedProperty().addListener((observable, oldValue, newValue) -> {
            final var tableCols = tableView.getColumns();
            final var groupCols = srcColumn.getColumns();

            if (!oldValue && newValue) {
                final int index = Stream.of(srcPortColumn, srcBridgeColumn, srcDispatchProtocolColumn, srcPortModeColumn)
                        .mapToInt(col -> tableView.getColumns().indexOf(col))
                        .min()
                        .orElse(0);

                tableCols.remove(srcPortColumn);
                tableCols.remove(srcBridgeColumn);
                tableCols.remove(srcDispatchProtocolColumn);
                tableCols.remove(srcPortModeColumn);

                srcPortColumn.setText("Port");
                srcBridgeColumn.setText("Bridge");
                srcDispatchProtocolColumn.setText("Dispatch Protocol");
                srcPortModeColumn.setText("Port Mode");

                groupCols.add(srcPortColumn);
                groupCols.add(srcBridgeColumn);
                groupCols.add(srcDispatchProtocolColumn);
                groupCols.add(srcPortModeColumn);

                tableCols.add(index, srcColumn);
            } else if (oldValue && !newValue) {
                int index = tableCols.indexOf(srcColumn);
                index = index == -1 ? 0 : index;
                tableCols.remove(srcColumn);

                groupCols.remove(srcPortColumn);
                groupCols.remove(srcBridgeColumn);
                groupCols.remove(srcDispatchProtocolColumn);
                groupCols.remove(srcPortModeColumn);

                srcPortColumn.setText("Src Port");
                srcBridgeColumn.setText("Src Bridge");
                srcDispatchProtocolColumn.setText("Src Dispatch Protocol");
                srcPortModeColumn.setText("Src Port Mode");

                tableCols.add(index, srcPortModeColumn);
                tableCols.add(index, srcDispatchProtocolColumn);
                tableCols.add(index, srcBridgeColumn);
                tableCols.add(index, srcPortColumn);
            }
        });
        return groupSources;
    }

    @NotNull
    private CheckBox createGroupDestinationPropertiesToggle() {
        final CheckBox groupSources = new CheckBox("Group dest properties");
        groupSources.setAllowIndeterminate(false);
        groupSources.setSelected(true);

        groupSources.selectedProperty().addListener((observable, oldValue, newValue) -> {
            final var tableCols = tableView.getColumns();
            final var groupCols = dstColumn.getColumns();

            if (!oldValue && newValue) {
                final int index = Stream.of(dstPortColumn, dstBridgeColumn, dstDispatchProtocolColumn, dstPortModeColumn)
                        .mapToInt(col -> tableView.getColumns().indexOf(col))
                        .min()
                        .orElse(0);

                tableCols.remove(dstPortColumn);
                tableCols.remove(dstBridgeColumn);
                tableCols.remove(dstDispatchProtocolColumn);
                tableCols.remove(dstPortModeColumn);

                dstPortColumn.setText("Port");
                dstBridgeColumn.setText("Bridge");
                dstDispatchProtocolColumn.setText("Dispatch Protocol");
                dstPortModeColumn.setText("Port Mode");

                groupCols.add(dstPortColumn);
                groupCols.add(dstBridgeColumn);
                groupCols.add(dstDispatchProtocolColumn);
                groupCols.add(dstPortModeColumn);

                tableCols.add(index, dstColumn);
            } else if (oldValue && !newValue) {
                int index = tableCols.indexOf(dstColumn);
                index = index == -1 ? 0 : index;
                tableCols.remove(dstColumn);

                groupCols.remove(dstPortColumn);
                groupCols.remove(dstBridgeColumn);
                groupCols.remove(dstDispatchProtocolColumn);
                groupCols.remove(dstPortModeColumn);

                dstPortColumn.setText("Dst Port");
                dstBridgeColumn.setText("Dst Bridge");
                dstDispatchProtocolColumn.setText("Dst Dispatch Protocol");
                dstPortModeColumn.setText("Dst Port Mode");

                tableCols.add(index, dstPortModeColumn);
                tableCols.add(index, dstDispatchProtocolColumn);
                tableCols.add(index, dstBridgeColumn);
                tableCols.add(index, dstPortColumn);
            }
        });
        return groupSources;
    }

    private CheckTreeView<String> createColumnVisibilityCheckTree() {
        final var root = new CheckBoxTreeItem<>("Table");
        root.setIndependent(false);
        root.setIndeterminate(false);
        root.setSelected(true);
        root.setExpanded(true);

        // use listener instead of binding because CheckTreeView class needs ability to change values manually
        root.selectedProperty().addListener((observable, oldValue, newValue) -> {
            tableView.visibleProperty().setValue(newValue);
        });

        for (TableColumn<Msg, ?> column : tableView.getColumns()) {
            final var child = recursivelyCreateCheckTreeNode(column);
            root.getChildren().add(child);
        }

        return new CheckTreeView<>(root);
    }

    private CheckBoxTreeItem<String> recursivelyCreateCheckTreeNode(TableColumn<Msg, ?> column) {
        final var root = new CheckBoxTreeItem<>(column.getText());
        root.setIndependent(false);
        root.setIndeterminate(false);
        root.setSelected(true);
        root.setExpanded(true);

        // use listener instead of binding because CheckTreeView class needs ability to change values manually
        root.selectedProperty().addListener((observable, oldValue, newValue) -> {
            column.visibleProperty().setValue(newValue);
        });

        for (TableColumn<Msg, ?> child : column.getColumns()) {
            final var treeItem = recursivelyCreateCheckTreeNode(child);
            root.getChildren().add(treeItem);
        }

        return root;
    }

    private static <T extends Region> T makeSrcBg(T region) {
//        region.setBackground(new Background(new BackgroundFill(SRC_COLOR, null, null)));
//        region.setStyle(SRC_STYLE_PROPERTY);
        return region;
    }

    private static <T extends Region> T makeDstBg(T region) {
//        region.setBackground(new Background(new BackgroundFill(DST_COLOR, null, null)));
//        region.setStyle(DST_STYLE_PROPERTY);
        return region;
    }

}
