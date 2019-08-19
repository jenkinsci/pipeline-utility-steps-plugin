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

import hudson.model.Label;
import hudson.model.Result;
import hudson.model.Run;
import jenkins.util.VirtualFile;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.*;

/**
 * Tests for {@link ZipStep}.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class ZipStepTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setup() throws Exception {
        j.createOnlineSlave(Label.get("slaves"));
    }

    @Test
    public void configRoundTrip() throws Exception {
        ZipStep step = new ZipStep("target/my.zip");
        step.setDir("base/");
        step.setGlob("**/*.zip");
        step.setArchive(true);

        ZipStep step2 = new StepConfigTester(j).configRoundTrip(step);
        j.assertEqualDataBoundBeans(step, step2);
    }

    @Test
    public void simpleArchivedZip() throws Exception {

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  writeFile file: 'hello.txt', text: 'Hello Outer World!'\n" +
                        "  dir('hello') {\n" +
                        "    writeFile file: 'hello.txt', text: 'Hello World!'\n" +
                        "  }\n" +
                        "  zip zipFile: 'hello.zip', dir: 'hello', archive: true\n" +
                        "}", true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        j.assertLogContains("Writing zip file", run);
        j.assertLogContains("Archiving", run);
        verifyArchivedHello(run, "");

    }

    @Test
    public void shouldNotPutOutputArchiveIntoItself() throws Exception {

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
            "node {" +
                "  writeFile file: 'hello.txt', text: 'Hello world'\n" +
                "  zip zipFile: 'output.zip', dir: '', glob: '', archive: true\n" +
                "}", true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        j.assertLogContains("Writing zip file", run);
        verifyArchivedNotContainingItself(run);
    }

    @Test
    public void canArchiveFileWithSameName() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" + 
                "  dir ('src') {\n" +
                "   writeFile file: 'hello.txt', text: 'Hello world'\n" +
                "   writeFile file: 'output.zip', text: 'not really a zip'\n" +
                "  }\n" +
                "  dir ('out') {\n" +
                "    zip zipFile: 'output.zip', dir: '../src', glob: '', archive: true\n" +
                "  }\n" +
                "}\n",
                true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));

        j.assertLogContains("Writing zip file", run);
        assertTrue("Build should have artifacts", run.getHasArtifacts());
        Run<WorkflowJob, WorkflowRun>.Artifact artifact = run.getArtifacts().get(0);
        assertEquals("output.zip", artifact.getFileName());
        VirtualFile file = run.getArtifactManager().root().child(artifact.relativePath);
        ZipInputStream zip = new ZipInputStream(file.open());
        ZipEntry entry = zip.getNextEntry();
        while (entry != null && !entry.getName().equals("output.zip")) {
            System.out.println("zip entry name is: " + entry.getName());
            entry = zip.getNextEntry();
        }
        assertNotNull("output.zip should be included in the zip", entry);
        // we should have the the zip - but double check
        assertEquals("output.zip", entry.getName());
        Scanner scanner = new Scanner(zip);
        assertTrue(scanner.hasNextLine());
        // the file that was not a zip should be included.
        assertEquals("not really a zip", scanner.nextLine());
        zip.close();
    }

    @Test
    public void globbedArchivedZip() throws Exception {

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  writeFile file: 'hello.outer', text: 'Hello Outer World!'\n" +
                        "  dir('hello') {\n" +
                        "    writeFile file: 'hello.txt', text: 'Hello World!'\n" +
                        "  }\n" +
                        "  zip zipFile: 'hello.zip', glob: '**/*.txt', archive: true\n" +
                        "}", true));
        WorkflowRun run = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        verifyArchivedHello(run, "hello/");

    }

    @Test
    public void emptyZipFile() throws Exception {

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  zip zipFile: '', glob: '**/*.txt', archive: true\n" +
                        "}", true));
        WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains("Can not be empty", run);

    }

    @Test
    public void existingZipFile() throws Exception {

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  writeFile file: 'hello.zip', text: 'Hello Zip!'\n" +
                        "  zip zipFile: 'hello.zip', glob: '**/*.txt', archive: true\n" +
                        "}", true));
        WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains("hello.zip exists.", run);

    }

    private void verifyArchivedHello(WorkflowRun run, String basePath) throws IOException {
        assertTrue("Build should have artifacts", run.getHasArtifacts());
        Run<WorkflowJob, WorkflowRun>.Artifact artifact = run.getArtifacts().get(0);
        assertEquals("hello.zip", artifact.getFileName());
        VirtualFile file = run.getArtifactManager().root().child(artifact.relativePath);
        ZipInputStream zip = new ZipInputStream(file.open());
        ZipEntry entry = zip.getNextEntry();
        while (entry.isDirectory()) {
            entry = zip.getNextEntry();
        }
        assertNotNull(entry);
        assertEquals(basePath + "hello.txt", entry.getName());
        try(Scanner scanner = new Scanner(zip)){
	        assertTrue(scanner.hasNextLine());
	        assertEquals("Hello World!", scanner.nextLine());
	        assertNull("There should be no more entries", zip.getNextEntry());
	        zip.close();
        }
	
    }

    private void verifyArchivedNotContainingItself(WorkflowRun run) throws IOException {
        assertTrue("Build should have artifacts", run.getHasArtifacts());
        Run<WorkflowJob, WorkflowRun>.Artifact artifact = run.getArtifacts().get(0);
        VirtualFile file = run.getArtifactManager().root().child(artifact.relativePath);
        ZipInputStream zip = new ZipInputStream(file.open());
        for(ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
            assertNotEquals("The zip output file shouldn't contain itself", entry.getName(), artifact.getFileName());
        }
    }
}