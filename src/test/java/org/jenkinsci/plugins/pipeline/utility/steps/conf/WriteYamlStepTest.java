package org.jenkinsci.plugins.pipeline.utility.steps.conf;

import hudson.model.Label;
import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

public class WriteYamlStepTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Before
    public void setup() throws Exception {
        j.createOnlineSlave(Label.get("slaves"));
    }

    @Test
    public void writeInvalidMap() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "def l = [] \n" +
                        "node('slaves') {\n" +
                        "  writeYaml file: 'test', data: /['a': ]/ \n" +
                        "  def yml = readYaml file: 'test' \n" +
                        "  assert yml == /['a': ]/ \n" +
                        "}",
                true));
        WorkflowRun b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

	@Test
	public void writeArbitraryObject() throws Exception {
		WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
		p.setDefinition(new CpsFlowDefinition(
                 "def l = [] \n" +
                        "node('slaves') {\n" +
						"   writeYaml file: 'test', data: new Date() \n" +
                         "  def yml = readYaml file: 'test' \n" +
                         "  assert yml =~ /2\\d{3}/ \n" +
						"}",
				true));
        WorkflowRun b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
	}

    @Test
    public void writeMapObject() throws Exception {
		WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "map");
		p.setDefinition(new CpsFlowDefinition(
				  "node('slaves') {\n" +
						 "  writeYaml file: 'test', data: ['a': 1, 'b': 2] \n" +
                         "  def yml = readYaml file: 'test' \n" +
                         "  assert yml == ['a' : 1, 'b': 2] \n" +
						 "}",
				true));
		WorkflowRun b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));

    }

    @Test
    public void writeListObjectAndRead() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "list");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  writeYaml file: 'test', data: ['a', 'b', 'c'] \n" +
                        "  def yml = readYaml file: 'test' \n" +
                        "  assert yml == ['a','b','c'] \n"+
                        "}",
                true));
        WorkflowRun b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void writeExistingFile() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "list");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        " sh 'touch test' \n" +
                        "  writeYaml file: 'test', data: ['a', 'b', 'c'] \n" +
                        "}",
                true));
        WorkflowRun b = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains("FileAlreadyExistsException", b);
    }

    @Test
     public void writeNoData() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("node('slaves') {\n" + "  writeYaml file: 'some' \n" + "}", true));
        WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains("data parameter must be provided to writeYaml", run);
    }

    @Test
    public void writeNoFile() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("node('slaves') {\n" + "  writeYaml data: 'some' \n" + "}", true));
        WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains("file parameter must be provided to writeYaml", run);
    }
}
