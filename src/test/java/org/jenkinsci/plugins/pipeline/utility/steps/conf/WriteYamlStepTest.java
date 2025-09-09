package org.jenkinsci.plugins.pipeline.utility.steps.conf;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import hudson.model.Label;
import hudson.model.Result;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.jenkinsci.plugins.workflow.actions.ArgumentsAction;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graphanalysis.DepthFirstScanner;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class WriteYamlStepTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        j = rule;
        j.createOnlineSlave(Label.get("slaves"));
    }

    @Test
    void writeInvalidMap() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        def l = []
                        node('slaves') {
                          writeYaml file: 'test', data: /['a': ]/
                          def yml = readYaml file: 'test'
                          assert yml == /['a': ]/
                        }""",
                true));
        WorkflowRun b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void writeArbitraryObject() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                                def l = []
                                node('slaves') {
                                   writeYaml file: 'test', data: new Date()
                                  def yml = readYaml file: 'test'
                                  assert yml =~ /2\\d{3}/
                                }""",
                true));
        WorkflowRun b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void writeMapObject() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "map");
        p.setDefinition(new CpsFlowDefinition(
                """
                                node('slaves') {
                                  writeYaml file: 'test', data: ['a': 1, 'b': 2]
                                  def yml = readYaml file: 'test'
                                  assert yml == ['a' : 1, 'b': 2]
                                }""",
                true));
        WorkflowRun b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void writeListObjectAndRead() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "list");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          writeYaml file: 'test', data: ['a', 'b', 'c']
                          def yml = readYaml file: 'test'
                          assert yml == ['a','b','c']
                        }""",
                true));
        WorkflowRun b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void writeListOfDocumentsAndRead() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "list");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          writeYaml file: 'test', datas: [['a': 1, 'b': 2], ['a': 3, 'b': 4]]
                          def yml = readYaml file: 'test'
                          assert yml == [['a': 1, 'b': 2], ['a': 3, 'b': 4]]
                        }""",
                true));
        WorkflowRun b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void writeExistingFile() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "list");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          touch 'test.yml'
                          writeYaml file: 'test.yml', data: ['a', 'b', 'c']
                        }""",
                true));
        WorkflowRun b = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains("FileAlreadyExistsException", b);
    }

    @Test
    void overwriteExistingFile() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "list");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          writeFile file: 'test.yml', text: 'overwrite me'
                          writeYaml file: 'test.yml', overwrite: true, data: 'overwritten'
                          final text = readFile file: 'test.yml'
                          if (text != 'overwritten\\n') error('got ' + text)
                        }""",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void writeGStringWithoutApproval() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "gstringjobnoapproval");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          writeYaml file: 'test', data: "${currentBuild.rawBuild}"
                          def yml = readYaml file: 'test'
                          assert yml == 'gstringjob#1'
                        }""",
                true));
        WorkflowRun b = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains(
                "Scripts not permitted to use method org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper getRawBuild",
                b);
    }

    @Test
    void writeGString() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "gstringjob");
        final ScriptApproval approval = ScriptApproval.get();
        approval.approveSignature("method org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper getRawBuild");
        approval.approveSignature("method hudson.model.Run getExternalizableId");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          writeYaml file: 'test', data: "${currentBuild.rawBuild.externalizableId}"
                          def yml = readYaml file: 'test'
                          echo yml
                          assert yml == 'gstringjob#1'
                        }""",
                true));
        WorkflowRun b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void writeNoData() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                node('slaves') {
                  writeYaml file: 'some'
                }""",
                true));
        WorkflowRun run =
                j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains("data or datas parameter must be provided to writeYaml", run);
    }

    @Test
    void writeDataAndDatas() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                         node('slaves') {
                           writeYaml file: 'test', data: ['a': 1, 'b': 2], datas: [['a': 1], ['b': 2]]
                         }""",
                true));
        WorkflowRun run =
                j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains("only one of data or datas must be provided to writeYaml", run);
    }

    @Test
    void writeNoFile() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                node('slaves') {
                  writeYaml data: 'some', file: ''
                }""",
                true));
        WorkflowRun run =
                j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains("either file or returnText must be provided to writeYaml", run);
    }

    @Test
    void invalidDataObject() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "list");
        ScriptApproval.get().approveSignature("staticMethod java.util.TimeZone getDefault");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          def tz = TimeZone.getDefault()
                          echo "${tz}"
                          writeYaml file: 'test', data: TimeZone.getDefault()
                        }""",
                true));
        WorkflowRun b = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains("data parameter has invalid content (no-basic classes)", b);
    }

    @Test
    void validComplexDataObject() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "list");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          def data = [['a': 1, 'b' : '2', true: false], true, null, 'some string value']
                          echo(/${data.toString().toUpperCase()}/)
                          writeYaml file: 'test', data: data
                        }""",
                true));
        WorkflowRun b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        for (FlowNode n : new DepthFirstScanner().allNodes(b.getExecution())) {
            String args = ArgumentsAction.getStepArgumentsAsString(n);
            assertThat(
                    "saved full YAML `" + args + "` in " + n + " ~ " + n.getDisplayName(),
                    args,
                    not(containsString("some string value")));
            // Note that ArgumentsAction.getArguments(n) shows that the FlowNode stores the original object.
        }
    }

    @Test
    void invalidComplexDataObject() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "list");
        ScriptApproval.get().approveSignature("staticMethod java.util.TimeZone getDefault");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node('slaves') {
                          def data = [true, null, ['a': 1, 'b' : '2', true: false, 'tz': TimeZone.getDefault()], 'str']
                          echo "${data}"
                          writeYaml file: 'test', data: data
                        }""",
                true));
        WorkflowRun b = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains("data parameter has invalid content (no-basic classes)", b);
    }

    @Test
    void returnText() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "map");
        p.setDefinition(new CpsFlowDefinition(
                """
                                node('slaves') {
                                  String written = writeYaml returnText: true, data: ['a': 1, 'b': 2]
                                  def yml = readYaml text: written
                                  assert yml == ['a' : 1, 'b': 2]
                                }""",
                true));
        WorkflowRun b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void returnTextWithoutNode() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "map");
        p.setDefinition(new CpsFlowDefinition(
                """
                                String written = writeYaml returnText: true, data: ['a': 1, 'b': 2]
                                def yml = readYaml text: written
                                assert yml == ['a' : 1, 'b': 2]
                                """,
                true));
        WorkflowRun b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }
}
