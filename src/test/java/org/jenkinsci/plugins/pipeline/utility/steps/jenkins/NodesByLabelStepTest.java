/*
 * The MIT License
 *
 * Copyright (c) 2018, Joseph Petersen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.pipeline.utility.steps.jenkins;

import static hudson.cli.CLICommandInvoker.Matcher.succeededSilently;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import hudson.EnvVars;
import hudson.cli.CLICommandInvoker;
import hudson.model.Computer;
import hudson.model.Result;
import hudson.slaves.DumbSlave;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class NodesByLabelStepTest {
    @Rule
    public JenkinsRule r = new JenkinsRule();
    private WorkflowJob job;
    private WorkflowRun run;

    @Before
    public void setup() throws Exception {
        job = r.jenkins.createProject(WorkflowJob.class, "workflow");
        r.createSlave("dummy1", "a", new EnvVars());
        r.createSlave("dummy2", "a b", new EnvVars());
        r.createSlave("dummy3", "a b c", new EnvVars());
        DumbSlave slave = r.createSlave("dummy4", "a b c d", new EnvVars());
        slave.toComputer().waitUntilOnline();
        CLICommandInvoker command = new CLICommandInvoker(r, "disconnect-node");
        CLICommandInvoker.Result result = command
                .authorizedTo(Computer.DISCONNECT, Jenkins.READ)
                .invokeWithArgs("dummy4");
        assertThat(result, succeededSilently());
        assertThat(slave.toComputer().isOffline(), equalTo(true));
    }

    @Test
    public void test_nodes_by_label_count_a() throws Exception {
        // leave out the subject
        job.setDefinition(new CpsFlowDefinition("nodesByLabel('a')", true));

        run = r.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0).get());
        r.assertLogContains("Found a total of 3 nodes with the 'a' label", run);
    }

    @Test
    public void test_nodes_by_label_count_b() throws Exception {
        job.setDefinition(new CpsFlowDefinition("nodesByLabel('b')", true));

        run = r.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0).get());
        r.assertLogContains("Found a total of 2 nodes with the 'b' label", run);
    }

    @Test
    public void test_nodes_by_label_count_c() throws Exception {
        job.setDefinition(new CpsFlowDefinition("nodesByLabel('c')", true));

        run = r.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0).get());
        r.assertLogContains("Found a total of 1 nodes with the 'c' label", run);
    }

    @Test
    public void test_nodes_by_label_count_d_offline() throws Exception {
        job.setDefinition(new CpsFlowDefinition("nodesByLabel('d')", true));

        run = r.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0).get());
        r.assertLogContains("Could not find any nodes with 'd' label", run);
    }

    @Test
    public void test_nodes_by_label_for_loop() throws Exception {
        job.setDefinition(new CpsFlowDefinition("def nodes = nodesByLabel('a')\n" +
                "  for (int i = 0; i < nodes.size(); i++) {\n" +
                "    def n = nodes[i]\n" +
                "    echo \"Hello ${n}\"\n" +
                "  }", true));
        assertBuildLoop(false);
    }
    @Test
    public void test_nodes_by_label_each_loop() throws Exception {
        job.setDefinition(new CpsFlowDefinition("def nodes = nodesByLabel 'a'\n" +
                "  nodes.each { n ->\n" +
                "    echo \"Hello ${n}\"\n" +
                "  }", true));
        assertBuildLoop(false);
    }

    @Test
    public void test_nodes_by_label_include_offline() throws Exception {
        job.setDefinition(new CpsFlowDefinition("def nodes = nodesByLabel label: 'a', offline: true\n" +
                "  nodes.each { n ->\n" +
                "    echo \"Hello ${n}\"\n" +
                "  }", true));
        assertBuildLoop(true);
    }

    private void assertBuildLoop(boolean offline) throws Exception {
        run = r.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0).get());
        r.assertLogContains("Hello dummy1", run);
        r.assertLogContains("Hello dummy2", run);
        r.assertLogContains("Hello dummy3", run);
        if (offline) {
            r.assertLogContains("Hello dummy4", run);
        } else {
            r.assertLogNotContains("Hello dummy4", run);
        }
    }

    @Test
    public void test_nodes_by_label_non_existing_label() throws Exception {
        job.setDefinition(new CpsFlowDefinition("def nodes = nodesByLabel('F')\n", true));
        run = r.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0).get());
        r.assertLogContains("Could not find any nodes with 'F' label", run);
    }

    @Test
    public void test_nodes_by_label_get_label() throws Exception {
        NodesByLabelStep step1 = new NodesByLabelStep("a");
        step1.setOffline(true);
        Assert.assertEquals(step1.getLabel(), "a");
        NodesByLabelStep step2 = new StepConfigTester(r).configRoundTrip(step1);
        r.assertEqualDataBoundBeans(step1, step2);
        step1.setOffline(false);
        step2 = new StepConfigTester(r).configRoundTrip(step1);
        r.assertEqualDataBoundBeans(step1, step2);
    }
}
