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

import static org.jenkinsci.plugins.pipeline.utility.steps.FilenameTestsUtils.separatorsToSystemEscaped;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import hudson.model.Label;
import hudson.model.Result;
import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.jenkinsci.plugins.pipeline.utility.steps.DecompressStepExecution;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Tests for {@link UnTarStep}.
 *
 * @author Alexander Falkenstern &lt;Alexander.Falkenstern@gmail.com&gt;.
 */
@WithJenkins
class UnTarStepTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        j = rule;
        j.createOnlineSlave(Label.get("slaves"));
    }

    @Test
    void configRoundTrip() throws Exception {
        UnTarStep step = new UnTarStep("target/my.tgz");
        step.setDir("base/");
        step.setGlob("**/*.tgz");
        step.setQuiet(false);

        UnTarStep step2 = new StepConfigTester(j).configRoundTrip(step);
        j.assertEqualDataBoundBeans(step, step2);
        assertFalse(step2.isTest());
    }

    @Test
    void simpleUntar() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          dir('compressIt') {
                            writeFile file: 'hello.txt', text: 'Hello World!'
                            writeFile file: 'hello.dat', text: 'Hello World!'
                            dir('two') {
                              writeFile file: 'hello.txt', text: 'Hello World2!'
                            }
                            tar file: '../hello.tgz'
                          }
                          dir('decompressIt') {
                            untar '../hello.tgz'
                            String txt = readFile 'hello.txt'
                            echo "Reading: ${txt}"
                          }
                        }""",
                true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        j.assertLogContains("Extracting: hello.txt ->", run);
        j.assertLogContains("Extracting: two/hello.txt ->", run);
        j.assertLogContains("Extracting: hello.dat ->", run);
        j.assertLogContains("Reading: Hello World!", run);
    }

    @Test
    void globUntar() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          dir('compressIt') {
                            writeFile file: 'hello.txt', text: 'Hello World!'
                            writeFile file: 'hello.dat', text: 'Hello World!'
                            dir('two') {
                              writeFile file: 'hello.txt', text: 'Hello World2!'
                            }
                            tar file: '../hello.tar.gz'
                          }
                          dir('decompressIt') {
                            untar file: '../hello.tar.gz', glob: '**/*.txt'
                            String txt = readFile 'hello.txt'
                            echo "Reading: ${txt}"
                            txt = readFile 'two/hello.txt'
                            echo "Reading: ${txt}"
                          }
                        }""",
                true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        j.assertLogContains("Extracting: hello.txt ->", run);
        j.assertLogContains("Extracting: two/hello.txt ->", run);
        j.assertLogNotContains("Extracting: hello.dat ->", run);
        j.assertLogContains("Reading: Hello World!", run);
        j.assertLogContains("Reading: Hello World2!", run);
    }

    @Test
    void tarTest() throws Exception {
        Assumptions.assumeTrue(File.pathSeparatorChar == ':', "Can only run in a gnu unix environment");
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          dir('compressIt') {
                            writeFile file: 'hello.txt', text: 'Hello World!'
                            writeFile file: 'hello.dat', text: 'Hello Data World!'
                            dir('two') {
                              writeFile file: 'hello.txt', text: 'Hello World2!'
                            }
                            tar file: '../hello.tgz'
                          }
                          sh 'head -c $(($(cat hello.tgz | wc -c) / 2)) hello.tgz > corrupt.tgz'
                          def result = untar file: 'corrupt.tgz', test: true
                          if (result != false)
                              error('Should be corrupt!')
                        }""",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void testTarGZTestingBrokenTarGZ() throws Exception {
        /*
        This test uses a prepared tgz file that has a single flipped bit inside the byte stream of the tgz file entry.
        The test method has to find this error. This will require the stream to be read, because the CRC check is able
        to reveal this error.
        */
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        URL resource = getClass().getResource("test_broken.tar.gz");
        String tgz = new File(URLDecoder.decode(resource.getPath(), StandardCharsets.UTF_8))
                .getAbsolutePath()
                .replace('\\', '/');
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" + "  def result = untar file: '"
                        + separatorsToSystemEscaped(tgz) + "', test: true\n" + "  if (result)\n"
                        + "      error('Should be corrupt!')\n"
                        + "}",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void testTarGZTestingOkayTarGZ() throws Exception {
        /*
        This test uses a prepared tar.gz file without any errors.
        */
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        URL resource = getClass().getResource("test_ok.tar.gz");
        String tgz = new File(URLDecoder.decode(resource.getPath(), StandardCharsets.UTF_8))
                .getAbsolutePath()
                .replace('\\', '/');
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" + "  def result = untar file: '"
                        + separatorsToSystemEscaped(tgz) + "', test: true\n" + "  if (!result)\n"
                        + "      error('Should be okay!')\n"
                        + "}",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void testTarTestingBrokenTar() throws Exception {
        /*
        This test uses a prepared tgz file that has a single flipped bit inside the byte stream of the tgz file entry.
        The test method has to find this error. This will require the stream to be read, because the CRC check is able
        to reveal this error.
        */
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        URL resource = getClass().getResource("test_broken.tar");
        String tgz = new File(URLDecoder.decode(resource.getPath(), StandardCharsets.UTF_8))
                .getAbsolutePath()
                .replace('\\', '/');
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" + "  def result = untar file: '"
                        + separatorsToSystemEscaped(tgz) + "', test: true\n" + "  if (result)\n"
                        + "      error('Should be corrupt!')\n"
                        + "}",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void testTarTestingOkayTar() throws Exception {
        /*
        This test uses a prepared tar.gz file without any errors.
        */
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        URL resource = getClass().getResource("test_ok.tar");
        String tgz = new File(URLDecoder.decode(resource.getPath(), StandardCharsets.UTF_8))
                .getAbsolutePath()
                .replace('\\', '/');
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" + "  def result = untar file: '"
                        + separatorsToSystemEscaped(tgz) + "', test: true\n" + "  if (!result)\n"
                        + "      error('Should be okay!')\n"
                        + "}",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void untarQuiet() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          dir('compressIt') {
                            writeFile file: 'hello.txt', text: 'Hello World!'
                            writeFile file: 'hello.dat', text: 'Hello World!'
                            dir('two') {
                              writeFile file: 'hello.txt', text: 'Hello World2!'
                            }
                            tar file: '../hello.tgz'
                          }
                          dir('decompressIt') {
                            untar file: '../hello.tgz', quiet: true
                            String txt = readFile 'hello.txt'
                            echo "Reading: ${txt}"
                            txt = readFile 'two/hello.txt'
                            echo "Reading: ${txt}"
                          }
                        }""",
                true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        j.assertLogNotContains("Extracting: hello.txt ->", run);
        j.assertLogNotContains("Extracting: two/hello.txt ->", run);
        j.assertLogNotContains("Extracting: hello.dat ->", run);
        j.assertLogContains("Extracted: 3 files", run);
        j.assertLogContains("Reading: Hello World!", run);
        j.assertLogContains("Reading: Hello World2!", run);
    }

    @Test
    void untarKeepPermissions() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          dir('compressIt') {
                            writeFile file: 'hello.txt', text: 'Hello World!'
                            if (isUnix()) {
                              sh 'chmod +x ./hello.txt'
                            }
                            tar file: '../hello.tgz'
                          }
                          dir('decompressIt') {
                            untar file: '../hello.tgz', quiet: true
                            String txt = ''
                            if (isUnix()) {
                              txt = sh(script: 'if [ -x ./hello.txt ]; then cat ./hello.txt; else echo false; fi', returnStdout: true).trim()
                            } else {
                              txt = readFile 'hello.txt'
                            }
                            echo "${txt}"\
                          }
                        }""",
                true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        j.assertLogContains("Hello World!", run);
    }

    @Test
    @Issue("SECURITY-2196")
    void testingAbsolutePathsShouldFail() throws Exception {
        assumeTrue(File.pathSeparatorChar == ':', "Can only run in a gnu unix environment");
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        URL resource = getClass().getResource("absolute.tar");
        String tgz = new File(URLDecoder.decode(resource.getPath(), StandardCharsets.UTF_8))
                .getAbsolutePath()
                .replace('\\', '/');
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" + "  def result = untar file: '"
                        + separatorsToSystemEscaped(tgz) + "', test: true\n" + "  if (result)\n"
                        + "      error('Should be fail!')\n"
                        + "}",
                true));
        WorkflowRun run = j.buildAndAssertSuccess(p);
        j.assertLogContains("is out of bounds!", run);
    }

    @Test
    @Issue("SECURITY-2196")
    void testingAbsolutePathsShouldNotFailWithEscapeHatch() throws Exception {
        assumeTrue(File.pathSeparatorChar == ':', "Can only run in a gnu unix environment");
        try {
            DecompressStepExecution.ALLOW_EXTRACTION_OUTSIDE_DESTINATION = true;
            j.createOnlineSlave(Label.get("bbb"));
            WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
            URL resource = getClass().getResource("absolute.tar");
            String tgz = new File(URLDecoder.decode(resource.getPath(), StandardCharsets.UTF_8))
                    .getAbsolutePath()
                    .replace('\\', '/');
            p.setDefinition(new CpsFlowDefinition(
                    "node('bbb') {\n" + "  def result = untar file: '"
                            + separatorsToSystemEscaped(tgz) + "', test: true\n" + "  if (!result)\n"
                            + "      error('Should not be fail!')\n"
                            + "}",
                    true));
            WorkflowRun run = j.buildAndAssertSuccess(p);
            j.assertLogNotContains("is out of bounds!", run);
        } finally {
            DecompressStepExecution.ALLOW_EXTRACTION_OUTSIDE_DESTINATION = false;
        }
    }

    @Test
    @Issue("SECURITY-2196")
    void absolutePathsShouldFailBuild() throws Exception {
        assumeTrue(File.pathSeparatorChar == ':', "Can only run in a gnu unix environment");
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        URL resource = getClass().getResource("absolute.tar");
        String tgz = new File(URLDecoder.decode(resource.getPath(), StandardCharsets.UTF_8))
                .getAbsolutePath()
                .replace('\\', '/');
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" + "  untar file: '" + separatorsToSystemEscaped(tgz) + "'\n" + "}", true));
        WorkflowRun run = j.buildAndAssertStatus(Result.FAILURE, p);
        j.assertLogContains("is out of bounds!", run);
    }

    @Test
    @Issue("SECURITY-2196")
    void absolutePathsShouldNotFailBuildWithEscapeHatch() throws Exception {
        assumeTrue(File.pathSeparatorChar == ':', "Can only run in a gnu unix environment");
        try {
            DecompressStepExecution.ALLOW_EXTRACTION_OUTSIDE_DESTINATION = true;

            WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
            URL resource = getClass().getResource("absolute.tar");
            String tgz = new File(URLDecoder.decode(resource.getPath(), StandardCharsets.UTF_8))
                    .getAbsolutePath()
                    .replace('\\', '/');
            p.setDefinition(new CpsFlowDefinition(
                    "node {\n" + "  untar file: '" + separatorsToSystemEscaped(tgz) + "'\n" + "}", true));
            WorkflowRun run = j.buildAndAssertStatus(Result.SUCCESS, p);
            j.assertLogNotContains("is out of bounds!", run);
        } finally {
            DecompressStepExecution.ALLOW_EXTRACTION_OUTSIDE_DESTINATION = false;
        }
    }
}
