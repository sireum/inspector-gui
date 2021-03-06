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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.ConfigurationBuilder;
import io.qameta.allure.Extension;
import io.qameta.allure.ReportGenerator;
import io.qameta.allure.allure2.Allure2Plugin;
import io.qameta.allure.category.CategoriesPlugin;
import io.qameta.allure.category.CategoriesTrendPlugin;
import io.qameta.allure.context.FreemarkerContext;
import io.qameta.allure.context.JacksonContext;
import io.qameta.allure.context.MarkdownContext;
import io.qameta.allure.context.RandomUidContext;
import io.qameta.allure.core.*;
import io.qameta.allure.duration.DurationPlugin;
import io.qameta.allure.duration.DurationTrendPlugin;
import io.qameta.allure.executor.ExecutorPlugin;
import io.qameta.allure.history.HistoryPlugin;
import io.qameta.allure.history.HistoryTrendPlugin;
import io.qameta.allure.idea.IdeaLinksPlugin;
import io.qameta.allure.junitplatform.AllureJunitPlatform;
import io.qameta.allure.junitxml.JunitXmlPlugin;
import io.qameta.allure.launch.LaunchPlugin;
import io.qameta.allure.retry.RetryPlugin;
import io.qameta.allure.retry.RetryTrendPlugin;
import io.qameta.allure.severity.SeverityPlugin;
import io.qameta.allure.status.StatusChartPlugin;
import io.qameta.allure.suites.SuitesPlugin;
import io.qameta.allure.summary.SummaryPlugin;
import io.qameta.allure.tags.TagsPlugin;
import io.qameta.allure.timeline.TimelinePlugin;
import javafx.concurrent.Task;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.sireum.hamr.inspector.common.ArtUtils;
import org.sireum.hamr.inspector.services.MsgService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

// https://github.com/allure-framework/allure-docs/blob/master/docs/plugins.adoc
@Slf4j
public class GenerateTestReportTask extends Task<Path> {

    /**
     * Effectively a concurrent weak-key weak-value hash map. This way generateReportTasks can add themselves to the
     * cache to be seen by its spawned tests, and will self-delete once garbage collected.
     */
    private static final Cache<String, GenerateTestReportTask> activeGenerateReportCache = Caffeine.newBuilder().weakKeys().weakValues().build();
    private static final AtomicInteger nextGenerateTestReportTaskCounter = new AtomicInteger();

    @Getter
    private final List<RuleTestJob> jobs;

//    @Getter
//    private final CountDownLatch latch;

    // for each test to make a reference from their dynamicTest name back to their RuleTestJob
    @Getter
    private final AtomicInteger nextJobCounter = new AtomicInteger();

    static final String CONFIGURATION_PARAMETER_KEY = "org.sireum.hamr.inspector.gui.tasks.reports.spawningClassUID";
    private final String configurationParameterValue = Integer.toString(nextGenerateTestReportTaskCounter.getAndIncrement());

    final MsgService msgService;

    final ArtUtils artUtils;

    public GenerateTestReportTask(Collection<RuleTestJob> tests, MsgService msgService, ArtUtils artUtils) {
        this.msgService = msgService;
        this.jobs = Collections.unmodifiableList(List.copyOf(tests));
        this.artUtils = artUtils;
//        this.latch = new CountDownLatch(jobs.size());
        checkRuleNameUniqueness();
    }

    private void checkRuleNameUniqueness() {
        final int len = jobs.size();
        for (int i = 0; i < len - 1; i++) {
            for (int j = i + 1; j < len; j++) {
                final boolean areNamesEqual = jobs.get(i).rule.name().equals(jobs.get(j).rule.name());
                final boolean areSessionsEqual = jobs.get(i).session.equals(jobs.get(j).session);
                if (areNamesEqual && areSessionsEqual) {
                    log.warn("duplicate rules found with both non-unique name: {} AND non-unique session {}",
                            jobs.get(i).rule.name(), jobs.get(i).session);

                    break; // stop checking after 1 collision is preferable because otherwise a group of n>2 collisions
                    // would create (n-1)! different warnings in the log output
                }
            }
        }
    }

    private LauncherDiscoveryRequest createLauncherDiscoveryRequest() {
        // moved to outside method:
        return LauncherDiscoveryRequestBuilder.request()
                .selectors(selectMethod(RuleTest.class, "dynamicTestFactory"))
                .configurationParameter(CONFIGURATION_PARAMETER_KEY, configurationParameterValue) // make sure to register its key for child tests to find
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
                .configurationParameter("junit.jupiter.execution.parallel.config.strategy", "dynamic")
                .build();
    }

