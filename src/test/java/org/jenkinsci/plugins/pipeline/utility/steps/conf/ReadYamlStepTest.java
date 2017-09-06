package org.jenkinsci.plugins.pipeline.utility.steps.conf;

import java.io.File;

import org.apache.commons.io.FileUtils;
import static org.jenkinsci.plugins.pipeline.utility.steps.FilenameTestsUtils.separatorsToSystemEscaped;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
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

    private final String yamlText="boolean: true\n"
    		+ "string: 'string'\n"
    		+ "integer: 3\n"
    		+ "double: 3.14\n"
    		+ "null: null\n"
    		+ "date: 2001-12-14T21:59:43.10Z\n"
    		+ "billTo:\n"
    		+ " address:\n"
    		+ "  postal  : 48046\n"
    		+ "array:\n"
			+ " - value1\n"
			+ " - value2";
    
    private final String yamlTextOverride="boolean: false\n"
    		+ "integer: 0\n";
    
    private final String yamlSeveralDocuments=
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
    public void checksPrimitivesAndDatesWithoutSandbox() throws Exception {
    	
    	//We desactive Sandbox because Class.getName and Date.format are not permitted in Sandbox...
    	
		WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
		p.setDefinition(new CpsFlowDefinition(
				"node('slaves') {\n" + "  def yaml = readYaml text: '''" + yamlText + "'''\n" 
						+ "  assert yaml.getClass().getName() == 'java.util.LinkedHashMap'\n" +
						"  assert yaml.boolean.getClass().getName() == 'java.lang.Boolean'\n" +
						"  assert yaml.string.getClass().getName() == 'java.lang.String'\n" +
						"  assert yaml.integer.getClass().getName() == 'java.lang.Integer'\n" +
						"  assert yaml.double.getClass().getName() == 'java.lang.Double'\n" +
						"  def timeZone = TimeZone.getTimeZone('UTC')\n" +
						"  assert yaml.date.format('yyyy-MM-dd HH:mm:ss',timeZone) == '2001-12-14 21:59:43'\n" +
						"  assert yaml.date.getClass().getName() == 'java.util.Date'\n" +
						"  assert yaml.billTo.getClass().getName() == 'java.util.LinkedHashMap'\n" +
						"  assert yaml.billTo.address.getClass().getName() == 'java.util.LinkedHashMap'\n" +
						"  assert yaml.billTo.address.postal.getClass().getName() == 'java.lang.Integer'\n" +
						"  assert yaml.array.getClass().getName() == 'java.util.ArrayList'\n" +
				        "  assert yaml.array[0].getClass().getName() == 'java.lang.String'\n" +
						"  assert yaml.array[1].getClass().getName() == 'java.lang.String'\n" +
				        "}",
				false));
		j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void readDirectText() throws Exception {
		WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
		p.setDefinition(new CpsFlowDefinition(
				"node('slaves') {\n" + "  def yaml = readYaml text: '''" + yamlText + "'''\n" 
						+ "  assert yaml.boolean == true\n" +
						"  assert yaml.string == 'string'\n" +
						"  assert yaml.integer == 3\n" +
						"  assert yaml.double == 3.14\n" +
						"  assert yaml.null == null\n" +
						"  assert yaml.billTo.address.postal == 48046\n" +
						"  assert yaml.array.size() == 2\n" +
						"  assert yaml.array[0] == 'value1'\n" +
						"  assert yaml.array[1] == 'value2'\n" +
				        "  assert yaml.another == null\n" + "}",
				true));
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
		WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
		p.setDefinition(new CpsFlowDefinition(
				"def yaml = readYaml text: '''" + yamlText + "'''\n" +
						"assert yaml.boolean == true\n" +
						"assert yaml.string == 'string'\n" +
						"assert yaml.integer == 3\n" +
						"assert yaml.double == 3.14\n" +
						"assert yaml.null == null\n" +
						"assert yaml.billTo.address.postal == 48046\n" +
						"assert yaml.array.size() == 2\n" +
						"assert yaml.array[0] == 'value1'\n" +
						"assert yaml.array[1] == 'value2'\n" +
						"assert yaml.another == null\n",
				true));
		WorkflowRun run = p.scheduleBuild2(0).get();
        System.out.println(JenkinsRule.getLog(run));
        j.assertBuildStatusSuccess(run);
    }
    
    @Test
    public void readFile() throws Exception {
       
    	File file = temp.newFile();
    	FileUtils.writeStringToFile(file, yamlText);

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  def yaml = readYaml file: '" + separatorsToSystemEscaped(file.getAbsolutePath()) + "'\n" +
            			"  assert yaml.boolean == true\n" +
            			"  assert yaml.string == 'string'\n" +
            			"  assert yaml.integer == 3\n" +
            			"  assert yaml.double == 3.14\n" +
            			"  assert yaml.null == null\n" +
            			"  assert yaml.billTo.address.postal == 48046\n" +
            			"  assert yaml.array.size() == 2\n" +
            			"  assert yaml.array[0] == 'value1'\n" +
            			"  assert yaml.array[1] == 'value2'\n" +
            	        "  assert yaml.another == null\n" + "}",
            	        true));
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
                		"  String yamlText = readFile file: '" + separatorsToSystemEscaped(fileOverride.getAbsolutePath()) + "'\n" +
                        "  def yaml = readYaml text: yamlText, file: '" + separatorsToSystemEscaped(file.getAbsolutePath()) + "'\n" +
            			"  assert yaml.boolean == false\n" +
            			"  assert yaml.string == 'string'\n" +
            			"  assert yaml.integer == 0\n" +
            			"  assert yaml.double == 3.14\n" +
            			"  assert yaml.null == null\n" +
            			"  assert yaml.billTo.address.postal == 48046\n" +
            			"  assert yaml.array.size() == 2\n" +
            			"  assert yaml.array[0] == 'value1'\n" +
            			"  assert yaml.array[1] == 'value2'\n" +
            	        "  assert yaml.another == null\n" + "}",
            	        true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void readNone() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("node('slaves') {\n" + "  def props = readYaml()\n" + "}", true));
        WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains("At least one of file or text needs to be provided.", run);
    }
}
