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
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class NodesByLabelStepTest {

    private JenkinsRule j;
    private WorkflowJob job;
    private WorkflowRun run;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        j = rule;
        job = j.jenkins.createProject(WorkflowJob.class, "workflow");

        DumbSlave[] dummies = {
            j.createSlave("dummy1", "a", new EnvVars()),
            j.createSlave("dummy2", "a b", new EnvVars()),
            j.createSlave("dummy3", "a b c", new EnvVars()),
            j.createSlave("dummy4", "a b c d", new EnvVars()),
        };
        for (DumbSlave dummy : dummies) {
            dummy.toComputer().waitUntilOnline();
        }

        CLICommandInvoker command = new CLICommandInvoker(j, "disconnect-node");
        CLICommandInvoker.Result result =
                command.authorizedTo(Computer.DISCONNECT, Jenkins.READ).invokeWithArgs("dummy4");
        assertThat(result, succeededSilently());
        assertThat(dummies[3].toComputer().isOffline(), equalTo(true));
    }

    @Test
    void test_nodes_by_label_count_a() throws Exception {
        // leave out the subject
        job.setDefinition(new CpsFlowDefinition("nodesByLabel('a')", true));

        run = j.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0).get());
        j.assertLogContains("Found a total of 3 nodes with the 'a' label", run);
    }

    @Test
    void test_nodes_by_label_count_b() throws Exception {
        job.setDefinition(new CpsFlowDefinition("nodesByLabel('b')", true));

        run = j.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0).get());
        j.assertLogContains("Found a total of 2 nodes with the 'b' label", run);
    }

    @Test
    void test_nodes_by_label_count_c() throws Exception {
        job.setDefinition(new CpsFlowDefinition("nodesByLabel('c')", true));

        run = j.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0).get());
        j.assertLogContains("Found a total of 1 nodes with the 'c' label", run);
    }

    @Test
    void test_nodes_by_label_count_d_offline() throws Exception {
        job.setDefinition(new CpsFlowDefinition("nodesByLabel('d')", true));

        run = j.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0).get());
        j.assertLogContains("Could not find any nodes with 'd' label", run);
    }

    @Test
    void test_nodes_by_label_for_loop() throws Exception {
        job.setDefinition(new CpsFlowDefinition(
                """
                def nodes = nodesByLabel('a')
                  for (int i = 0; i < nodes.size(); i++) {
                    def n = nodes[i]
                    echo "Hello ${n}"
                  }""",
                true));
        assertBuildLoop(false);
    }

    @Test
    void test_nodes_by_label_each_loop() throws Exception {
        job.setDefinition(new CpsFlowDefinition(
                """
                def nodes = nodesByLabel 'a'
                  nodes.each { n ->
                    echo "Hello ${n}"
                  }""",
                true));
        assertBuildLoop(false);
    }

    @Test
    void test_nodes_by_label_include_offline() throws Exception {
        job.setDefinition(new CpsFlowDefinition(
                """
                def nodes = nodesByLabel label: 'a', offline: true
                  nodes.each { n ->
                    echo "Hello ${n}"
                  }""",
                true));
        assertBuildLoop(true);
    }

    private void assertBuildLoop(boolean offline) throws Exception {
        run = j.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0).get());
        j.assertLogContains("Hello dummy1", run);
        j.assertLogContains("Hello dummy2", run);
        j.assertLogContains("Hello dummy3", run);
        if (offline) {
            j.assertLogContains("Hello dummy4", run);
        } else {
            j.assertLogNotContains("Hello dummy4", run);
        }
    }

    @Test
    void test_nodes_by_label_non_existing_label() throws Exception {
        job.setDefinition(new CpsFlowDefinition("def nodes = nodesByLabel('F')\n", true));
        run = j.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0).get());
        j.assertLogContains("Could not find any nodes with 'F' label", run);
    }

    @Test
    void test_nodes_by_label_get_label() throws Exception {
        NodesByLabelStep step1 = new NodesByLabelStep("a");
        step1.setOffline(true);
        assertEquals("a", step1.getLabel());
        NodesByLabelStep step2 = new StepConfigTester(j).configRoundTrip(step1);
        j.assertEqualDataBoundBeans(step1, step2);
        step1.setOffline(false);
        step2 = new StepConfigTester(j).configRoundTrip(step1);
        j.assertEqualDataBoundBeans(step1, step2);
    }
}
