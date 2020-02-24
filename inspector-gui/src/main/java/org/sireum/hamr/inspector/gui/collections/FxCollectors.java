package org.sireum.hamr.inspector.gui.collections;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.stream.Collector;

import static javafx.collections.FXCollections.observableList;
import static javafx.collections.FXCollections.unmodifiableObservableList;

@UtilityClass
public final class FxCollectors {

    public static <T> Collector<T, ?, ObservableList<T>> toObservableList() {
        return Collector.of(
                ArrayList::new,
                (BiConsumer<ArrayList<T>, T>) ArrayList::add,
                (left, right) -> { left.addAll(right); return left; },
                FXCollections::observableList);
    }

    public static <T> Collector<T, ?, ObservableList<T>> toUnmodifiableObservableList() {
        return Collector.of(
                ArrayList::new,
                (BiConsumer<ArrayList<T>, T>) ArrayList::add,
                (left, right) -> { left.addAll(right); return left; },
                list -> unmodifiableObservableList(observableList(list)));
    }

}