    static GenerateTestReportTask getInstanceById(String uniquePropertyString) {
        return activeGenerateReportCache.getIfPresent(uniquePropertyString);
    }

    /**
     * See https://docs.oracle.com/javase/8/javafx/api/javafx/concurrent/Task.html?
     * Remember to wrap any blocking calls in try catch for InterruptedException
     * @return
     * @throws Exception
     */
    @Override
    protected Path call() throws Exception {
        try {
            final Path path = handleCall();
            return path;
        } catch (Exception e) {
            log.error("An exception occurred during task call()", e);
            e.printStackTrace();
            throw e;
        }
    }

    private Path handleCall() throws Exception {
        update1();
        log.info("starting with jobs {}", Arrays.toString(jobs.toArray()));

        // create files
        final var reportOutputPath = createOutputPath("allure-generated");
        final var testEngineOutputPath = createOutputPath("allure-results");

        if (checkCancelledStage()) return null;
        update2();

        // setup test plan and discover tests
        activeGenerateReportCache.put(configurationParameterValue, this); // add this instance to the cache
        final var writer = new CustomResultsWriter(testEngineOutputPath, jobs);
        final AllureLifecycle lifecycle = new AllureLifecycle(writer);
        final var platform = new AllureJunitPlatform(lifecycle);

        final LauncherDiscoveryRequest launcherDiscoveryRequest = createLauncherDiscoveryRequest();

        final JupiterTestEngine engine = new JupiterTestEngine();

        final var launcherConfig = LauncherConfig.builder()
                .addTestExecutionListeners(platform)
                .addTestEngines(engine)
                .enableTestEngineAutoRegistration(false) // was true
                .enableTestExecutionListenerAutoRegistration(false) // avoid because serviceProvider impl has wrong output path
                .build();

        if (checkCancelledStage()) return null;
        update3(testEngineOutputPath);

        final var launcher = LauncherFactory.create(launcherConfig);

        // blocks until complete
        launcher.execute(launcherDiscoveryRequest);
//
//        while (!latch.await(500, TimeUnit.MILLISECONDS)) {
//
//            // every 500 ms check to see if user canceled.
//            // it's OK to block because thread is scheduled on Schedulers.elastic()
//
//            if (checkCancelledStage()) return null; // todo cancel scheduled tasks
//        }

        if (checkCancelledStage()) return null;
        update4(reportOutputPath);

        // generate report
        final Configuration config = new ConfigurationBuilder().fromExtensions(getExtensions()).build();
        final var reportGenerator = new ReportGenerator(config);

        try {
            final Path parent = testEngineOutputPath.getParent();
            final List<Path> dirs = Files
                    .find(parent, Integer.MAX_VALUE, (path, info) -> info.isDirectory())
                    .collect(Collectors.toUnmodifiableList());
            reportGenerator.generate(reportOutputPath, dirs);
        } catch (IOException e) {
            setException(e);
            updateMessage("Error Generating Report");
            log.error("an error occurred when generating report {} from input data {}\n{}", reportOutputPath, testEngineOutputPath, e);
            e.printStackTrace();
            return null;
        }

        update5();
        return reportOutputPath;
    }

    private void update5() {
        updateMessage("Complete");
        updateProgress(100, 100);
    }

    private void update4(Path reportOutputPath) {
        log.debug("generating test report and writing output to {}", reportOutputPath);
        updateMessage("Generating test report");
        updateProgress(75, 100);
    }

    private void update3(Path testEngineOutputPath) {
        log.debug("executing test plan and writing output to {}", testEngineOutputPath);
        updateMessage("Executing test plan");
        updateProgress(15, 100);
    }

    private void update2() {
        updateMessage("Creating test plan");
        updateProgress(5, 100);
    }

    private void update1() {
        updateTitle("Generate Report Task");
        updateMessage("Preparing files");
        updateProgress(0, 100);
    }

    private boolean checkCancelledStage() {
        final boolean cancelled = isCancelled();
        if (cancelled) {
            updateMessage("Cancelled");
        }
        return cancelled;
    }

    @NotNull
    private List<Extension> getExtensions() {
        return List.of(
                new JunitXmlPlugin(), // <-- added to read on junit
                new JacksonContext(),
                new MarkdownContext(),
                new FreemarkerContext(),
                new RandomUidContext(),
                new MarkdownDescriptionsPlugin(),
                new RetryPlugin(),
                new RetryTrendPlugin(),
                new TagsPlugin(),
                new SeverityPlugin(),
//                        new OwnerPlugin(),
                new IdeaLinksPlugin(),
                new HistoryPlugin(),
                new HistoryTrendPlugin(),
                new CategoriesPlugin(),
                new CategoriesTrendPlugin(),
                new DurationPlugin(),
                new DurationTrendPlugin(),
                new StatusChartPlugin(),
                new TimelinePlugin(),
                new SuitesPlugin(),
                new ReportWebPlugin(),
                new TestsResultsPlugin(),
                new AttachmentsPlugin(),
                new SummaryPlugin(),
                new ExecutorPlugin(),
                new LaunchPlugin(),
                new Allure2Plugin()
        );
    }

