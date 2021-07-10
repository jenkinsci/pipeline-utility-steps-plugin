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

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.Label;

/**
 * Tests {@link PrependToFileStep}.
 */
public class PrependToFileStepTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setup() throws Exception {
        this.j.createOnlineSlave(Label.get("slaves"));
    }

    @Test
    public void configRoundTrip() throws Exception {
        final PrependToFileStep step = new PrependToFileStep("target/my.tag","the prepended content");

        final PrependToFileStep step2 = new StepConfigTester(this.j).configRoundTrip(step);
        this.j.assertEqualDataBoundBeans(step, step2);
    }

    @Test
    public void testPrependToExistingFile() throws Exception {
        final WorkflowJob p = this.j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  dir('inhere') {\n" +
                		"    sh \"echo 'original content' > tomaswashere.tag\"\n"+
                        "    def file = prependToFile(file: 'tomaswashere.tag', content: 'prepended ')\n" +
                        "    def newContent = readFile(file: 'tomaswashere.tag')\n" +
                        "    echo \"newContent: ${newContent}\"\n" +
                        "    assert newContent.trim() == '''prepended original content'''\n" +
                        "  }\n" +
                        "}", true));
        this.j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void testPrependToNewFile() throws Exception {
        final WorkflowJob p = this.j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  dir('inhere') {\n" +
                        "    def file = prependToFile(file: 'tomaswashere.tag', content: 'prepended')\n" +
                        "    def newContent = readFile(file: 'tomaswashere.tag')\n" +
                        "    echo \"newContent: ${newContent}\"\n" +
                        "    assert newContent.trim() == '''prepended'''\n" +
                        "  }\n" +
                        "}", true));
        this.j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }
}
