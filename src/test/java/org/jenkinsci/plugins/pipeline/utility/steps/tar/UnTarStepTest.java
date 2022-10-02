/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Alexander Falkenstern
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

package org.jenkinsci.plugins.pipeline.utility.steps.tar;

import hudson.model.Label;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;

import static org.jenkinsci.plugins.pipeline.utility.steps.FilenameTestsUtils.separatorsToSystemEscaped;
import static org.junit.Assert.assertFalse;

/**
 * Tests for {@link UnTarStep}.
 *
 * @author Alexander Falkenstern &lt;Alexander.Falkenstern@gmail.com&gt;.
 */
public class UnTarStepTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setup() throws Exception {
        j.createOnlineSlave(Label.get("slaves"));
    }

    @Test
    public void configRoundTrip() throws Exception {
        UnTarStep step = new UnTarStep("target/my.tgz");
        step.setDir("base/");
        step.setGlob("**/*.tgz");
        step.setQuiet(false);

        UnTarStep step2 = new StepConfigTester(j).configRoundTrip(step);
        j.assertEqualDataBoundBeans(step, step2);
        assertFalse(step2.isTest());
    }

    @Test
    public void simpleUntar() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  dir('compressIt') {\n" +
                        "    writeFile file: 'hello.txt', text: 'Hello World!'\n" +
                        "    writeFile file: 'hello.dat', text: 'Hello World!'\n" +
                        "    dir('two') {\n" +
                        "      writeFile file: 'hello.txt', text: 'Hello World2!'\n" +
                        "    }\n" +
                        "    tar file: '../hello.tgz'\n" +
                        "  }\n" +
                        "  dir('decompressIt') {\n" +
                        "    untar '../hello.tgz'\n" +
                        "    String txt = readFile 'hello.txt'\n" +
                        "    echo \"Reading: ${txt}\"\n" +
                        "  }\n" +
                        "}", true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        j.assertLogContains("Extracting: hello.txt ->", run);
        j.assertLogContains("Extracting: two/hello.txt ->", run);
        j.assertLogContains("Extracting: hello.dat ->", run);
        j.assertLogContains("Reading: Hello World!", run);
    }

    @Test
    public void globUntar() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  dir('compressIt') {\n" +
                        "    writeFile file: 'hello.txt', text: 'Hello World!'\n" +
                        "    writeFile file: 'hello.dat', text: 'Hello World!'\n" +
                        "    dir('two') {\n" +
                        "      writeFile file: 'hello.txt', text: 'Hello World2!'\n" +
                        "    }\n" +
                        "    tar file: '../hello.tar.gz'\n" +
                        "  }\n" +
                        "  dir('decompressIt') {\n" +
                        "    untar file: '../hello.tar.gz', glob: '**/*.txt'\n" +
                        "    String txt = readFile 'hello.txt'\n" +
                        "    echo \"Reading: ${txt}\"\n" +
                        "    txt = readFile 'two/hello.txt'\n" +
                        "    echo \"Reading: ${txt}\"\n" +
                        "  }\n" +
                        "}", true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        j.assertLogContains("Extracting: hello.txt ->", run);
        j.assertLogContains("Extracting: two/hello.txt ->", run);
        j.assertLogNotContains("Extracting: hello.dat ->", run);
        j.assertLogContains("Reading: Hello World!", run);
        j.assertLogContains("Reading: Hello World2!", run);
    }

    @Test
    public void tarTest() throws Exception {
        Assume.assumeTrue("Can only run in a gnu unix environment", File.pathSeparatorChar == ':');
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  dir('compressIt') {\n" +
                        "    writeFile file: 'hello.txt', text: 'Hello World!'\n" +
                        "    writeFile file: 'hello.dat', text: 'Hello Data World!'\n" +
                        "    dir('two') {\n" +
                        "      writeFile file: 'hello.txt', text: 'Hello World2!'\n" +
                        "    }\n" +
                        "    tar file: '../hello.tgz'\n" +
                        "  }\n" +
                        "  sh 'head -c $(($(cat hello.tgz | wc -c) / 2)) hello.tgz > corrupt.tgz'\n" +
                        "  def result = untar file: 'corrupt.tgz', test: true \n" +
                        "  if (result != false)\n" +
                        "      error('Should be corrupt!')\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void testTarGZTestingBrokenTarGZ() throws Exception {
        /*
         This test uses a prepared tgz file that has a single flipped bit inside the byte stream of the tgz file entry.
         The test method has to find this error. This will require the stream to be read, because the CRC check is able
         to reveal this error.
         */
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        URL resource = getClass().getResource("test_broken.tar.gz");
        String tgz = new File(URLDecoder.decode(resource.getPath(), "UTF-8")).getAbsolutePath().replace('\\', '/');
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                "  def result = untar file: '" + separatorsToSystemEscaped(tgz) + "', test: true\n" +
                "  if (result)\n" +
                "      error('Should be corrupt!')\n" +
                "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void testTarGZTestingOkayTarGZ() throws Exception {
        /*
         This test uses a prepared tar.gz file without any errors.
         */
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        URL resource = getClass().getResource("test_ok.tar.gz");
        String tgz = new File(URLDecoder.decode(resource.getPath(), "UTF-8")).getAbsolutePath().replace('\\', '/');
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                "  def result = untar file: '" + separatorsToSystemEscaped(tgz) + "', test: true\n" +
                "  if (!result)\n" +
                "      error('Should be okay!')\n" +
                "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void testTarTestingBrokenTar() throws Exception {
        /*
         This test uses a prepared tgz file that has a single flipped bit inside the byte stream of the tgz file entry.
         The test method has to find this error. This will require the stream to be read, because the CRC check is able
         to reveal this error.
         */
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        URL resource = getClass().getResource("test_broken.tar");
        String tgz = new File(URLDecoder.decode(resource.getPath(), "UTF-8")).getAbsolutePath().replace('\\', '/');
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "  def result = untar file: '" + separatorsToSystemEscaped(tgz) + "', test: true\n" +
                        "  if (result)\n" +
                        "      error('Should be corrupt!')\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void testTarTestingOkayTar() throws Exception {
        /*
         This test uses a prepared tar.gz file without any errors.
         */
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        URL resource = getClass().getResource("test_ok.tar");
        String tgz = new File(URLDecoder.decode(resource.getPath(), "UTF-8")).getAbsolutePath().replace('\\', '/');
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "  def result = untar file: '" + separatorsToSystemEscaped(tgz) + "', test: true\n" +
                        "  if (!result)\n" +
                        "      error('Should be okay!')\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void untarQuiet() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  dir('compressIt') {\n" +
                        "    writeFile file: 'hello.txt', text: 'Hello World!'\n" +
                        "    writeFile file: 'hello.dat', text: 'Hello World!'\n" +
                        "    dir('two') {\n" +
                        "      writeFile file: 'hello.txt', text: 'Hello World2!'\n" +
                        "    }\n" +
                        "    tar file: '../hello.tgz'\n" +
                        "  }\n" +
                        "  dir('decompressIt') {\n" +
                        "    untar file: '../hello.tgz', quiet: true\n" +
                        "    String txt = readFile 'hello.txt'\n" +
                        "    echo \"Reading: ${txt}\"\n" +
                        "    txt = readFile 'two/hello.txt'\n" +
                        "    echo \"Reading: ${txt}\"\n" +
                        "  }\n" +
                        "}", true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        j.assertLogNotContains("Extracting: hello.txt ->", run);
        j.assertLogNotContains("Extracting: two/hello.txt ->", run);
        j.assertLogNotContains("Extracting: hello.dat ->", run);
        j.assertLogContains("Extracted: 3 files", run);
        j.assertLogContains("Reading: Hello World!", run);
        j.assertLogContains("Reading: Hello World2!", run);
    }
    @Test
    public void untarKeepPermissions() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  dir('compressIt') {\n" +
                        "    writeFile file: 'hello.txt', text: 'Hello World!'\n" +
                        "    if (isUnix()) {\n" +
                        "      sh 'chmod +x ./hello.txt'\n" +
                        "    }\n" +
                        "    tar file: '../hello.tgz'\n" +
                        "  }\n" +
                        "  dir('decompressIt') {\n" +
                        "    untar file: '../hello.tgz', quiet: true\n" +
                        "    String txt = ''\n" +
                        "    if (isUnix()) {\n" +
                        "      txt = sh(script: 'if [ -x ./hello.txt ]; then cat ./hello.txt; else echo false; fi', returnStdout: true).trim()\n" +
                        "    } else {\n" +
                        "      txt = readFile 'hello.txt'\n" +
                        "    }\n" +
                        "    echo \"${txt}\"" +
                        "  }\n" +
                        "}", true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        j.assertLogContains("Hello World!", run);
    }
}
