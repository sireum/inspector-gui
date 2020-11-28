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

import impl.org.controlsfx.collections.ReadOnlyUnbackedObservableList;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sireum.hamr.inspector.common.ArtUtils;
import org.sireum.hamr.inspector.common.Filter;
import org.sireum.hamr.inspector.common.Msg;
import org.sireum.hamr.inspector.gui.ThreadedOn;
import org.sireum.hamr.inspector.services.MsgService;
import org.sireum.hamr.inspector.services.Session;
import org.sireum.hamr.inspector.stream.Flux;
import org.sireum.hooks.TimeBarriers;
import org.sireum.hooks.TimeUtils;
import org.springframework.data.domain.Range;
import reactor.core.Disposable;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A {@link javafx.collections.ObservableList} that reports new {@link Msg}s and optimized for linear access patterns.
 *
 * This list caches recently accessed {@link Msg}s and will prefetch more {@link Msg}s when {@link Msg}s are accessed
 * near the edge of what is cached. Currently, the get() method will block to fetch a {@link Msg} if the requested
 * {@link Msg} is not-cached (and surrounding {@link Msg}s will be cached as well). However this behavior may change
 * to instead return null or a dummmy-value immediately, and fake a list-update when the actual {@link Msg} arrives
 * asynchronously.
 *
 */
public class UnbackedLinearAccessObservableList extends ReadOnlyUnbackedObservableList<Msg> {

    @NotNull
    private final ArtUtils artUtils;

    @NotNull
    private final MsgService msgService;

    @NotNull
    private final Session session;

    @NotNull
    private final Filter filter;

    @ThreadedOn(threadName = "fx")
    final CircularNonSequentialGrowthBuffer<Msg> backingBuffer;

    @ThreadedOn(threadName = "fx")
    private int count = 0;

    private final reactor.core.publisher.Flux<Msg> filteredMsgs;

    final int MAX_UPDATE_CHUNK_SIZE = 256;
    final Duration MAX_UPDATE_REFRESH = Duration.ofMillis(100);

    final int backingArraySize = 2048;
    // tol must be less than backingArraySize/2, preferably at least backingArraySize/3 to avoid lots of prefetching
    final int boundaryPrefetchTolerance = 512;
    final int numBackbuffers = 3;

    @ThreadedOn(threadName = "fx")
    final Msg[][] buffers = new Msg[numBackbuffers][backingArraySize];

    @ThreadedOn(threadName = "fx")
    final int[] buffersCorrespondingGlobalIndex;
    {
        buffersCorrespondingGlobalIndex = new int[numBackbuffers];
        Arrays.fill(buffersCorrespondingGlobalIndex, -1);
    }

    @ThreadedOn(threadName = "fx")
    final Disposable[] buffersCorrespondingMaybeDisposable = new Disposable[numBackbuffers];

    @ThreadedOn(threadName = "fx")
    final CircularNonSequentialGrowthBuffer<Msg> liveBuffer = new CircularNonSequentialGrowthBuffer<>(new Msg[backingArraySize]);

    @ThreadedOn(threadName = "fx")
    private int lastIndex = -1; // initial -1 case is handled by bufferBoundaryCrossed

    private final Disposable counter;

    public UnbackedLinearAccessObservableList(@NotNull ArtUtils artUtils,
                                              @NotNull MsgService msgService,
                                              @NotNull Session session,
                                              @NotNull Filter filter) {
        this.artUtils = artUtils;
        this.msgService = msgService;
        this.session = session;
        this.filter = filter;

        backingBuffer = new CircularNonSequentialGrowthBuffer<>(new Msg[backingArraySize]);

        filteredMsgs = virtualFilterLimitRate(msgService.live(session, Range.unbounded()), backingArraySize, filter);

        counter = filteredMsgs
                .index()
                .bufferTimeout(MAX_UPDATE_CHUNK_SIZE, MAX_UPDATE_REFRESH)
                .subscribe(
                        indexedMsgs -> {
                            if (indexedMsgs.isEmpty()) {
                                return;
                            }

                            final long firstIndex = indexedMsgs.get(0).getT1();
                            final long lastIndex = indexedMsgs.get(indexedMsgs.size() - 1).getT1();
                            if (lastIndex >= Integer.MAX_VALUE) {
                                // count is 1 greater than (0-based) lastIndex, so cant be greater than OR equal
                                throw new IllegalStateException("Cannot count past int max value");
                            }

                            Platform.runLater(() -> {
                                count = 1 + (int) lastIndex;
                                for (Tuple2<Long, Msg> indexedMsg : indexedMsgs) {
                                    final Msg msg = indexedMsg.getT2();
                                    final int index = indexedMsg.getT1().intValue();
                                    liveBuffer.insertHead(msg, index);
                                }

                                beginChange();
                                nextAdd((int) firstIndex, (int) lastIndex);
                                endChange();
                            });
                        }
                );

    }

