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

package org.jenkinsci.plugins.pipeline.utility.steps.zip;

import static org.junit.jupiter.api.Assertions.*;

import hudson.model.Label;
import hudson.model.Result;
import hudson.model.Run;
import java.io.IOException;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import jenkins.util.VirtualFile;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Tests for {@link ZipStep}.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
@WithJenkins
class ZipStepTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        j = rule;
        j.createOnlineSlave(Label.get("slaves"));
    }

    @Test
    void configRoundTrip() throws Exception {
        ZipStep step = new ZipStep("target/my.zip");
        step.setDir("base/");
        step.setGlob("**/*.zip");
        step.setExclude("**/*.txt");
        step.setArchive(true);
        step.setOverwrite(true);

        ZipStep step2 = new StepConfigTester(j).configRoundTrip(step);
        j.assertEqualDataBoundBeans(step, step2);
    }

    @Test
    void simpleArchivedZip() throws Exception {

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          writeFile file: 'hello.txt', text: 'Hello Outer World!'
                          dir('hello') {
                            writeFile file: 'hello.txt', text: 'Hello World!'
                          }
                          zip zipFile: 'hello.zip', dir: 'hello', archive: true
                        }""",
                true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        j.assertLogContains("Compress", run);
        j.assertLogContains("Archiving", run);
        verifyArchivedHello(run, "");
    }

    @Test
    void shouldNotPutOutputArchiveIntoItself() throws Exception {

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node {\
                          writeFile file: 'hello.txt', text: 'Hello world'
                          zip zipFile: 'output.zip', dir: '', glob: '', archive: true, overwrite: true
                        }""",
                true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        j.assertLogContains("Compress", run);
        verifyArchivedNotContainingItself(run);
    }

    @Test
    @Issue("JENKINS-62928")
    void shouldNotPutOutputArchiveIntoItself_nonCanonicalPath() throws Exception {

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node {\
                          dir ('src') {
                           writeFile file: 'hello.txt', text: 'Hello world'
                          }
                          zip zipFile: 'src/../src/output.zip', dir: '', glob: '', archive: true
                        }""",
                true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        j.assertLogContains("Compress", run);
        verifyArchivedNotContainingItself(run);
    }

    @Test
    void canArchiveFileWithSameName() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node {
                          dir ('src') {
                           writeFile file: 'hello.txt', text: 'Hello world'
                           writeFile file: 'output.zip', text: 'not really a zip'
                          }
                          dir ('out') {
                            zip zipFile: 'output.zip', dir: '../src', glob: '', archive: true, overwrite: true
                          }
                        }
                        """,
                true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));

        j.assertLogContains("Compress", run);
        assertTrue(run.getHasArtifacts(), "Build should have artifacts");
        Run<WorkflowJob, WorkflowRun>.Artifact artifact = run.getArtifacts().get(0);
        assertEquals("output.zip", artifact.getFileName());
        VirtualFile file = run.getArtifactManager().root().child(artifact.relativePath);
        try (ZipInputStream zip = new ZipInputStream(file.open())) {
            ZipEntry entry = zip.getNextEntry();
            while (entry != null && !entry.getName().equals("output.zip")) {
                System.out.println("zip entry name is: " + entry.getName());
                entry = zip.getNextEntry();
            }
            assertNotNull(entry, "output.zip should be included in the zip");
            // we should have the the zip - but double check
            assertEquals("output.zip", entry.getName());
            Scanner scanner = new Scanner(zip);
            assertTrue(scanner.hasNextLine());
            // the file that was not a zip should be included.
            assertEquals("not really a zip", scanner.nextLine());
        }
    }

    @Test
    void globbedArchivedZip() throws Exception {

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          writeFile file: 'hello.outer', text: 'Hello Outer World!'
                          dir('hello') {
                            writeFile file: 'hello.txt', text: 'Hello World!'
                          }
                          zip zipFile: 'hello.zip', glob: '**/*.txt', archive: true
                        }""",
                true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        verifyArchivedHello(run, "hello/");
    }

    @Test
    void excludedPatternWithAll() throws Exception {

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          writeFile file: 'hello.txt', text: 'Hello World!'
                          writeFile file: 'goodbye.txt', text: 'Goodbye World!'
                          zip zipFile: 'hello.zip', exclude: 'goodbye.txt', archive: true
                        }""",
                true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        verifyArchivedHello(run, "");
    }

    @Test
    void excludedPatternWithGlob() throws Exception {

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          writeFile file: 'hello.txt', text: 'Hello World!'
                          writeFile file: 'goodbye.txt', text: 'Goodbye World!'
                          zip zipFile: 'hello.zip', glob: '*.txt', exclude: 'goodbye.txt', archive: true
                        }""",
                true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        verifyArchivedHello(run, "");
    }

    @Test
    void emptyZipFile() throws Exception {

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          zip zipFile: '', glob: '**/*.txt', archive: true
                        }""",
                true));
        WorkflowRun run =
                j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains("Can not be empty", run);
    }

    @Test
    void existingZipFileWithoutOverwrite() throws Exception {

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          writeFile file: 'hello.zip', text: 'Hello Zip!'
                          zip zipFile: 'hello.zip', glob: '**/*.txt', archive: true
                        }""",
                true));
        WorkflowRun run =
                j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains("hello.zip exists.", run);
    }

    @Test
    void existingZipFileWithOverwrite() throws Exception {

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          writeFile file: 'hello.zip', text: 'Hello Zip!'
                          writeFile file: 'hello.txt', text: 'Hello world'
                          zip zipFile: 'hello.zip', glob: '**/*.txt', archive: true, overwrite:true
                        }""",
                true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0).get());
        j.assertLogNotContains("hello.zip exists.", run);
    }

    @Test
    void noExistingZipFileWithOverwrite() throws Exception {

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          writeFile file: 'hello.txt', text: 'Hello world'
                          zip zipFile: 'hello.zip', glob: '**/*.txt', overwrite:true
                        }""",
                true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0).get());
        j.assertLogNotContains("java.io.IOException", run);
        j.assertLogNotContains("Failed to delete", run);
        j.assertLogContains("Compressed 1 entries.", run);
    }

    private void verifyArchivedHello(WorkflowRun run, String basePath) throws IOException {
        assertTrue(run.getHasArtifacts(), "Build should have artifacts");
        Run<WorkflowJob, WorkflowRun>.Artifact artifact = run.getArtifacts().get(0);
        assertEquals("hello.zip", artifact.getFileName());
        VirtualFile file = run.getArtifactManager().root().child(artifact.relativePath);
        try (ZipInputStream zip = new ZipInputStream(file.open())) {
            ZipEntry entry = zip.getNextEntry();
            while (entry.isDirectory()) {
                entry = zip.getNextEntry();
            }
            assertNotNull(entry);
            assertEquals(basePath + "hello.txt", entry.getName());
            try (Scanner scanner = new Scanner(zip)) {
                assertTrue(scanner.hasNextLine());
                assertEquals("Hello World!", scanner.nextLine());
                assertNull(zip.getNextEntry(), "There should be no more entries");
            }
        }
    }

    private void verifyArchivedNotContainingItself(WorkflowRun run) throws IOException {
        assertTrue(run.getHasArtifacts(), "Build should have artifacts");
        Run<WorkflowJob, WorkflowRun>.Artifact artifact = run.getArtifacts().get(0);
        VirtualFile file = run.getArtifactManager().root().child(artifact.relativePath);
        try (ZipInputStream zip = new ZipInputStream(file.open())) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                assertNotEquals(entry.getName(), artifact.relativePath, "The zip output file shouldn't contain itself");
            }
        }
    }

    @Test
    void defaultExcludesPatternWithAll() throws Exception {

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          writeFile file: '.gitignore', text: '/target'
                          zip zipFile: 'hello.zip', defaultExcludes: false, archive: true
                        }""",
                true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        verifyArchivedDefaultExcludes(run, "");
    }

    private void verifyArchivedDefaultExcludes(WorkflowRun run, String basePath) throws IOException {
        assertTrue(run.getHasArtifacts(), "Build should have artifacts");
        Run<WorkflowJob, WorkflowRun>.Artifact artifact = run.getArtifacts().get(0);
        assertEquals("hello.zip", artifact.getFileName());
        VirtualFile file = run.getArtifactManager().root().child(artifact.relativePath);
        try (ZipInputStream zip = new ZipInputStream(file.open())) {
            ZipEntry entry = zip.getNextEntry();
            while (entry.isDirectory()) {
                entry = zip.getNextEntry();
            }
            assertNotNull(entry);
            assertEquals(basePath + ".gitignore", entry.getName());
            try (Scanner scanner = new Scanner(zip)) {
                assertTrue(scanner.hasNextLine());
                assertEquals("/target", scanner.nextLine());
                assertNull(zip.getNextEntry(), "There should be no more entries");
            }
        }
    }

    @Test
    void defaultExcludesPatternEnabled() throws Exception {

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          writeFile file: '.gitignore', text: '/target'
                          zip zipFile: 'hello.zip', defaultExcludes: true, archive: true
                        }""",
                true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        verifyArchivedNotContainingItself(run);
    }
}