    final RuleTestJob getNextJob() {
        final int jobId = nextJobCounter.getAndIncrement();
        try {
            Assertions.assertTrue(jobId < jobs.size(), "more jobs were requested than could be supplied. Jobs and tests should be a 1 to 1 relationship.");
        } catch (AssertionError error) {
            log.error("getNextJob() assertion failed", error);
        }
        return jobs.get(jobId);
    }

    private static Path createOutputPath(String prefix) {
        File directory;

        // increment number after filename until an available filename is found
        int i = 1;
        do {
            final String suffix = Integer.toUnsignedString(i) + "/";
            directory = new File(prefix + "/" + prefix + "-" + suffix);
            i++;
        } while (directory.exists());

        mkdirs(directory.toPath());

        return directory.toPath();
    }

    private static void mkdirs(Path path) {
        if (path.toFile().mkdirs()) {
            log.debug("created empty directory {}", path);
        }
    }

}




//package org.sireum.hamr.inspector.gui.tasks.reports;
//
//import com.github.benmanes.caffeine.cache.Cache;
//import com.github.benmanes.caffeine.cache.Caffeine;
//import io.qameta.allure.AllureLifecycle;
//import io.qameta.allure.ConfigurationBuilder;
//import io.qameta.allure.Extension;
//import io.qameta.allure.ReportGenerator;
//import io.qameta.allure.allure2.Allure2Plugin;
//import io.qameta.allure.category.CategoriesPlugin;
//import io.qameta.allure.category.CategoriesTrendPlugin;
//import io.qameta.allure.context.FreemarkerContext;
//import io.qameta.allure.context.JacksonContext;
//import io.qameta.allure.context.MarkdownContext;
//import io.qameta.allure.context.RandomUidContext;
//import io.qameta.allure.core.*;
//import io.qameta.allure.duration.DurationPlugin;
//import io.qameta.allure.duration.DurationTrendPlugin;
//import io.qameta.allure.executor.ExecutorPlugin;
//import io.qameta.allure.history.HistoryPlugin;
//import io.qameta.allure.history.HistoryTrendPlugin;
//import io.qameta.allure.idea.IdeaLinksPlugin;
//import io.qameta.allure.junitplatform.AllureJunitPlatform;
//import io.qameta.allure.junitxml.JunitXmlPlugin;
//import io.qameta.allure.launch.LaunchPlugin;
//import io.qameta.allure.retry.RetryPlugin;
//import io.qameta.allure.retry.RetryTrendPlugin;
//import io.qameta.allure.severity.SeverityPlugin;
//import io.qameta.allure.status.StatusChartPlugin;
//import io.qameta.allure.suites.SuitesPlugin;
//import io.qameta.allure.summary.SummaryPlugin;
//import io.qameta.allure.tags.TagsPlugin;
//import io.qameta.allure.timeline.TimelinePlugin;
//import javafx.concurrent.Task;
//import lombok.Getter;
//import lombok.extern.slf4j.Slf4j;
//import org.jetbrains.annotations.NotNull;
//import org.junit.platform.launcher.LauncherDiscoveryRequest;
//import org.junit.platform.launcher.core.LauncherConfig;
//import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
//import org.junit.platform.launcher.core.LauncherFactory;
//import org.sireum.hamr.inspector.common.ArtUtils;
//import org.sireum.hamr.inspector.services.MsgService;
//import reactor.core.scheduler.Schedulers;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.List;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.stream.Collectors;
//
//import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
//
//// https://github.com/allure-framework/allure-docs/blob/master/docs/plugins.adoc
//@Slf4j
//public class GenerateTestReportTask extends Task<Path> {
//
//    /**
//     * Effectively a concurrent weak-key weak-value hash map. This way generateReportTasks can add themselves to the
//     * cache to be seen by its spawned tests, and will self-delete once garbage collected.
//     */
//    private static final Cache<String, GenerateTestReportTask> activeGenerateReportCache = Caffeine.newBuilder().weakKeys().weakValues().build();
//    private static final AtomicInteger nextGenerateTestReportTaskCounter = new AtomicInteger();
//
//    @Getter
//    private final List<RuleTestJob> jobs;
//
//    @Getter
//    private final CountDownLatch latch; // todo temp?
//
////    // for each test to make a reference from their dynamicTest name back to their RuleTestJob
////    @Getter
////    private final AtomicInteger nextJobCounter = new AtomicInteger();
//
//    static final String CONFIGURATION_PARAMETER_KEY = "org.sireum.hamr.inspector.gui.spawningClassUID";
//    private final String configurationParameterValue = Integer.toString(nextGenerateTestReportTaskCounter.getAndIncrement());
//
//    final MsgService msgService;
//    final ArtUtils artUtils;
//
//    public GenerateTestReportTask(Collection<RuleTestJob> tests, MsgService msgService, ArtUtils artUtils) {
//        this.msgService = msgService;
//        this.artUtils = artUtils;
//        jobs = Collections.unmodifiableList(List.copyOf(tests));
//        checkRuleNameUniqueness();
//        latch = new CountDownLatch(jobs.size());
//    }
//
//    private void checkRuleNameUniqueness() {
//        final int len = jobs.size();
//        for (int i = 0; i < len - 1; i++) {
//            for (int j = i + 1; j < len; j++) {
//                final boolean areNamesEqual = jobs.get(i).rule.name().equals(jobs.get(j).rule.name());
//                final boolean areSessionsEqual = jobs.get(i).session.equals(jobs.get(j).session);
//                if (areNamesEqual && areSessionsEqual) {
//                    log.warn("duplicate rules found with both non-unique name: {} AND non-unique session {}",
//                            jobs.get(i).rule.name(), jobs.get(i).session);
//
//                    break; // stop checking after 1 collision is preferable because otherwise a group of n>2 collisions
//                           // would create (n-1)! different warnings in the log output
//                }
//            }
//        }
//    }
//
//    private LauncherDiscoveryRequest createLauncherDiscoveryRequest() {
//        activeGenerateReportCache.put(configurationParameterValue, this); // add this instance to the cache
//        return LauncherDiscoveryRequestBuilder.request()
//                .selectors(selectMethod(RuleTest.class, "dynamicTestFactory"))
//                .configurationParameter(CONFIGURATION_PARAMETER_KEY, configurationParameterValue) // make sure to register its key for child tests to find
//                .configurationParameter("junit.jupiter.execution.parallel.enabled", "true")
//                .configurationParameter("junit.jupiter.execution.parallel.config.strategy", "dynamic")
//                .build();
//    }
//
//    static GenerateTestReportTask getInstanceById(String uniquePropertyString) {
//        return activeGenerateReportCache.getIfPresent(uniquePropertyString);
//    }
//
//    /**
//     * See https://docs.oracle.com/javase/8/javafx/api/javafx/concurrent/Task.html?
//     * Remember to wrap any blocking calls in try catch for InterruptedException
//     * @return
//     * @throws Exception
//     */
//    @Override
//    protected Path call() throws Exception {
//
//        try {
//            update1();
//            log.info("starting with jobs {}", Arrays.toString(jobs.toArray()));
//
//            // create files
//            final var reportOutputPath = createOutputPath("allure-generated");
//            final var testEngineOutputPath = createOutputPath("allure-results");
////        final var reportOutputPath = Path.of("/Users/matthewweis/Desktop/out/allure-generated");
////        final var testEngineOutputPath = Path.of("/Users/matthewweis/Desktop/out/allure-results");
//
//            if (checkCancelledStage()) return null;
//            update2();
//
//            // setup test plan and discover tests
//            final var request = createLauncherDiscoveryRequest();
//            final var writer = new CustomResultsWriter(testEngineOutputPath, jobs);
//            final var platform = new AllureJunitPlatform(new AllureLifecycle(writer));
//
//            // todo PROBLEM IS THAT ALLURE TEST HAS NO TEST UUIDS!!
//
//            final var launcher = LauncherFactory.create(LauncherConfig.builder()
//                    .addTestExecutionListeners(platform) // todo rm or confirm?
//                    .enableTestEngineAutoRegistration(true) // was true!
//                    .enableTestExecutionListenerAutoRegistration(false) // avoid because serviceProvider impl has wrong output path
//                    .build());
//
//            final var testPlan = launcher.discover(request);
//
//            if (checkCancelledStage()) return null;
//            update3(testEngineOutputPath);
//
//            // run tests
////        launcher.registerTestExecutionListeners(platform);
//
//            Schedulers.parallel().schedule(() -> launcher.execute(testPlan));
//
////            Thread t = new Thread(() -> launcher.execute(testPlan));
////
////            t.start();
////            launcher.execute(testPlan); // todo add custom test listener to increment progress when each test completes
//
//
//            // block until tests are done and written (OK because this whole class is run on own thread via elastic scheduler)
////            while (!latch.await(500, TimeUnit.MILLISECONDS)) { // false if timeout (so invert to keep looping)
////                if (checkCancelledStage()) return null; // periodically check for user cancel
////            }
//
//            if (checkCancelledStage()) return null;
//            update4(reportOutputPath);
//
//            // generate report
//            final Configuration config = new ConfigurationBuilder().fromExtensions(getExtensions()).build();
//            final var reportGenerator = new ReportGenerator(config);
//
//            try {
//                final Path parent = testEngineOutputPath.getParent();
//                final List<Path> dirs = Files
//                        .find(parent, Integer.MAX_VALUE, (path, info) -> info.isDirectory())
//                        .collect(Collectors.toUnmodifiableList());
//                reportGenerator.generate(reportOutputPath, dirs);
//            } catch (IOException e) {
//                setException(e);
//                updateMessage("Error Generating Report");
//                log.error("an error occurred when generating report {} from input data {}\n{}", reportOutputPath, testEngineOutputPath, e);
//                e.printStackTrace();
//                return null;
//            }
//
//            update5();
//            return reportOutputPath;
//        } catch (Exception e) {
//            e.printStackTrace();
//            log.error("Exception thrown during report generation!", e);
//        }
//
//        return null;
//    }
//
//    private void update5() {
//        updateMessage("Complete");
//        updateProgress(100, 100);
//    }
//
//    private void update4(Path reportOutputPath) {
//        log.info("generating test report and writing output to {}", reportOutputPath);
//        updateMessage("Generating test report");
//        updateProgress(75, 100);
//    }
//
//    private void update3(Path testEngineOutputPath) {
//        log.info("executing test plan and writing output to {}", testEngineOutputPath);
//        updateMessage("Executing test plan");
//        updateProgress(15, 100);
//    }
//
//    private void update2() {
//        updateMessage("Creating test plan");
//        updateProgress(5, 100);
//    }
//
//    private void update1() {
//        updateTitle("Generate Report Task");
//        updateMessage("Preparing files");
//        updateProgress(0, 100);
//    }
//
//    private boolean checkCancelledStage() {
//        final boolean cancelled = isCancelled();
//        if (cancelled) {
//            updateMessage("Cancelled");
//        }
//        return cancelled;
//    }
//
//    @NotNull
//    private List<Extension> getExtensions() {
//        return List.of(
//                new JacksonContext(),
//                new MarkdownContext(),
//                new FreemarkerContext(),
//                new RandomUidContext(),
//                new MarkdownDescriptionsPlugin(),
//                new RetryPlugin(),
//                new RetryTrendPlugin(),
//                new TagsPlugin(),
//                new SeverityPlugin(),
////                        new OwnerPlugin(),
//                new IdeaLinksPlugin(),
//                new HistoryPlugin(),
//                new HistoryTrendPlugin(),
//                new CategoriesPlugin(),
//                new CategoriesTrendPlugin(),
//                new DurationPlugin(),
//                new DurationTrendPlugin(),
//                new StatusChartPlugin(),
//                new TimelinePlugin(),
//                new SuitesPlugin(),
//                new ReportWebPlugin(),
//                new TestsResultsPlugin(),
//                new AttachmentsPlugin(),
//                new SummaryPlugin(),
//                new ExecutorPlugin(),
//                new LaunchPlugin(),
//                new Allure2Plugin(),
//                new JunitXmlPlugin() // <-- added to read un junit
//        );
//    }
//
////    final RuleTestJob getNextJob() {
////        final int jobId = nextJobCounter.getAndIncrement();
////        try {
////            Assertions.assertTrue(jobId < jobs.size(), "more jobs were requested than could be supplied. Jobs and tests should be a 1 to 1 relationship.");
////        } catch (AssertionError error) {
////            log.error("getNextJob() assertion failed", error);
////        }
////        return jobs.get(jobId);
////    }
//
//    private static Path createOutputPath(String prefix) {
//        File directory;
//
//        // increment number after filename until an available filename is found
//        int i = 1;
//        do {
//            final String suffix = Integer.toUnsignedString(i) + "/";
//            directory = new File(prefix + "/" + prefix + "-" + suffix);
//            i++;
//        } while (directory.exists());
//
//        mkdirs(directory.toPath());
//
//        return directory.toPath();
//    }
//
//    private static void mkdirs(Path path) {
//        if (path.toFile().mkdirs()) {
//            log.info("created empty directory {}", path);
//        }
//    }
//
//}