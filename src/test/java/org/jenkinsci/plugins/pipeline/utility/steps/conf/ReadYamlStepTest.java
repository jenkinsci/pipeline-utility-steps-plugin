package org.jenkinsci.plugins.pipeline.utility.steps.conf;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.actions.LogAction;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.Label;
import hudson.model.Result;

public class ReadYamlStepTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private String yamlText="boolean: true\n"
    		+ "string: 'string'\n"
    		+ "integer: 3\n"
    		+ "float: 3.14\n"
    		+ "array:\n"
    		+ " - value1\n"
			+ " - value2";
    
    private String yamlTextOverride="boolean: false\n"
    		+ "integer: 0\n";
    
    private String yamlSeveralDocuments=
    		"---\n"
    		+ "string: 'doc1'\n"
    		+ "---\n"
    		+ "string: 'doc2'\n"
    		+ "...\n"
    		+ "---\n"
    		+ "string: 'doc3'\n";
    
    @Before
    public void setup() throws Exception {
        j.createOnlineSlave(Label.get("slaves"));
    }

    @Test
    public void readDirectText() throws Exception {
		WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
		p.setDefinition(new CpsFlowDefinition("node('slaves') {\n"+
			"  def yaml = readYaml text: '''"+yamlText+"'''\n" +
			"  assert yaml.boolean == true\n" +
	        "  assert yaml.string == 'string'\n" +
	        "  assert yaml.integer == 3\n" +
	        "  assert yaml.float == 3.14\n" +
	        "  assert yaml.array.size() == 2\n" +
	        "  assert yaml.array[0] == 'value1'\n" +
	        "  assert yaml.array[1] == 'value2'\n" +
	        "  assert yaml.another == null\n" +
	        "}", true));
		j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }
    
    @Test
    public void readSeveralDocuments() throws Exception {
		WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
		p.setDefinition(new CpsFlowDefinition("node('slaves') {\n"+
			"  def yaml = readYaml text: '''"+yamlSeveralDocuments+"'''\n" +
			"  assert yaml.size() == 3\n" +
			"  assert yaml[0].string == 'doc1'\n" +
			"  assert yaml[1].string == 'doc2'\n" +
			"  assert yaml[2].string == 'doc3'\n" +
	        "}", true));
		j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }
    
    @Test
    public void readText() throws Exception {
       
    	File file = temp.newFile();
    	FileUtils.writeStringToFile(file, yamlText);

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                		"  String yamlText = readFile file: '" + file.getAbsolutePath().replace("\\","\\\\") + "'\n" +
                        "  def yaml = readYaml text: yamlText\n" +
                        "  assert yaml.boolean == true\n" +
                        "  assert yaml.string == 'string'\n" +
                        "  assert yaml.integer == 3\n" +
                        "  assert yaml.float == 3.14\n" +
                        "  assert yaml.array.size() == 2\n" +
                        "  assert yaml.array[0] == 'value1'\n" +
                        "  assert yaml.array[1] == 'value2'\n" +
                        "  assert yaml.another == null\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }
    
    @Test
    public void readFile() throws Exception {
       
    	File file = temp.newFile();
    	FileUtils.writeStringToFile(file, yamlText);

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  def yaml = readYaml file: '" + file.getAbsolutePath().replace("\\","\\\\") + "'\n" +
                        "  assert yaml.boolean == true\n" +
                        "  assert yaml.string == 'string'\n" +
                        "  assert yaml.integer == 3\n" +
                        "  assert yaml.float == 3.14\n" +
                        "  assert yaml.array.size() == 2\n" +
                        "  assert yaml.array[0] == 'value1'\n" +
                        "  assert yaml.array[1] == 'value2'\n" +
                        "  assert yaml.another == null\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }
    
    @Test
    public void readFileAndText() throws Exception {
       
    	File file = temp.newFile();
    	FileUtils.writeStringToFile(file, yamlText);

    	File fileOverride = temp.newFile();
    	FileUtils.writeStringToFile(fileOverride, yamlTextOverride);

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                		"  String yamlText = readFile file: '" + fileOverride.getAbsolutePath().replace("\\","\\\\") + "'\n" +
                        "  def yaml = readYaml text: yamlText, file: '" + file.getAbsolutePath().replace("\\","\\\\") + "'\n" +
                        "  assert yaml.boolean == false\n" +
                        "  assert yaml.string == 'string'\n" +
                        "  assert yaml.integer == 0\n" +
                        "  assert yaml.float == 3.14\n" +
                        "  assert yaml.array.size() == 2\n" +
                        "  assert yaml.array[0] == 'value1'\n" +
                        "  assert yaml.array[1] == 'value2'\n" +
                        "  assert yaml.another == null\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }
    
    @Test
    public void readNone() throws Exception {
    	WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
    	p.setDefinition(new CpsFlowDefinition("node('slaves') {\n" + "  def props = readYaml()\n" + "}", true));
    	WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
    	j.assertLogContains("At least one of file or text needs to be provided to readYaml.", run);
    }
}
