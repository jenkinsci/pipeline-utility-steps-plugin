package org.jenkinsci.plugins.pipeline.utility.steps.conf;

import static org.jenkinsci.plugins.pipeline.utility.steps.FilenameTestsUtils.separatorsToSystemEscaped;
import static org.junit.jupiter.api.Assertions.*;

import hudson.model.Label;
import hudson.model.Result;
import java.io.File;
import java.nio.charset.Charset;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.pipeline.utility.steps.Messages;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.yaml.snakeyaml.LoaderOptions;

@WithJenkins
class ReadYamlStepTest {

    private JenkinsRule j;

    @TempDir
    private File temp;

    private final String yamlText =
            """
            boolean: true
            string: 'string'
            integer: 3
            double: 3.14
            null: null
            date: 2001-12-14T21:59:43.10Z
            billTo:
             address:
              postal  : 48046
            array:
             - value1
             - value2""";

    private final String yamlTextOverride = """
            boolean: false
            integer: 0
            """;

    private final String yamlSeveralDocuments =
            """
                    ---
                    string: 'doc1'
                    ---
                    string: 'doc2'
                    ...
                    ---
                    string: 'doc3'
                    """;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        j = rule;
        System.setProperty(
                "org.jenkinsci.plugins.pipeline.utility.steps.conf.ReadYamlStep.MAX_CODE_POINT_LIMIT", "10485760");
        System.setProperty(
                "org.jenkinsci.plugins.pipeline.utility.steps.conf.ReadYamlStep.MAX_MAX_ALIASES_FOR_COLLECTIONS",
                "500");
        j.createOnlineSlave(Label.get("slaves"));
    }

    @Test
    void checksPrimitivesAndDates() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" + "  def yaml = readYaml text: '''" + yamlText + "'''\n"
                        + "  assert yaml.getClass().getName() == 'java.util.LinkedHashMap'\n"
                        + "  assert yaml.boolean.getClass().getName() == 'java.lang.Boolean'\n"
                        + "  assert yaml.string.getClass().getName() == 'java.lang.String'\n"
                        + "  assert yaml.integer.getClass().getName() == 'java.lang.Integer'\n"
                        + "  assert yaml.double.getClass().getName() == 'java.lang.Double'\n"
                        + "  def timeZone = TimeZone.getTimeZone('UTC')\n"
                        + "  assert yaml.date.format('yyyy-MM-dd HH:mm:ss',timeZone) == '2001-12-14 21:59:43'\n"
                        + "  assert yaml.date.getClass().getName() == 'java.util.Date'\n"
                        + "  assert yaml.billTo.getClass().getName() == 'java.util.LinkedHashMap'\n"
                        + "  assert yaml.billTo.address.getClass().getName() == 'java.util.LinkedHashMap'\n"
                        + "  assert yaml.billTo.address.postal.getClass().getName() == 'java.lang.Integer'\n"
                        + "  assert yaml.array.getClass().getName() == 'java.util.ArrayList'\n"
                        + "  assert yaml.array[0].getClass().getName() == 'java.lang.String'\n"
                        + "  assert yaml.array[1].getClass().getName() == 'java.lang.String'\n"
                        + "}",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void readDirectText() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" + "  def yaml = readYaml text: '''" + yamlText + "'''\n"
                        + "  assert yaml.boolean == true\n" + "  assert yaml.string == 'string'\n"
                        + "  assert yaml.integer == 3\n"
                        + "  assert yaml.double == 3.14\n"
                        + "  assert yaml.null == null\n"
                        + "  assert yaml.billTo.address.postal == 48046\n"
                        + "  assert yaml.array.size() == 2\n"
                        + "  assert yaml.array[0] == 'value1'\n"
                        + "  assert yaml.array[1] == 'value2'\n"
                        + "  assert yaml.another == null\n"
                        + "}",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void readSeveralDocuments() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" + "  def yaml = readYaml text: '''"
                        + yamlSeveralDocuments + "'''\n" + "  assert yaml.size() == 3\n"
                        + "  assert yaml[0].string == 'doc1'\n"
                        + "  assert yaml[1].string == 'doc2'\n"
                        + "  assert yaml[2].string == 'doc3'\n"
                        + "}",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void readText() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "def yaml = readYaml text: '''" + yamlText + "'''\n" + "assert yaml.boolean == true\n"
                        + "assert yaml.string == 'string'\n"
                        + "assert yaml.integer == 3\n"
                        + "assert yaml.double == 3.14\n"
                        + "assert yaml.null == null\n"
                        + "assert yaml.billTo.address.postal == 48046\n"
                        + "assert yaml.array.size() == 2\n"
                        + "assert yaml.array[0] == 'value1'\n"
                        + "assert yaml.array[1] == 'value2'\n"
                        + "assert yaml.another == null\n",
                true));
        WorkflowRun run = p.scheduleBuild2(0).get();
        System.out.println(JenkinsRule.getLog(run));
        j.assertBuildStatusSuccess(run);
    }

    @Test
    void readFile() throws Exception {

        File file = File.createTempFile("junit", null, temp);
        FileUtils.writeStringToFile(file, yamlText, Charset.defaultCharset());

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" + "  def yaml = readYaml file: '"
                        + separatorsToSystemEscaped(file.getAbsolutePath()) + "'\n" + "  assert yaml.boolean == true\n"
                        + "  assert yaml.string == 'string'\n"
                        + "  assert yaml.integer == 3\n"
                        + "  assert yaml.double == 3.14\n"
                        + "  assert yaml.null == null\n"
                        + "  assert yaml.billTo.address.postal == 48046\n"
                        + "  assert yaml.array.size() == 2\n"
                        + "  assert yaml.array[0] == 'value1'\n"
                        + "  assert yaml.array[1] == 'value2'\n"
                        + "  assert yaml.another == null\n"
                        + "}",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void readFileAndText() throws Exception {
        File file = File.createTempFile("junit", null, temp);
        FileUtils.writeStringToFile(file, yamlText, Charset.defaultCharset());

        File fileOverride = File.createTempFile("junit", null, temp);
        FileUtils.writeStringToFile(fileOverride, yamlTextOverride, Charset.defaultCharset());

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" + "  String yamlText = readFile file: '"
                        + separatorsToSystemEscaped(fileOverride.getAbsolutePath()) + "'\n"
                        + "  def yaml = readYaml text: yamlText, file: '"
                        + separatorsToSystemEscaped(file.getAbsolutePath()) + "'\n" + "  assert yaml.boolean == false\n"
                        + "  assert yaml.string == 'string'\n"
                        + "  assert yaml.integer == 0\n"
                        + "  assert yaml.double == 3.14\n"
                        + "  assert yaml.null == null\n"
                        + "  assert yaml.billTo.address.postal == 48046\n"
                        + "  assert yaml.array.size() == 2\n"
                        + "  assert yaml.array[0] == 'value1'\n"
                        + "  assert yaml.array[1] == 'value2'\n"
                        + "  assert yaml.another == null\n"
                        + "}",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void codePointLimits() throws Exception {
        StringBuilder str = new StringBuilder("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        int desired = new LoaderOptions().getCodePointLimit();
        while (str.length() < desired) {
            str.append(str);
        }
        final String yaml = "a: " + str;
        File file = File.createTempFile("junit", null, temp);
        FileUtils.writeStringToFile(file, yaml, Charset.defaultCharset());
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('" + j.jenkins.getSelfLabel() + "') { def yaml = readYaml file: '"
                        + separatorsToSystemEscaped(file.getAbsolutePath()) + "'}",
                true));
        j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0));
        p.setDefinition(new CpsFlowDefinition(
                "node('" + j.jenkins.getSelfLabel() + "') { def yaml = readYaml codePointLimit: 10485760, file: '"
                        + separatorsToSystemEscaped(file.getAbsolutePath()) + "'}",
                true));
        j.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0));
    }

    @Test
    void setDefaultCodePointLimitHigherThanMaxFailsWithException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ReadYamlStep readYamlStep = new ReadYamlStep();
            readYamlStep.setDefaultCodePointLimit(ReadYamlStep.getMaxCodePointLimit() + 1);
        });
        String expectedMessage =
                "Reduce the required DEFAULT_CODE_POINT_LIMIT or convince your administrator to increase";
        String actualMessage = exception.getMessage();
        assertTrue(
                actualMessage.contains(expectedMessage),
                actualMessage + " <<<< DOES NOT CONTAIN >>>> " + expectedMessage);
    }

    @Test
    void millionLaughs() throws Exception {
        final String lol =
                """
                a: &a ["lol","lol","lol","lol","lol","lol","lol","lol","lol"]
                b: &b [*a,*a,*a,*a,*a,*a,*a,*a,*a]
                c: &c [*b,*b,*b,*b,*b,*b,*b,*b,*b]
                d: &d [*c,*c,*c,*c,*c,*c,*c,*c,*c]
                e: &e [*d,*d,*d,*d,*d,*d,*d,*d,*d]
                f: &f [*e,*e,*e,*e,*e,*e,*e,*e,*e]
                g: &g [*f,*f,*f,*f,*f,*f,*f,*f,*f]
                """ /* + //Not the full billion
                                                                                                                                                                                                                                                                                                                                                                                                                                        "h: &h [*g,*g,*g,*g,*g,*g,*g,*g,*g]\n" +
                                                                                                                                                                                                                                                                                                                                                                                                                                        "i: &i [*h,*h,*h,*h,*h,*h,*h,*h,*h]"*/;
        File file = File.createTempFile("junit", null, temp);
        FileUtils.writeStringToFile(file, lol, Charset.defaultCharset());
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('" + j.jenkins.getSelfLabel() + "') { def yaml = readYaml file: '"
                        + separatorsToSystemEscaped(file.getAbsolutePath()) + "'}",
                true));
        j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0));
        p.setDefinition(new CpsFlowDefinition(
                "node('" + j.jenkins.getSelfLabel() + "') { def yaml = readYaml maxAliasesForCollections: 500, file: '"
                        + separatorsToSystemEscaped(file.getAbsolutePath()) + "'}",
                true));
        j.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0));
    }

    @Test
    void setMaxMaxFallingBackToUpperLimit() {
        ReadYamlStep.setMaxMaxAliasesForCollections(ReadYamlStep.HARDCODED_CEILING_MAX_ALIASES_FOR_COLLECTIONS + 1);
        assertEquals(
                ReadYamlStep.HARDCODED_CEILING_MAX_ALIASES_FOR_COLLECTIONS,
                ReadYamlStep.getMaxMaxAliasesForCollections());
    }

    @Test
    void setDefaultHigherThanMaxFailsWithException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ReadYamlStep readYamlStep = new ReadYamlStep();
            readYamlStep.setDefaultMaxAliasesForCollections(ReadYamlStep.getMaxMaxAliasesForCollections() + 1);
        });
        String expectedMessage =
                "Reduce the required DEFAULT_MAX_ALIASES_FOR_COLLECTIONS or convince your administrator to increase";
        String actualMessage = exception.getMessage();
        assertTrue(
                actualMessage.contains(expectedMessage),
                actualMessage + " <<<< DOES NOT CONTAIN >>>> " + expectedMessage);
    }

    @Test
    void setDefaultHigherThanHardcodedMaxFailsWithException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ReadYamlStep readYamlStep = new ReadYamlStep();
            readYamlStep.setDefaultMaxAliasesForCollections(
                    ReadYamlStep.HARDCODED_CEILING_MAX_ALIASES_FOR_COLLECTIONS + 1);
        });
        String expectedMessage = "Hardcoded upper limit breached";
        String actualMessage = exception.getMessage();
        assertTrue(
                actualMessage.contains(expectedMessage),
                actualMessage + " <<<< DOES NOT CONTAIN >>>> " + expectedMessage);
    }

    @Test
    void readNone() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                node('slaves') {
                  def props = readYaml()
                }""",
                true));
        WorkflowRun run =
                j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains(Messages.AbstractFileOrTextStepDescriptorImpl_missingRequiredArgument("readYaml"), run);
    }
}
