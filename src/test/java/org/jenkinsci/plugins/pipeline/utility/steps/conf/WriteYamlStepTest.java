package org.jenkinsci.plugins.pipeline.utility.steps.conf;

import hudson.model.Label;
import hudson.model.Result;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
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
    public void writeListOfDocumentsAndRead() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "list");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  writeYaml file: 'test', datas: [['a': 1, 'b': 2], ['a': 3, 'b': 4]] \n" +
                        "  def yml = readYaml file: 'test' \n" +
                        "  assert yml == [['a': 1, 'b': 2], ['a': 3, 'b': 4]] \n"+
                        "}",
                true));
        WorkflowRun b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void writeExistingFile() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "list");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  touch 'test.yml' \n" +
                        "  writeYaml file: 'test.yml', data: ['a', 'b', 'c'] \n" +
                        "}",
                true));
        WorkflowRun b = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains("FileAlreadyExistsException", b);
    }

    @Test
    public void overwriteExistingFile() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "list");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  writeFile file: 'test.yml', text: 'overwrite me' \n" +
                        "  writeYaml file: 'test.yml', overwrite: true, data: 'overwritten' \n" +
                        "  final text = readFile file: 'test.yml' \n" +
                        "  if (text != 'overwritten\\n') error('got ' + text) \n" +
                        "}",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void writeGStringWithoutApproval() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "gstringjobnoapproval");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  writeYaml file: 'test', data: \"${currentBuild.rawBuild}\" \n" +
                        "  def yml = readYaml file: 'test' \n" +
                        "  assert yml == 'gstringjob#1' \n"+
                        "}",
                true));
        WorkflowRun b = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains("Scripts not permitted to use method org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper getRawBuild", b);
    }

    @Test
    public void writeGString() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "gstringjob");
        final ScriptApproval approval = ScriptApproval.get();
        approval.approveSignature("method org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper getRawBuild");
        approval.approveSignature("method hudson.model.Run getExternalizableId");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  writeYaml file: 'test', data: \"${currentBuild.rawBuild.externalizableId}\" \n" +
                        "  def yml = readYaml file: 'test' \n" +
                        "  echo yml \n"+
                        "  assert yml == 'gstringjob#1' \n"+
                        "}",
                true));
        WorkflowRun b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
     public void writeNoData() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("node('slaves') {\n" + "  writeYaml file: 'some' \n" + "}", true));
        WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains("data or datas parameter must be provided to writeYaml", run);
    }

    @Test
     public void writeDataAndDatas() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
         p.setDefinition(new CpsFlowDefinition(
                 "node('slaves') {\n" +
                         "  writeYaml file: 'test', data: ['a': 1, 'b': 2], datas: [['a': 1], ['b': 2]] \n" +
                         "}",
                 true));
        WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains("only one of data or datas must be provided to writeYaml", run);
    }

    @Test
    public void writeNoFile() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("node('slaves') {\n" + "  writeYaml data: 'some', file: '' \n" + "}", true));
        WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains("either file or returnText must be provided to writeYaml", run);
    }

    @Test
    public void invalidDataObject() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "list");
        ScriptApproval.get().approveSignature("staticMethod java.util.TimeZone getDefault");
        p.setDefinition(new CpsFlowDefinition(
                        "node('slaves') {\n" +
                        "  def tz = TimeZone.getDefault() \n" +
                        "  echo \"${tz}\"  \n" +
                        "  writeYaml file: 'test', data: TimeZone.getDefault()\n" +
                        "}",
                true));
        WorkflowRun b = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains("data parameter has invalid content (no-basic classes)", b);
    }

    @Test
    public void validComplexDataObject() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "list");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  def data = [['a': 1, 'b' : '2', true: false], true, null, 'str'] \n" +
                        "  echo \"${data}\"  \n" +
                        "  writeYaml file: 'test', data: data\n" +
                        "}",
                true));
        WorkflowRun b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void invalidComplexDataObject() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "list");
        ScriptApproval.get().approveSignature("staticMethod java.util.TimeZone getDefault");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  def data = [true, null, ['a': 1, 'b' : '2', true: false, 'tz': TimeZone.getDefault()], 'str'] \n" +
                        "  echo \"${data}\"  \n" +
                        "  writeYaml file: 'test', data: data\n" +
                        "}",
                true));
        WorkflowRun b = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains("data parameter has invalid content (no-basic classes)", b);
    }

    @Test
    public void returnText() throws Exception {
		WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "map");
		p.setDefinition(new CpsFlowDefinition(
				  "node('slaves') {\n" +
						 "  String written = writeYaml returnText: true, data: ['a': 1, 'b': 2] \n" +
                         "  def yml = readYaml text: written \n" +
                         "  assert yml == ['a' : 1, 'b': 2] \n" +
						 "}",
				true));
		WorkflowRun b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }
}
