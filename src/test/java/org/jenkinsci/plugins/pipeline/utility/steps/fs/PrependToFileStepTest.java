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
import java.io.File;
import java.io.FileWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.pipeline.utility.steps.FilenameTestsUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Tests {@link PrependToFileStep}.
 */
@WithJenkins
class PrependToFileStepTest {

    private JenkinsRule j;

    @TempDir
    private File temp;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        j = rule;
        this.j.createOnlineSlave(Label.get("slaves"));
    }

    @Test
    void configRoundTrip() throws Exception {
        final PrependToFileStep step = new PrependToFileStep("target/my.tag", "the prepended content");

        final PrependToFileStep step2 = new StepConfigTester(this.j).configRoundTrip(step);
        this.j.assertEqualDataBoundBeans(step, step2);
    }

    @Test
    void testPrependToExistingFile() throws Exception {
        final WorkflowJob p = this.j.jenkins.createProject(WorkflowJob.class, "p");
        final File output = File.createTempFile("junit", null, this.temp);

        try (Writer f = new FileWriter(output);
                Reader r = new StringReader("original content")) {
            IOUtils.copy(r, f);
        }

        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" + "    def file = prependToFile(file: '"
                        + FilenameTestsUtils.toPath(output) + "', content: 'prepended ')\n"
                        + "    def newContent = readFile(file: '"
                        + FilenameTestsUtils.toPath(output) + "')\n" + "    echo \"newContent: ${newContent}\"\n"
                        + "    assert newContent.trim() == '''prepended original content'''\n"
                        + "}",
                true));
        this.j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void testPrependToNewFile() throws Exception {
        final WorkflowJob p = this.j.jenkins.createProject(WorkflowJob.class, "p");
        final File output = File.createTempFile("junit", null, this.temp);
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" + "    def file = prependToFile(file: '"
                        + FilenameTestsUtils.toPath(output) + "', content: 'prepended')\n"
                        + "    def newContent = readFile(file: '"
                        + FilenameTestsUtils.toPath(output) + "')\n" + "    echo \"newContent: ${newContent}\"\n"
                        + "    assert newContent.trim() == '''prepended'''\n"
                        + "}",
                true));
        this.j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }
}
