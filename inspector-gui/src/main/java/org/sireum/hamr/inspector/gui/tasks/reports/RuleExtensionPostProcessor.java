package org.sireum.hamr.inspector.gui.tasks.reports;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import java.util.Optional;

// generated by junit5, do not throw exceptions as junit will swallow some (see ThrowableCollector, BlacklistedExceptions)
@Slf4j
@NoArgsConstructor // <-- important for generated junit5 class
public class RuleExtensionPostProcessor implements TestInstancePostProcessor {

    /**
     * Will be called once per TestFactory, do NOT call task.getNextJob() here or it will offset for the tests.
     * @param testInstance
     * @param context
     * @throws Exception
     */
    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        final RuleTest ruleTest = (RuleTest) testInstance;

        warnIfNotNull(ruleTest.getMsgService(), "ruleTest should have no msgService because it is the job of this post-processing class to assign the msgService");
        warnIfNotNull(ruleTest.getJobs(), "ruleTest should have no ruleTestJobs because it is the job of this post-processing class to assign the ruleTestJobs");

        final Optional<String> configurationParameter =
                context.getConfigurationParameter(GenerateTestReportTask.CONFIGURATION_PARAMETER_KEY);

        if (configurationParameter.isEmpty()) {
            log.error("test case should have configuration parameter pointing to test set, but it was not found");
        }

        final String id = configurationParameter.get();
        final GenerateTestReportTask task = GenerateTestReportTask.getInstanceById(id);

        // populate RuleTest's fields
        ruleTest.setMsgService(task.msgService);
        ruleTest.setJobs(task.getJobs());
    }

    private static void warnIfNotNull(Object object, String msg) {
        if (object != null) {
            log.warn(msg);
        }
    }

}