    private boolean bufferBoundaryCrossed(int newIndex, int tol) {
        if (lastIndex == -1) {
            return true;
        } else if (lastIndex == newIndex) {
            return false;
        }

        final int smaller = Math.min(lastIndex, newIndex);
        final int bigger = Math.max(lastIndex, newIndex);

        return (lastIndex / backingArraySize != newIndex / backingArraySize) ||
                ((smaller + tol) / backingArraySize != bigger / backingArraySize) ||
                (smaller / backingArraySize != (bigger - tol) / backingArraySize);
    }

    private int indexToGlobalBufferIndex(int index) {
        return (index / backingArraySize);
    }

    private int indexToLocalBufferIndex(int index) {
        return indexToGlobalBufferIndex(index) % numBackbuffers;
    }

    private int globalBufferToStartIndex(int globalBufferIndex) {
        return globalBufferIndex * backingArraySize;
    }

    @Nullable
    private Msg tryGet(int i) {
        return buffers[indexToLocalBufferIndex(i)][i % backingArraySize];
    }

    // copies list into array while preserving array length
    private static <T> void arrayCopy(List<T> src, T[] dst) {
        final int srcLen = src.size();
        for (int i=0; i < srcLen; i++) {
            dst[i] = src.get(i);
        }
        for (int i=srcLen; i < dst.length; i++) {
            dst[i] = null;
        }
    }

