package org.jenkinsci.plugins.pipeline.utility.steps.fs;

import hudson.model.Label;
import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class FileSha1VerifyStepTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setup() throws Exception {
        j.createOnlineSlave(Label.get("slaves"));
    }

    @Test
    public void configRoundTrip() throws Exception {
        FileVerifySha1Step step = new FileVerifySha1Step("f.txt", "hash");
        FileVerifySha1Step step2 = new StepConfigTester(j).configRoundTrip(step);
        j.assertEqualDataBoundBeans(step, step2);
    }

    @Test
    public void emptyFile() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  dir('test') {\n" +
                        "    touch 'empty.txt'\n" +
                        "    verifySha1(file: 'empty.txt', hash: 'da39a3ee5e6b4b0d3255bfef95601890afd80709')\n" +
                        "  }\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void ignoresHashCase() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  dir('test') {\n" +
                        "    touch 'empty.txt'\n" +
                        "    verifySha1(file: 'empty.txt', hash: 'DA39A3EE5E6B4B0D3255BFEF95601890AFD80709')\n" +
                        "  }\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void fileWithContent() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  dir('test') {\n" +
                        "    writeFile file: 'f.txt', text: 'abc', encoding: 'UTF-8'\n" +
                        "    verifySha1(file: 'f.txt', hash: 'a9993e364706816aba3e25717850c26c9cd0d89d')\n" +
                        "  }\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void failsOnMismatch() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  dir('test') {\n" +
                        "    writeFile file: 'fail.txt', text: 'should fail with invalid hash', encoding: 'UTF-8'\n" +
                        "    verifySha1(file: 'fail.txt', hash: 'a9993e364706816aba3e25717850c26c9cd0d89d')\n" +
                        "  }\n" +
                        "}", true));
        final WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0));
        j.assertLogContains("SHA1 hash mismatch", run);
    }

    @Test
    public void failsIfFileNotFound() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  dir('test') {\n" +
                        "    verifySha1(file: 'nonexistent.txt', hash: 'a9993e364706816aba3e25717850c26c9cd0d89d')\n" +
                        "  }\n" +
                        "}", true));
        final WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0));
        j.assertLogContains("File not found: nonexistent.txt", run);
    }
}
