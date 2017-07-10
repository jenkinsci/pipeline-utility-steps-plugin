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

import java.io.File;
import java.io.FileWriter;
import java.util.Properties;

import static org.jenkinsci.plugins.pipeline.utility.steps.FilenameTestsUtils.separatorsToSystemEscaped;

public class PropertiesWrapperStepTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Before
    public void setup() throws Exception {
        j.createOnlineSlave(Label.get("slaves"));
    }

    @Test
    public void readFile() throws Exception {
        Properties props = new Properties();
        props.setProperty("test", "One");
        props.setProperty("another", "Two");
        File file = temp.newFile();
        try (FileWriter f = new FileWriter(file)) {
            props.store(f, "Pipeline test");
        }

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  propertiesWrapper(file: '" + separatorsToSystemEscaped(file.getAbsolutePath()) + "') {\n" +
                        "    assert test == 'One'\n" +
                        "    assert another == 'Two'\n" +
                        "    assert env.test == 'One'\n" +
                        "    assert env.another == 'Two'\n" +
                        "  }\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void readFileWithDefaults() throws Exception {
        Properties props = new Properties();
        props.setProperty("test", "One");
        props.setProperty("another", "Two");
        File file = temp.newFile();
        try (FileWriter f = new FileWriter(file)) {
            props.store(f, "Pipeline test");
        }

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  def d = [test: 'Default', something: 'Default']\n" +
                        "  propertiesWrapper(defaults: d, file: '" + separatorsToSystemEscaped(file.getAbsolutePath()) + "') {\n" +
                        "    assert test == 'One'\n" +
                        "    assert another == 'Two'\n" +
                        "    assert env.test == 'One'\n" +
                        "    assert env.another == 'Two'\n" +
                        "    assert something == 'Default'\n" +
                        "    assert env.something == 'Default'\n" +
                        "  }\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void readText() throws Exception {
        Properties props = new Properties();
        props.setProperty("test", "One");
        props.setProperty("another", "Two");
        File file = temp.newFile();
        try (FileWriter f = new FileWriter(file)) {
            props.store(f, "Pipeline test");
        }

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  String propsText = readFile file: '" + separatorsToSystemEscaped(file.getAbsolutePath()) + "'\n" +
                        "  propertiesWrapper(text: propsText) {\n" +
                        "    assert test == 'One'\n" +
                        "    assert another == 'Two'\n" +
                        "    assert env.test == 'One'\n" +
                        "    assert env.another == 'Two'\n" +
                        "  }\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void readDirectText() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  propertiesWrapper(text: 'test=something') {\n" +
                        "    assert test == 'something'\n" +
                        "    assert env.test == 'something'\n" +
                        "    assert env.another == null\n" +
                        "  }\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void readNone() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  propertiesWrapper() {}\n" +
                        "}", true));
        WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains("At least one of file or text needs to be provided to propertiesWrapper.", run);
    }

    @Test
    public void readFileAndText() throws Exception {
        Properties props = new Properties();
        props.setProperty("test", "One");
        props.setProperty("another", "Two");
        File file = temp.newFile();
        try (FileWriter f = new FileWriter(file)) {
            props.store(f, "Pipeline test");
        }

        props = new Properties();
        props.setProperty("text", "TextOne");
        props.setProperty("another", "TextTwo");
        File textFile = temp.newFile();
        try (FileWriter f = new FileWriter(textFile)) {
            props.store(f, "Pipeline test");
        }

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  String propsText = readFile file: '" + separatorsToSystemEscaped(textFile.getAbsolutePath()) + "'\n" +
                        "  propertiesWrapper(text: propsText, file: '" + separatorsToSystemEscaped(file.getAbsolutePath()) + "') {\n" +
                        "    assert test == 'One'\n" +
                        "    assert env.test == 'One'\n" +
                        "    assert text == 'TextOne'\n" +
                        "    assert env.text == 'TextOne'\n" +
                        "    assert another == 'TextTwo'\n" +
                        "    assert env.another == 'TextTwo'\n" +
                        "  }\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }
}
