/*
 * The MIT License
 *
 * Copyright 2017 CloudBees, Inc.
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

package org.jenkinsci.plugins.pipeline.utility.steps.fs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.stringContainsInOrder;

import hudson.Functions;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.StringParameterDefinition;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.jenkinsci.plugins.workflow.test.steps.SemaphoreStep;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsSessionRule;

public class TeeStepTest {

    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule
    public JenkinsSessionRule sessions = new JenkinsSessionRule();

    @Test
    public void smokes() throws Throwable {
        sessions.then(r -> {
            r.createSlave("remote", null, null);
            WorkflowJob p = r.createProject(WorkflowJob.class, "p");
            // Remote FS gets blown away during restart, alas; need JenkinsRule utility for stable agent workspace:
            p.addProperty(new ParametersDefinitionProperty(new StringParameterDefinition(
                    "WS", r.jenkins.getWorkspaceFor(p).getRemote())));
            p.setDefinition(new CpsFlowDefinition(
                    """
                                node('remote') {
                                  dir(params.WS) {
                                    tee('x.log') {
                                      echo 'first message'
                                      semaphore 'wait'
                                      echo 'second message'
                                      if (isUnix()) {sh 'true'} else {bat 'rem'}
                                    }
                                    echo(/got: ${readFile('x.log').trim().replaceAll('\\\\s+', ' ').replace(pwd(), 'WS')}/)
                                  }
                                }""",
                    true));
            WorkflowRun b = p.scheduleBuild2(0).waitForStart();
            SemaphoreStep.waitForStart("wait/1", b);
        });
        sessions.then(r -> {
            SemaphoreStep.success("wait/1", null);
            WorkflowRun b = r.jenkins.getItemByFullName("p", WorkflowJob.class).getBuildByNumber(1);
            r.assertBuildStatus(Result.SUCCESS, r.waitForCompletion(b));
            assertThat(
                    JenkinsRule.getLog(b),
                    stringContainsInOrder(
                            "got: first message second message", Functions.isWindows() ? "WS>rem" : "+ true"));
        });
    }

    @Test
    @Issue({"JENKINS-54346", "JENKINS-55505"})
    public void closed() throws Throwable {
        sessions.then(r -> {
            r.createSlave("remote", null, null);
            WorkflowJob p = r.createProject(WorkflowJob.class, "p");
            p.setDefinition(new CpsFlowDefinition(
                    """
                            node('remote') {
                              tee('x.log') {
                                echo 'first message'
                              }
                              if (isUnix()) {sh 'rm x.log'} else {bat 'del x.log'}
                              writeFile file: 'x.log', text: 'second message'
                              echo(/got: ${readFile('x.log').trim().replaceAll('\\\\s+', ' ')}/)
                            }""",
                    true));
            WorkflowRun b = p.scheduleBuild2(0).waitForStart();
            r.assertBuildStatus(Result.SUCCESS, r.waitForCompletion(b));
            r.assertLogContains("got: second message", b);
        });
    }

    @Test
    @Issue({"JENKINS-55505"})
    public void closedMultiple() throws Throwable {
        sessions.then(r -> {
            r.createSlave("remote", null, null);
            WorkflowJob p = r.createProject(WorkflowJob.class, "p");
            p.setDefinition(new CpsFlowDefinition(
                    """
                            node('remote') {
                              tee('x.log') {
                                if (isUnix()) { sh 'echo first message' } else { bat 'echo first message' }
                                semaphore 'wait'
                                if (isUnix()) { sh 'echo second message' } else { bat 'echo second message' }
                              }
                              if (isUnix()) {sh 'rm x.log'} else {bat 'del x.log'}
                              writeFile file: 'x.log', text: 'third message'
                              echo(/got: ${readFile('x.log').trim().replaceAll('\\\\s+', ' ')}/)
                            }""",
                    true));
            WorkflowRun b = p.scheduleBuild2(0).waitForStart();
            SemaphoreStep.waitForStart("wait/1", b);
        });

        sessions.then(r -> {
            SemaphoreStep.success("wait/1", null);
            WorkflowRun b = r.jenkins.getItemByFullName("p", WorkflowJob.class).getBuildByNumber(1);
            r.assertBuildStatus(Result.SUCCESS, r.waitForCompletion(b));
            r.assertLogContains("got: third message", b);
        });
    }

    @Test
    public void configRoundtrip() throws Throwable {
        sessions.then(r -> {
            TeeStep s = new TeeStep("x.log");
            StepConfigTester t = new StepConfigTester(r);
            r.assertEqualDataBoundBeans(s, t.configRoundTrip(s));
        });
    }
}
