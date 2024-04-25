package org.jenkinsci.plugins.pipeline.utility.steps.jenkins;

import hudson.EnvVars;
import hudson.cli.CLICommandInvoker;
import hudson.model.Computer;
import hudson.model.Result;
import hudson.slaves.DumbSlave;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static hudson.cli.CLICommandInvoker.Matcher.succeededSilently;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class CurrentAgentStepTest {
    @Rule
    public JenkinsRule r = new JenkinsRule();
    private WorkflowJob job;
    private WorkflowRun run;

    @Before
    public void setup() throws Exception {
        job = r.jenkins.createProject(WorkflowJob.class, "workflow");

        DumbSlave agent = r.createSlave("agent", "a", new EnvVars());
        agent.toComputer().waitUntilOnline();
    }

    @Test
    public void outside_of_node() throws Exception {
        job.setDefinition(new CpsFlowDefinition("echo 'Outside:' + currentAgent() + ':'", true));
        run = r.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0).get());
        r.assertLogContains("Outside::", run);
    }

    @Test
    public void on_builtin_node() throws Exception {
        job.setDefinition(new CpsFlowDefinition("node('built-in') {echo 'built-in:' + currentAgent() + ':'}", true));
        run = r.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0).get());
        r.assertLogContains("built-in:built-in:", run);
    }

    @Test
    public void on_explicit_node() throws Exception {
        job.setDefinition(new CpsFlowDefinition("node('agent') {echo 'agent:' + currentAgent() + ':'}", true));
        run = r.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0).get());
        r.assertLogContains("agent:agent:", run);
    }

    @Test
    public void on_label() throws Exception {
        job.setDefinition(new CpsFlowDefinition("node('a') {echo 'label:' + currentAgent() + ':'}", true));
        run = r.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0).get());
        r.assertLogContains("label:agent:", run);
    }

    @Test
    public void on_nested() throws Exception {
        job.setDefinition(new CpsFlowDefinition("node('built-in') { node('a') {echo 'nested:' + currentAgent() + ':'}}", true));
        run = r.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0).get());
        r.assertLogContains("nested:agent:", run);
    }
}
