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

import hudson.Functions;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.jenkinsci.plugins.workflow.test.steps.SemaphoreStep;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.Rule;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.RestartableJenkinsRule;

public class TeeStepTest {

    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule
    public RestartableJenkinsRule rr = new RestartableJenkinsRule();

    @Test
    public void smokes() throws Exception {
        rr.then(r -> {
                r.createSlave("remote", null, null);
                WorkflowJob p = r.createProject(WorkflowJob.class, "p");
                // Remote FS gets blown away during restart, alas; need JenkinsRule utility for stable agent workspace:
                p.addProperty(new ParametersDefinitionProperty(new StringParameterDefinition("WS", r.jenkins.getWorkspaceFor(p).getRemote())));
                p.setDefinition(new CpsFlowDefinition(
                        "node('remote') {\n" +
                        "  dir(params.WS) {\n" +
                        "    tee('x.log') {\n" +
                        "      echo 'first message'\n" +
                        "      semaphore 'wait'\n" +
                        "      echo 'second message'\n" +
                        "      if (isUnix()) {sh 'true'} else {bat 'rem'}\n" +
                        "    }\n" +
                        "    echo(/got: ${readFile('x.log').trim().replaceAll('\\\\s+', ' ').replace(pwd(), 'WS')}/)\n" +
                        "  }\n" +
                        "}", true));
                WorkflowRun b = p.scheduleBuild2(0).waitForStart();
                SemaphoreStep.waitForStart("wait/1", b);
        });
        rr.then(r -> {
                SemaphoreStep.success("wait/1", null);
                WorkflowRun b = r.jenkins.getItemByFullName("p", WorkflowJob.class).getBuildByNumber(1);
                r.waitForCompletion(b);
                r.assertLogContains("got: first message second message " + (Functions.isWindows() ? "WS>rem" : "+ true"), b);
        });
    }

    @Test
    public void create() throws Exception {
        rr.then(r -> {
            r.createSlave("remote", null, null);
            WorkflowJob p = r.createProject(WorkflowJob.class, "p");
            p.setDefinition(new CpsFlowDefinition(
                    "node('remote') {\n" +
                            "  tee('x.log') {\n" +
                            "    echo 'first message'\n" +
                            "  }\n" +
                            "  tee('x.log') {\n" +
                            "    echo 'second message'\n" +
                            "  }\n" +
                            "  echo(/got: ${readFile('x.log').trim().replaceAll('\\\\s+', ' ')}/)\n" +
                            "}", true));
            WorkflowRun b = p.scheduleBuild2(0).waitForStart();
            r.waitForCompletion(b);
            r.assertLogContains("got: second message", b);
        });
    }

    @Test
    public void closed() throws Exception {
        rr.then(r -> {
            r.createSlave("remote", null, null);
            WorkflowJob p = r.createProject(WorkflowJob.class, "p");
            p.setDefinition(new CpsFlowDefinition(
                    "node('remote') {\n" +
                            "  tee('x.log') {\n" +
                            "    echo 'first message'\n" +
                            "  }\n" +
                            "  if (isUnix()) {sh 'rm x.log'} else {bat 'del x.log'}\n" +
                            "  writeFile file: 'x.log', text: 'second message'\n" +
                            "  echo(/got: ${readFile('x.log').trim().replaceAll('\\\\s+', ' ')}/)\n" +
                            "}", true));
            WorkflowRun b = p.scheduleBuild2(0).waitForStart();
            r.waitForCompletion(b);
            r.assertLogContains("got: second message", b);
        });
    }

    @Test
    public void configRoundtrip() throws Exception {
        rr.then(r -> {
                TeeStep s = new TeeStep("x.log");
                StepConfigTester t = new StepConfigTester(rr.j);
                rr.j.assertEqualDataBoundBeans(s, t.configRoundTrip(s));
        });
    }

}
