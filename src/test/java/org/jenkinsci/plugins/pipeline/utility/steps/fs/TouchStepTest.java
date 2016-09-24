/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 CloudBees Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.jenkinsci.plugins.pipeline.utility.steps.fs;

import hudson.model.Label;
import org.jenkinsci.plugins.pipeline.utility.steps.zip.ZipStep;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

/**
 * Tests {@link TouchStep}.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class TouchStepTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setup() throws Exception {
        j.createOnlineSlave(Label.get("slaves"));
    }

    @Test
    public void configRoundTrip() throws Exception {
        TouchStep step = new TouchStep("target/my.tag");
        step.setTimestamp(10000l);

        TouchStep step2 = new StepConfigTester(j).configRoundTrip(step);
        j.assertEqualDataBoundBeans(step, step2);
    }

    @Test
    public void testNow() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  dir('inhere') {\n" +
                        "    def file = touch 'bobbywashere.tag'\n" +
                        "    long changed = file.lastModified\n" +
                        "    long now = System.currentTimeMillis()\n" +
                        "    echo \"Now: ${now} Changed: ${changed}\"\n" +
                        "    assert changed > now - 3000 && changed < now + 3000\n" + //Is a three seconds margin too tight or too loose?
                        "  }\n" +
                        "}", false)); //For some reason the Sandbox forbids invoking currentTimeMillis?
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void testThen() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  dir('inhere') {\n" +
                        "    def file = touch file: 'bobbywashere.tag', timestamp: 10000\n" +
                        "    long changed = file.lastModified\n" +
                        "    assert changed == 10000\n" +
                        "  }\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }
}