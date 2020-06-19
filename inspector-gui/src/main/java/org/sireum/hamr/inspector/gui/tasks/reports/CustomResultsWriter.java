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

import io.qameta.allure.AllureResultsWriter;
import io.qameta.allure.FileSystemResultsWriter;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/*
 * Can keep everything the same and only override methods that actually write the results.
 *
 * Basic strategy, get result in, use it to look up info from rules list, change info as needed, then pass
 * on to delegate for writing.
 */
@Slf4j
public class CustomResultsWriter implements AllureResultsWriter {

    private final AllureResultsWriter delegate;
    private final List<RuleTestJob> jobs;

    // max size of a session name. Used for leftPadding names out.
    private final int maxSessionNameSize;

    public CustomResultsWriter(Path outputDirectory, List<RuleTestJob> jobs) {
        this.delegate = new FileSystemResultsWriter(outputDirectory);
        this.jobs = jobs;

        maxSessionNameSize = jobs.stream()
                .map(job -> job.session.getName().length())
                .max(Integer::compareTo)
                .orElse(0);
    }

    @Override
    public void write(TestResult testResult) {
        final RuleTestJob job = match(testResult);
        if (job != null) {
            // todo dont forget to add regex matchers for categories, can attach info here needed for matchers
            // todo add msc chart as attachment?

            testResult.setHistoryId(job.getRule().name()); // history is of this jobs over N Instant session
            testResult.setRerunOf(job.getName()); // todo should only show from this session?

            final String paddedName = StringUtils.leftPad(job.getSession().getName(), maxSessionNameSize, '0');
            testResult.setName(job.getRule().name() + " @ " + paddedName);

            // todo set stage
            // todo set flaky
            if (testResult.getStatus() == Status.BROKEN) { // todo look into
                testResult.setStatus(Status.FAILED);
            }

            if (testResult.getStatusDetails() != null) {
                testResult.getStatusDetails().setMessage(job.getRule().name());
            }

//            setLabel("package", "package!", testResult);
//            setLabel("testClass", "testClass!", testResult);
//            setLabel("suite", job.getRule().name(), testResult);

//            setLabel("epic", job.getRule().name() + "-e", testResult);
//            setLabel("feature", job.getRule().name() + "-f", testResult);
//            setLabel("story", job.getName() + "-s", testResult);

            // epic, feature, story

            // find label
//            for (Label label : testResult.getLabels()) {
//                if (label.getName().equals("package")) {
//                    label.setValue("package!");
//                } else if (label.getName().equals("testClass")) {
//                    label.setValue("testClass!");
//                } else if (label.getName().equals("suite")) { // suite is the top-level category in Suites
//                    label.setValue(job.getRule().name()); // set this to name so we can have jobType -> Sessions
//                } else if (label.getName().equals(""))
//            }

        }
//        if (job != null) {
//            // todo dont forget to add regex matchers for categories, can attach info here needed for matchers
//            // todo add msc chart as attachment?
//            testResult.setDescription(job.rule.description());
//            testResult.setHistoryId(job.getName()); // history is of this jobs over N Instant session
//            testResult.setRerunOf(job.getName()); // todo should only show from this session?
//            if (testResult.getStatus() == Status.BROKEN) { // todo look into
//                testResult.setStatus(Status.FAILED);
//            }
//
//            testResult.setLabels(
//                    List.of(new Label().setName("session").setValue(job.session.toString())
//            ));
//
//        }
        delegate.write(testResult);
    }

    private static void setLabel(String name, String value, TestResult testResult) {
        // replace existing key->value pair if one is found
        for (Label label : testResult.getLabels()) {
            if (label.getName().equals(name)) {
                label.setValue(value);
                return;
            }
        }
        // otherwise add a new key-value pair
        testResult.getLabels().add(new Label().setName(name).setValue(value));
    }

    @Override
    public void write(TestResultContainer testResultContainer) {
        delegate.write(testResultContainer);
    }

    @Override
    public void write(String source, InputStream attachment) {
        delegate.write(source, attachment);
    }

    private RuleTestJob match(TestResult testResult) {
        for (RuleTestJob job : jobs) {
            if (job.getName().equals(testResult.getName())) {
                return job;
            }
        }
        log.warn("unable to match testResult {} to a corresponding ruleTestJob", testResult);
        return null;
    }

}
