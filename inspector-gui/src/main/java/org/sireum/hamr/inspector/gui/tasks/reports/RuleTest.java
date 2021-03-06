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

package org.sireum.hamr.inspector.gui.tasks.reports;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.sireum.hamr.inspector.common.ArtUtils;
import org.sireum.hamr.inspector.common.Msg;
import org.sireum.hamr.inspector.services.MsgService;
import org.springframework.data.domain.Range;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

// generated by junit5 then post-processed by RuleExtensionPostProcessor
@Slf4j
@RunWith(JUnitPlatform.class)
@NoArgsConstructor @SuppressWarnings({"SpringJavaAutowiredFieldsWarningInspection", "RedundantSuppression"}) // <-- protection for generated classes
class RuleTest {

    // all four values are set by RuleExtensionPostProcessor

    @Getter @Setter
    private MsgService msgService = null;

    @Getter @Setter
    private ArtUtils artUtils = null;

    @Getter @Setter
    private List<RuleTestJob> jobs = null;

//    @Getter @Setter
//    private CountDownLatch latch = null;

    /**
     * CANNOT BE private OR static!
     * MUST BE NAMED "dynamicTestFactory" (this string is used above for reflection)
     * see: https://junit.org/junit5/docs/current/user-guide/#writing-tests-dynamic-tests
     *
     * @return
     */
    @TestFactory
    @ExtendWith(RuleExtensionPostProcessor.class)
    public Collection<DynamicTest> dynamicTestFactory() { // DO NOT RENAME (see doc above method)
        log.debug("Preparing dynamic rule tests for testing engine.");
        final List<DynamicTest> ruleTests = new ArrayList<>(jobs.size());
        for (RuleTestJob job : jobs) {
            ruleTests.add(DynamicTest.dynamicTest(job.getName(), () -> {
//                try {
                    log.debug("Running test {}", job.getName());
                    runTest(job, msgService, artUtils);
//                } finally {
                    log.debug("Finished test {}", job.getName());
//                    latch.countDown();
//                }
            }));
        }
        return Collections.unmodifiableList(ruleTests);
    }

    private static void runTest(RuleTestJob job, MsgService msgService, ArtUtils artUtils) throws Throwable {
        final var error = new AtomicBoolean();
        final var errorCause = new AtomicReference<Throwable>();
        final var success = new AtomicBoolean();

        final var key = job.getSession();
        final var rule = job.rule;

        // todo replace count and live-subscribe with replaySnapshop() method
        final Mono<Long> msgCount = msgService.count(key);
        final org.sireum.hamr.inspector.stream.Flux<Msg> input = org.sireum.hamr.inspector.stream.Flux.from(
//                msgCount.flatMapMany(count -> msgService.replay(key).take(count)));
                // todo why take count?
                msgCount.flatMapMany(count -> msgService.replay(key, Range.unbounded()).take(count)));

        // rule update 1
        final Flux<?> output = Flux.from(rule.rule(input));

        output
                .onErrorStop()
                .doOnError(throwable -> {
                    error.set(true);
                    errorCause.set(throwable);
                })
                .doOnComplete(() -> success.set(true))
                .then()
                .block();

        final boolean e = error.get();
        final boolean s = success.get();

        if (!e && s) {
            log.info("Test of Rule {} has completed successfully.", rule.name());
        } else if (e && !s) {
            final Throwable ec = errorCause.get();
            log.info("Test of Rule {} has ended in an error: {}", rule.name(), ec);
            throw ec;
        } else if (e && s) {
            final String cause = "a Rule should not be capable of both completing and erroring out";
            final Throwable ec = errorCause.get();
            log.error(cause, ec);
            throw new IllegalStateException(cause, ec);
        } else {
            final String cause = "a Rule should not end without indicating error or success";
            log.error(cause);
            throw new IllegalStateException(cause);
        }
    }
}
