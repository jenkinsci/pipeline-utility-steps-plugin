package org.jenkinsci.plugins.pipeline.utility.steps.fs;

import hudson.model.Label;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class FileMd5StepTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setup() throws Exception {
        j.createOnlineSlave(Label.get("remote"));
    }

    @Test
    public void configRoundTrip() throws Exception {
        FileMd5Step step = new FileMd5Step("dir/f.txt");
        FileMd5Step step2 = new StepConfigTester(j).configRoundTrip(step);
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
                      def hash = md5 'empty.txt'
                      assert hash == 'd41d8cd98f00b204e9800998ecf8427e'
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
                      def hash = md5 'f.txt'
                      assert hash == '900150983cd24fb0d6963f7d28e17f72'
                    }
                  }
                """,
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void returnsNullIfFileNotFound() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                   node('remote') {
                     dir('test') {
                       def hash = md5 'not_existing.txt'
                       assert hash == null
                     }
                   }
                """,
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }
}
