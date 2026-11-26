package org.jenkinsci.plugins.pipeline.utility.steps;

import hudson.model.Label;
import hudson.model.Slave;
import java.util.concurrent.CountDownLatch;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class NetworkBlipTest {

    private JenkinsRule j;

    private Slave agent;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        j = rule;
        agent = j.createOnlineSlave(Label.get("slaves"));
    }

    @AfterEach
    void tearDown() {
        AbstractFileOrTextStepExecution.afterWorkspaceResolved = workspace -> {};
    }

    @Test
    void networkBlipRightAfterWorkspaceResolutionDoesNotFailReadYaml() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                node('slaves') {
                  writeFile file: 'a.yaml', text: 'a: 1'
                  def yaml = readYaml file: 'a.yaml'
                  assert yaml.a == 1
                }""",
                true));
        var blipped = new CountDownLatch(1);
        AbstractFileOrTextStepExecution.afterWorkspaceResolved = workspace -> {
            if (blipped.getCount() > 0) {
                agent.toComputer().getChannel().close();
                blipped.countDown();
            }
        };
        var b = p.scheduleBuild2(0).waitForStart();
        blipped.await();
        agent.toComputer().waitUntilOffline();
        agent.toComputer().connect(false);
        j.assertBuildStatusSuccess(j.waitForCompletion(b));
    }
}
