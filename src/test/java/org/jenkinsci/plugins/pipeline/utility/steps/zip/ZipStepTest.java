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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
}