    @Override
    public Msg get(int i) {


        Msg maybeMsg = null;

        if (count - backingArraySize + 1 < i) {
            maybeMsg = liveBuffer.tryGet(i); // try to get from live circular buffer
        }

        if (maybeMsg == null) {
            maybeMsg = tryGet(i); // otherwise default to historic buffers (good chance its prefetched)
        }

        if (maybeMsg != null) {
            // if msg was cached, then possibly async prefetch next buffer before return if close enough to buffer edge
            if (!bufferBoundaryCrossed(i, boundaryPrefetchTolerance)) {
                final int nextBufferLocalIndex;
                final int nextBufferGlobalIndex;
                if (lastIndex < i) {
                    nextBufferLocalIndex = circularRem(indexToLocalBufferIndex(i) + 1, numBackbuffers);
                    nextBufferGlobalIndex = indexToGlobalBufferIndex(i) + 1;
                } else {
                    nextBufferLocalIndex = circularRem(indexToLocalBufferIndex(i) - 1, numBackbuffers);
                    nextBufferGlobalIndex = indexToGlobalBufferIndex(i) - 1;
                }

                if (nextBufferGlobalIndex >= 0 && buffersCorrespondingGlobalIndex[nextBufferLocalIndex] != nextBufferGlobalIndex) {
                    buffersCorrespondingGlobalIndex[nextBufferLocalIndex] = nextBufferGlobalIndex;
                    final Disposable d = msgService.replay(session, Range.unbounded())
                            // todo overkill for replay? (the rate limiting and backpressure buffering?)s
                            .transform(flux -> virtualFilterLimitRate(flux, backingArraySize, filter))
                            .skip(globalBufferToStartIndex(nextBufferGlobalIndex))
                            .take(backingArraySize)
                            .collectList()
                            .subscribe(
                                    list -> Platform.runLater(() -> {
                                        // buffersCorrespondingGlobalIndex[nextBufferLocalIndex] == nextBufferGlobalIndex
                                        // checks for race conditions from multiple async prefetches on same local index

                                        // buffersCorrespondingMaybeDisposable[nextBufferLocalIndex] != null
                                        // checks for race conditions with manual cancel that comes before blocking
                                        // (null always follows dispose() in this case and is on same thread)
                                        // note: isDisposed() doesn't necessarily have to be reported so this is better way
                                        if (buffersCorrespondingMaybeDisposable[nextBufferLocalIndex] != null &&
                                                buffersCorrespondingGlobalIndex[nextBufferLocalIndex] == nextBufferGlobalIndex) {
                                            arrayCopy(list, buffers[nextBufferLocalIndex]);
                                        }
                                    })
                            );

                    if (buffersCorrespondingMaybeDisposable[nextBufferLocalIndex] != null) {
                        buffersCorrespondingMaybeDisposable[nextBufferLocalIndex].dispose();
                    }
                    buffersCorrespondingMaybeDisposable[nextBufferLocalIndex] = d;
                }
            }

            lastIndex = i;
            return maybeMsg;
        }

        // otherwise, must manually update (blocking the thread to do so)

        lastIndex = i;
        final int bufferGlobalIndex = indexToGlobalBufferIndex(i);
        final int bufferLocalIndex = indexToLocalBufferIndex(i);
        buffersCorrespondingGlobalIndex[bufferLocalIndex] = bufferGlobalIndex;

        // cancel existing async request and block instead
        // (cannot rely on async because large jumps may not trigger one)
        // (cannot race with async unless atomic arrays are used OR async result made available via hot observable
        //  if present)
        if (buffersCorrespondingMaybeDisposable[bufferLocalIndex] != null) {
            buffersCorrespondingMaybeDisposable[bufferLocalIndex].dispose();
            buffersCorrespondingMaybeDisposable[bufferLocalIndex] = null;
        }

        final reactor.core.publisher.Mono<List<Msg>> serviceQuery = msgService.replay(session, Range.unbounded())
                .transform(flux -> virtualFilterLimitRate(flux, backingArraySize, filter))
                .skip(globalBufferToStartIndex(bufferGlobalIndex))
                .take(backingArraySize)
                .collectList();

        final List<Msg> list = Objects.requireNonNull(serviceQuery.block());
        arrayCopy(list, buffers[bufferLocalIndex]);
        return tryGet(i);
    }

    @Override
    @ThreadedOn(threadName = "fx")
    public int size() {
        return count;
    }

    private static reactor.core.publisher.Flux<Msg> virtualFilterLimitRate(reactor.core.publisher.Flux<Msg> flux, int backingArraySize, Filter filter) {
        return flux
                .limitRate(backingArraySize, backingArraySize) // overkill for replay?
                .onBackpressureBuffer(backingArraySize, BufferOverflowStrategy.ERROR) // overkill for replay?
                .map(msg -> TimeUtils.attachTimestamp(msg.timestamp(), msg))
                .publishOn(Schedulers.elastic())
                .transform(TimeBarriers::ENTER_VIRTUAL_TIME)
                .transformDeferred(it -> it.publish(lockStep -> filter.filter(Flux.from(lockStep))))
                .transform(TimeBarriers::EXIT_VIRTUAL_TIME);
    }

    // only allows for growth
    // non-sequential because one can "jump" ahead an index, at the cost of becoming the new head+tail`
    private static class CircularNonSequentialGrowthBuffer<T> {

        int next = 0;
        int tailCutoff = -1;

        private final T[] backingArray;

        private CircularNonSequentialGrowthBuffer(T[] backingArray) {
            this.backingArray = backingArray;
        }

        public void insertHead(@NotNull T msg, int index) {
            if (index != next) {
                tailCutoff = index;
            }

            final int actualHead = index % backingArray.length;
            backingArray[actualHead] = msg;
            next = index + 1;
        }

        // use i that corresponds to global index
        @Nullable
        public T tryGet(int i) {
            final int tail = Math.max(tailCutoff, next - backingArray.length);
            if (tail <= next && i < next) {
                final int actual = i % backingArray.length;
                return backingArray[actual];
            } else {
                return null;
            }
        }

    }

    private static int circularRem(int value, int modulus) {
        return ((value % modulus) + modulus) % modulus;
    }

}
