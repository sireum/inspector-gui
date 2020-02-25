package org.sireum.hamr.inspector.gui.modules.msc;

import art.Bridge;
import freetimelabs.io.reactorfx.schedulers.FxSchedulers;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sireum.hamr.inspector.common.Filter;
import org.sireum.hamr.inspector.common.Msg;
import org.sireum.hamr.inspector.gui.App;
import org.sireum.hamr.inspector.gui.ViewController;
import org.sireum.hamr.inspector.gui.components.msc.MscTableCell;
import org.sireum.hamr.inspector.gui.modules.DisposableTabController;
import org.sireum.hamr.inspector.services.MsgService;
import org.sireum.hamr.inspector.services.Session;
import org.sireum.hamr.inspector.stream.Flux$;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
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
    @Qualifier("sessionNames")
    private ObservableList<String> sessionNames;

    @Autowired
    private MsgService dbService;

    @FXML
    private TableView<Msg> tableView;

    @FXML
    public ComboBox<Session> sessionComboBox;

    @FXML
    public ComboBox<Filter> filterComboBox;

    private final AtomicReference<Disposable> streamDisposable = new AtomicReference<>(null);

    private final ChangeListener<Disposable> subscriptionChangeListener = (observable, oldValue, newValue) -> {
        tableView.getItems().clear();
        if (oldValue != null && !oldValue.isDisposed()) {
            oldValue.dispose();
        }
        streamDisposable.getAndSet(newValue);
    };

    @SuppressWarnings("FieldCanBeLocal") // this property MUST be a field to avoid being GC'd as a weak reference
    private ObjectBinding<Disposable> currentSubscription = null;

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

    private void initTableContent() {
        currentSubscription = Bindings.createObjectBinding(() -> {
            final var session = sessionComboBox.getValue();
            final var filter = filterComboBox.getValue();

            if (session != null && filter != null) {
                return dbService.replayThenLive(session)
                        .publishOn(Schedulers.parallel())
                        .transformDeferred(flux -> filter.filter(Flux$.MODULE$.from(flux)))
                        .bufferTimeout(64, Duration.ofMillis(100))
                        .publishOn(FxSchedulers.fxThread())
                        .subscribe(msgs -> {
                            tableView.getItems().addAll(msgs);
                        });
            }

            return null;
        }, sessionComboBox.valueProperty(), filterComboBox.valueProperty());
        currentSubscription.addListener(subscriptionChangeListener);
    }

}
