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

package org.jenkinsci.plugins.pipeline.utility.steps.conf.mf;

import static org.jenkinsci.plugins.pipeline.utility.steps.FilenameTestsUtils.separatorsToSystemEscaped;
import static org.jenkinsci.plugins.pipeline.utility.steps.Messages.AbstractFileOrTextStepDescriptorImpl_missingRequiredArgument;

import hudson.model.Label;
import hudson.model.Result;
import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Tests for {@link ReadManifestStep}.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
@WithJenkins
class ReadManifestStepTest {

    private JenkinsRule j;
    private WorkflowJob p;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        j = rule;
        j.createOnlineSlave(Label.get("slaves"));
        p = j.jenkins.createProject(WorkflowJob.class, "p");
    }

    @Test
    void configRoundTrip() throws Exception {
        ReadManifestStep step = new ReadManifestStep();
        step.setFile("target/my.jar");
        step.setText("tst");

        ReadManifestStep step2 = new StepConfigTester(j).configRoundTrip(step);
        j.assertEqualDataBoundBeans(step, step2);
    }

    @Test
    void testJarWithGradleManifest() throws Exception {
        URL resource = getClass().getResource("gradle-manifest.war");
        String remoting = new File(URLDecoder.decode(resource.getPath(), StandardCharsets.UTF_8))
                .getAbsolutePath()
                .replace('\\', '/');
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" + "  def man = readManifest file: '"
                        + separatorsToSystemEscaped(remoting) + "'\n" + "  assert man != null\n"
                        + "  assert man.main != null\n"
                        + "  echo man.main['Implementation-Version']\n"
                        + "  assert man.main['Implementation-Version'] == '1.0'\n"
                        + "}\n",
                true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        j.assertLogContains("Reading: META-INF/MANIFEST.MF", run);
    }

    @Test
    void testRemotingJar() throws Exception {
        String remotingVersion = hudson.remoting.Launcher.VERSION;
        String remoting = new File(j.getWebAppRoot(), "WEB-INF/lib/remoting-" + remotingVersion + ".jar")
                .getAbsolutePath()
                .replace('\\', '/');
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" + "  def man = readManifest file: '"
                        + separatorsToSystemEscaped(remoting) + "'\n" + "  assert man != null\n"
                        + "  assert man.main != null\n"
                        + "  echo man.main['Version']\n"
                        + "  assert man.main['Version'] == '"
                        + remotingVersion + "'\n" + "}\n",
                true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        j.assertLogContains("Reading: META-INF/MANIFEST.MF", run);
    }

    @Test
    void testText() throws Exception {
        String s = IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream("testmanifest.mf")), Charset.defaultCharset());
        p.setDefinition(new CpsFlowDefinition(
                "def man = readManifest text: '''" + s + "'''\n" + "assert man != null\n"
                        + "assert man.main != null\n"
                        + "echo man.main['Version']\n"
                        + "assert man.main['Version'] == '6.15.8'\n"
                        + "echo man.main['Application-Name']\n"
                        + "assert man.main['Application-Name'] == 'My App'\n"
                        + "assert man.entries['Section1']['Shame'] == 'On U'\n"
                        + "assert man.entries['Section2']['Shame'] == 'On Me'\n",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void testFile() throws Exception {
        URL resource = getClass().getResource("testmanifest.mf");
        File f = new File(resource.toURI());
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" + "  def man = readManifest file: '"
                        + f.getPath().replace('\\', '/') + "'\n" + "  assert man != null\n"
                        + "  assert man.main != null\n"
                        + "  echo man.main['Version']\n"
                        + "  assert man.main['Version'] == '6.15.8'\n"
                        + "  echo man.main['Application-Name']\n"
                        + "  assert man.main['Application-Name'] == 'My App'\n"
                        + "  assert man.entries['Section1']['Shame'] == 'On U'\n"
                        + "  assert man.entries['Section2']['Shame'] == 'On Me'\n"
                        + "}\n",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void testNothing() throws Exception {
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          def man = readManifest()
                        }
                        """,
                true));
        WorkflowRun run =
                j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains(AbstractFileOrTextStepDescriptorImpl_missingRequiredArgument("readManifest"), run);
    }

    @Test
    void testBoth() throws Exception {
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          def man = readManifest file: 'hello.mf', text: 'yolo'
                        }
                        """,
                true));
        WorkflowRun run =
                j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains("Need to specify either file or text to readManifest, can't do both", run);
    }
}
