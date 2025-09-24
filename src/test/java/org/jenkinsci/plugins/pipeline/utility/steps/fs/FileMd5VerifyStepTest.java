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

public class FileMd5VerifyStepTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setup() throws Exception {
        j.createOnlineSlave(Label.get("remote"));
    }

    @Test
    public void configRoundTrip() throws Exception {
        FileVerifyMd5Step step = new FileVerifyMd5Step("f.txt", "hash");
        FileVerifyMd5Step step2 = new StepConfigTester(j).configRoundTrip(step);
        j.assertEqualDataBoundBeans(step, step2);
    }

    @Test
    public void emptyFile() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                  node('remote') {
                    dir('test') {
                      touch 'empty.txt'
                      verifyMd5(file: 'empty.txt', hash: 'd41d8cd98f00b204e9800998ecf8427e')
                    }
                  }
                """,
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void ignoresHashCase() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                  node('remote') {
                    dir('test') {
                     touch 'empty.txt'
                     verifyMd5(file: 'empty.txt', hash: 'D41D8CD98F00B204E9800998ECF8427E')
                    }
                  }
                """,
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void fileWithContent() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                  node('remote') {
                    dir('test') {
                      writeFile file: 'f.txt', text: 'abc', encoding: 'UTF-8'
                      verifyMd5(file: 'f.txt', hash: '900150983cd24fb0d6963f7d28e17f72')
                    }
                  }
                """,
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void failsOnMismatch() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                  node('remote') {
                    dir('test') {
                      writeFile file: 'fail.txt', text: 'should fail with invalid hash', encoding: 'UTF-8'
                      verifyMd5(file: 'fail.txt', hash: '900150983cd24fb0d6963f7d28e17f72')
                    }
                  }
                """,
                true));
        final WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0));
        j.assertLogContains("MD5 hash mismatch", run);
    }

    @Test
    public void failsIfFileNotFound() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                  node('remote') {
                    dir('test') {
                    verifyMd5(file: 'nonexistent.txt', hash: 'd41d8cd98f00b204e9800998ecf8427e')
                  }
                }
                """,
                true));
        final WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0));
        j.assertLogContains("File not found: nonexistent.txt", run);
    }
}
