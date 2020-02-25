package org.sireum.hamr.inspector.gui.components.msc;

import art.Bridge;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sireum.hamr.inspector.common.Msg;
import org.sireum.hamr.inspector.gui.App;
import org.sireum.hamr.inspector.gui.ViewController;

@Slf4j
@ViewController
@NoArgsConstructor
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection") // <-- protection for generated classes
public final class Msc {

    static final double ROW_HEIGHT = 80.0;
    static final double COLUMN_WIDTH = 200.0;

    @Getter
    private final double columnWidth = COLUMN_WIDTH;

    @Getter
    private final double rowHeight = ROW_HEIGHT; // accessed by msc.fxml

//    @Getter
//    @Autowired
//    @Qualifier("filters")
//    private ObservableList<Filter> filters;

//    @Autowired
//    @Qualifier("msgService")
//    private MsgService msgService;

    @FXML
    private TableView<Msg> tableView;

//    @FXML
//    public ComboBox<Filter> filterComboBox;

    public ObservableList<Msg> getItems() {
        return tableView.getItems();
    }

    public ObjectProperty<ObservableList<Msg>> itemsProperty() {
        return tableView.itemsProperty();
    }

    public void setItems(ObservableList<Msg> items) {
        this.tableView.setItems(items);
    }

    @FXML
    protected void initialize() {
        initTableStructure();
    }

    // todo needed? bad for service?
//    @Override
//    public void dispose() {
//        streamDisposable.updateAndGet(disposable -> {
//            if (disposable != null && !disposable.isDisposed()) {
//                log.info("disposing of mscTab's stream subscription");
//                disposable.dispose();
//            }
//            return null;
//        });
//    }

    private void initTableStructure() {
        for (Bridge bridge : App.getArtUtils().getBridges()) {
            final var column = new TableColumn<Msg, Msg>(App.getArtUtils().prettyPrint(bridge));

            column.setUserData(bridge); // user data MUST be set to bridge
            column.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
            column.setCellFactory(col -> new MscTableCell());
            column.setPrefWidth(columnWidth);

            column.setResizable(false);
            column.setReorderable(true);
            column.setEditable(false);

            column.setStyle("-bridge-color: #" + App.getBridgeColoring().getRgbStringOf(bridge) + ";");

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

}
