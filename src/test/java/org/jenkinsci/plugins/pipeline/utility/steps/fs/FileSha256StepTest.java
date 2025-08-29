package org.jenkinsci.plugins.pipeline.utility.steps.fs;

import hudson.model.Label;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class FileSha256StepTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        j = rule;
        j.createOnlineSlave(Label.get("slaves"));
    }

    @Test
    void configRoundTrip() throws Exception {
        FileSha256Step step = new FileSha256Step("dir/f.txt");
        FileSha256Step step2 = new StepConfigTester(j).configRoundTrip(step);
        j.assertEqualDataBoundBeans(step, step2);
    }

    @Test
    void emptyFile() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          dir('test') {
                            touch 'empty.txt'
                            def hash = sha256 'empty.txt'
                            assert hash == 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'
                          }
                        }""",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void fileWithContent() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          dir('test') {
                            writeFile file: 'f.txt', text: 'abc', encoding: 'UTF-8'
                            def hash = sha256 'f.txt'
                            assert hash == 'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad'
                          }
                        }""",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void returnsNullIfFileNotFound() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          dir('test') {
                            def hash = sha256 'not_existing.txt'
                            assert hash == null
                          }
                        }""",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }
}
