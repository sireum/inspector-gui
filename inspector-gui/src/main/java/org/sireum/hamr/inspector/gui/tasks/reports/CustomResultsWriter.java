package org.sireum.hamr.inspector.gui.tasks.reports;

import io.qameta.allure.AllureResultsWriter;
import io.qameta.allure.FileSystemResultsWriter;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import lombok.extern.slf4j.Slf4j;

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

    public CustomResultsWriter(Path outputDirectory, List<RuleTestJob> jobs) {
        this.delegate = new FileSystemResultsWriter(outputDirectory);
        this.jobs = jobs;
    }

    @Override
    public void write(TestResult testResult) {
        final RuleTestJob job = match(testResult);
        if (job != null) {
            // todo dont forget to add regex matchers for categories, can attach info here needed for matchers
            // todo add msc chart as attachment?
            testResult.setName(job.getSession().toString()); // set to session since already categorized by name (suite)
//            testResult.setFullName(job.getRule().name());
//            testResult.setTestCaseId(job.getRule().name());
////            testResult.setUuid(job.getName());
//            testResult.setDescription(job.rule.description());
//            testResult.setHistoryId(job.getName()); // history is of this jobs over N Instant session
////            testResult.setRerunOf(job.getName()); // todo should only show from this session?
//            if (testResult.getStatus() == Status.BROKEN) { // todo look into
//                testResult.setStatus(Status.FAILED);
//            }

//            testResult.setLabels(
//                    List.of(
//                            new Label().setName("session").setValue(job.session.toString()),
//                            new Label().setName("rule").setValue(job.rule.displayName())
//                    ));

            testResult.setRerunOf(testResult.getHistoryId());

            // find label
            for (Label label : testResult.getLabels()) {
                if (label.getName().equals("package")) {
                    label.setValue("package!");
                } else if (label.getName().equals("testClass")) {
                    label.setValue("testClass!");
                } else if (label.getName().equals("suite")) { // suite is the top-level category in Suites
                    label.setValue(job.getRule().name()); // set this to name so we can have jobType -> Sessions
                }
            }

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
