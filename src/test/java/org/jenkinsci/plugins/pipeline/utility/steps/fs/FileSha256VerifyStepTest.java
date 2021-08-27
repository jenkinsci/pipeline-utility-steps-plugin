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

public class FileSha256VerifyStepTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setup() throws Exception {
        j.createOnlineSlave(Label.get("slaves"));
    }

    @Test
    public void configRoundTrip() throws Exception {
        FileVerifySha256Step step = new FileVerifySha256Step("f.txt", "hash");
        FileVerifySha256Step step2 = new StepConfigTester(j).configRoundTrip(step);
        j.assertEqualDataBoundBeans(step, step2);
    }

    @Test
    public void emptyFile() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  dir('test') {\n" +
                        "    touch 'empty.txt'\n" +
                        "    verifySha256(file: 'empty.txt', hash: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855')\n" +
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
                        "    verifySha256(file: 'empty.txt', hash: 'E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855')\n" +
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
                        "    verifySha256(file: 'f.txt', hash: 'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad')\n" +
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
                        "    verifySha256(file: 'fail.txt', hash: 'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad')\n" +
                        "  }\n" +
                        "}", true));
        final WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0));
        j.assertLogContains("SHA-256 hash mismatch", run);
    }

    @Test
    public void failsIfFileNotFound() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  dir('test') {\n" +
                        "    verifySha256(file: 'nonexistent.txt', hash: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855')\n" +
                        "  }\n" +
                        "}", true));
        final WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0));
        j.assertLogContains("File not found: nonexistent.txt", run);
    }
}
