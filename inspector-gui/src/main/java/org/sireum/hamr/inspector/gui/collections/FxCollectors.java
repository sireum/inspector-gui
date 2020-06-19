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

package org.sireum.hamr.inspector.gui.collections;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.experimental.UtilityClass;

import java.util.stream.Collector;

import static java.util.stream.Collector.Characteristics.*;

@UtilityClass
public final class FxCollectors {

    private static final Collector.Characteristics[] CH_CONCURRENT_ID = { CONCURRENT, UNORDERED, IDENTITY_FINISH };
    private static final Collector.Characteristics[] CH_CONCURRENT_NOID = { CONCURRENT, UNORDERED };
    private static final Collector.Characteristics[] CH_ID = { IDENTITY_FINISH };
    private static final Collector.Characteristics[] CH_UNORDERED_ID = { UNORDERED, IDENTITY_FINISH };
    private static final Collector.Characteristics[] CH_NOID = { };
    private static final Collector.Characteristics[] CH_UNORDERED_NOID = { UNORDERED };

    public static <T> Collector<T, ?, ObservableList<T>> toObservableList() {
        return Collector.of(FXCollections::observableArrayList, ObservableList::add,
                (left, right) -> { left.addAll(right); return left; },
                CH_ID);
    }

    @SuppressWarnings("unchecked")
    public static <T> Collector<T, ?, ObservableList<T>> toUnmodifiableObservableList() {
        return Collector.of(FXCollections::observableArrayList, ObservableList::add,
                (left, right) -> { left.addAll(right); return left; },
                list -> (ObservableList<T>) FXCollections.unmodifiableObservableList(list),
                CH_NOID);
    }

}
