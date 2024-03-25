/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 CloudBees Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.jenkinsci.plugins.pipeline.utility.steps.conf;

import hudson.model.Label;
import hudson.model.Result;

import static org.jenkinsci.plugins.pipeline.utility.steps.FilenameTestsUtils.separatorsToSystemEscaped;
import static org.jenkinsci.plugins.pipeline.utility.steps.conf.ReadPropertiesStepExecution.CUSTOM_PREFIX_INTERPOLATOR_LOOKUPS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.jenkinsci.plugins.pipeline.utility.steps.Messages;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.FlagRule;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.Properties;

/**
 * Tests for {@link ReadPropertiesStep}.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class ReadPropertiesStepTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();
    @Rule
    public FlagRule<String> customLookups = new FlagRule<>(() -> CUSTOM_PREFIX_INTERPOLATOR_LOOKUPS, x -> CUSTOM_PREFIX_INTERPOLATOR_LOOKUPS = x);

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
                        "  def props = readProperties file: '" + separatorsToSystemEscaped(file.getAbsolutePath()) + "'\n" +
                        "  assert props['test'] == 'One'\n" +
                        "  assert props['another'] == 'Two'\n" +
                        "  assert props.test == 'One'\n" +
                        "  assert props.another == 'Two'\n" +
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
                        "  def props = readProperties defaults: d, file: '" + separatorsToSystemEscaped(file.getAbsolutePath()) + "'\n" +
                        "  assert props['test'] == 'One'\n" +
                        "  assert props['another'] == 'Two'\n" +
                        "  assert props.test == 'One'\n" +
                        "  assert props.another == 'Two'\n" +
                        "  assert props['something'] == 'Default'\n" +
                        "  assert props.something == 'Default'\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void readFileWithCharset() throws Exception {
        Properties props = new Properties();
        props.setProperty("test", "One");
        props.setProperty("another", "Two");
        props.setProperty("zh", "中文");
        File file = temp.newFile();
        try (FileWriter f = new FileWriter(file)) {
            props.store(f, "Pipeline test");
        }

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  def props = readProperties charset: 'utf-8', file: '" + separatorsToSystemEscaped(file.getAbsolutePath()) + "'\n" +
                        "  assert props['test'] == 'One'\n" +
                        "  assert props['another'] == 'Two'\n" +
                        "  assert props.test == 'One'\n" +
                        "  assert props.another == 'Two'\n" +
                        "  assert props.zh == '中文' \n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void readText() throws Exception {
        Properties props = new Properties();
        props.setProperty("test", "One");
        props.setProperty("another", "Two");
        StringWriter propsString = new StringWriter();
        props.store(propsString, "Pipeline test");

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                        "def props = readProperties text: '''" + propsString + "'''\n" +
                        "assert props['test'] == 'One'\n" +
                        "assert props['another'] == 'Two'\n" +
                        "assert props.test == 'One'\n" +
                        "assert props.another == 'Two'\n", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void readDirectText() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  def props = readProperties text: 'test=something'\n" +
                        "  assert props['test'] == 'something'\n" +
                        "  assert props.test == 'something'\n" +
                        "  assert props.another == null\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void readNone() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  def props = readProperties()\n" +
                        "}", true));
        WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains(Messages.AbstractFileOrTextStepDescriptorImpl_missingRequiredArgument("readProperties"), run);
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
                        "  def props = readProperties text: propsText, file: '" + separatorsToSystemEscaped(file.getAbsolutePath()) + "'\n" +
                        "  assert props['test'] == 'One'\n" +
                        "  assert props.test == 'One'\n" +
                        "  assert props['text'] == 'TextOne'\n" +
                        "  assert props.text == 'TextOne'\n" +
                        "  assert props['another'] == 'TextTwo'\n" +
                        "  assert props.another == 'TextTwo'\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void readFileAndTextInterpolatedDisabled() throws Exception {
        Properties props = new Properties();
        props.setProperty("test", "One");
        props.setProperty("another", "Two");
        File file = temp.newFile();
        try (FileWriter f = new FileWriter(file)) {
            props.store(f, "Pipeline test");
        }

        props = new Properties();
        props.setProperty("url", "http://localhost");
        props.setProperty("file", "book.txt");
        props.setProperty("fileUrl", "${url}/${file}");
        File textFile = temp.newFile();
        try (FileWriter f = new FileWriter(textFile)) {
            props.store(f, "Pipeline test");
        }

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  String propsText = readFile file: '" + separatorsToSystemEscaped(textFile.getAbsolutePath()) + "'\n" +
                        "  def props = readProperties text: propsText, file: '" + separatorsToSystemEscaped(file.getAbsolutePath()) + "'\n" +
                        "  assert props['test'] == 'One'\n" +
                        "  assert props.test == 'One'\n" +
                        "  assert props['url'] == 'http://localhost'\n" +
                        "  assert props.url == 'http://localhost'\n" +
                        "  assert props['file'] == 'book.txt'\n" +
                        "  assert props.file == 'book.txt'\n" +
                        "  assert props['fileUrl'] == '${url}/${file}'\n" +
                        "  assert props.fileUrl == '${url}/${file}'\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));

        p = j.jenkins.createProject(WorkflowJob.class, "p2");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  String propsText = readFile file: '" + separatorsToSystemEscaped(textFile.getAbsolutePath()) + "'\n" +
                        "  def props = readProperties interpolate: false, text: propsText, file: '" + separatorsToSystemEscaped(file.getAbsolutePath()) + "'\n" +
                        "  assert props['test'] == 'One'\n" +
                        "  assert props.test == 'One'\n" +
                        "  assert props['url'] == 'http://localhost'\n" +
                        "  assert props.url == 'http://localhost'\n" +
                        "  assert props['file'] == 'book.txt'\n" +
                        "  assert props.file == 'book.txt'\n" +
                        "  assert props['fileUrl'] == '${url}/${file}'\n" +
                        "  assert props.fileUrl == '${url}/${file}'\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void readFileAndTextInterpolated() throws Exception {
        Properties props = new Properties();
        props.setProperty("test", "One");
        props.setProperty("another", "Two");
        File file = temp.newFile();
        try (FileWriter f = new FileWriter(file)) {
            props.store(f, "Pipeline test");
        }

        props = new Properties();
        props.setProperty("url", "http://localhost");
        props.setProperty("file", "book.txt");
        props.setProperty("fileUrl", "${url}/${file}");
        File textFile = temp.newFile();
        try (FileWriter f = new FileWriter(textFile)) {
            props.store(f, "Pipeline test");
        }

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  String propsText = readFile file: '" + separatorsToSystemEscaped(textFile.getAbsolutePath()) + "'\n" +
                        "  def props = readProperties interpolate: true, text: propsText, file: '" + separatorsToSystemEscaped(file.getAbsolutePath()) + "'\n" +
                        "  assert props['test'] == 'One'\n" +
                        "  assert props.test == 'One'\n" +
                        "  assert props['url'] == 'http://localhost'\n" +
                        "  assert props.url == 'http://localhost'\n" +
                        "  assert props['file'] == 'book.txt'\n" +
                        "  assert props.file == 'book.txt'\n" +
                        "  assert props['fileUrl'] == 'http://localhost/book.txt'\n" +
                        "  assert props.fileUrl == 'http://localhost/book.txt'\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }
    
    @Test
    public void readFileAndTextAndDefaultsInterpolated() throws Exception {
        Properties props = new Properties();
        props.setProperty("test", "One");
        props.setProperty("another", "Two");
        File file = temp.newFile();
        try (FileWriter f = new FileWriter(file)) {
            props.store(f, "Pipeline test");
        }

        props = new Properties();
        props.setProperty("file", "book.txt");
        props.setProperty("fileUrl", "${url}/${file}");
        File textFile = temp.newFile();
        try (FileWriter f = new FileWriter(textFile)) {
            props.store(f, "Pipeline test");
        }

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  String propsText = readFile file: '" + separatorsToSystemEscaped(textFile.getAbsolutePath()) + "'\n" +
                		"  def defaults = [\"url\": \"http://localhost\"]\n" + 
                        "  def props = readProperties interpolate: true, defaults: defaults, text: propsText, file: '" + separatorsToSystemEscaped(file.getAbsolutePath()) + "'\n" +
                        "  assert props['test'] == 'One'\n" +
                        "  assert props.test == 'One'\n" +
                        "  assert props['url'] == 'http://localhost'\n" +
                        "  assert props.url == 'http://localhost'\n" +
                        "  assert props['file'] == 'book.txt'\n" +
                        "  assert props.file == 'book.txt'\n" +
                        "  assert props['fileUrl'] == 'http://localhost/book.txt'\n" +
                        "  assert props.fileUrl == 'http://localhost/book.txt'\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void readFileAndTextInterpolatedWithCyclicValues() throws Exception {
        Properties props = new Properties();
        props.setProperty("test", "One");
        props.setProperty("another", "Two");
        File file = temp.newFile();
        try (FileWriter f = new FileWriter(file)) {
            props.store(f, "Pipeline test");
        }

        props = new Properties();
        props.setProperty("url", "${file}");
        props.setProperty("file", "${fileUrl}");
        props.setProperty("fileUrl", "${url}");
        File textFile = temp.newFile();
        try (FileWriter f = new FileWriter(textFile)) {
            props.store(f, "Pipeline test");
        }

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  String propsText = readFile file: '" + separatorsToSystemEscaped(textFile.getAbsolutePath()) + "'\n" +
                        "  def props = readProperties text: propsText, interpolate: true, file: '" + separatorsToSystemEscaped(file.getAbsolutePath()) + "'\n" +
                        "  assert props['test'] == 'One'\n" +
                        "  assert props.test == 'One'\n" +
                        "  assert props['url'] == '${file}'\n" +
                        "  assert props.url == '${file}'\n" +
                        "  assert props['file'] == '${fileUrl}'\n" +
                        "  assert props.file == '${fileUrl}'\n" +
                        "  assert props['fileUrl'] == '${url}'\n" +
                        "  assert props.fileUrl == '${url}'\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Issue("SECURITY-2949")
    @Test
    public void unsafeInterpolatorsDoNotInterpolate() throws Exception {
        Properties props = new Properties();
        props.setProperty("file", "${file:utf8:/etc/passwd}");
        props.setProperty("hax", "${script:Groovy:jenkins.model.Jenkins.get().systemMessage = 'pwn3d'}");
        File textFile = temp.newFile();
        try (FileWriter f = new FileWriter(textFile)) {
            props.store(f, "Pipeline test");
        }

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  def props = readProperties interpolate: true, file: '" + separatorsToSystemEscaped(textFile.getAbsolutePath()) + "'\n" +
                        "  assert props['file'] == '${file:utf8:/etc/passwd}'\n" +
                        "  assert props['hax'] == '${script:Groovy:jenkins.model.Jenkins.get().systemMessage = \\'pwn3d\\'}'\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        assertNotEquals("pwn3d", j.jenkins.getSystemMessage());
    }

    @Issue("SECURITY-2949")
    @Test
    public void safeInterpolatorsDoInterpolate() throws Exception {
        Properties props = new Properties();
        props.setProperty("urld", "${urlDecoder:Hello+World%21}");
        props.setProperty("urle", "${urlEncoder:Hello World!}");
        props.setProperty("base64d", "${base64Decoder:SGVsbG9Xb3JsZCE=}");
        props.setProperty("base64e", "${base64Encoder:HelloWorld!}");
        File textFile = temp.newFile();
        try (FileWriter f = new FileWriter(textFile)) {
            props.store(f, "Pipeline test");
        }
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  def props = readProperties interpolate: true, file: '" + separatorsToSystemEscaped(textFile.getAbsolutePath()) + "'\n" +
                        "  assert props['base64d'] == 'HelloWorld!'\n" +
                        "  assert props['base64e'] == 'SGVsbG9Xb3JsZCE='\n" +
                        "  assert props['urld'] == 'Hello World!'\n" +
                        "  assert props['urle'] == 'Hello+World%21'\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Issue("SECURITY-2949")
    @Test
    public void customLookupsOverrideDefaultInterpolators() throws Exception {
        CUSTOM_PREFIX_INTERPOLATOR_LOOKUPS = "script,base64_encoder";

        Properties props = new Properties();
        props.setProperty("hax", "${script:Groovy:jenkins.model.Jenkins.get().systemMessage = 'pwn3d'}");
        props.setProperty("base64d", "${base64Decoder:SGVsbG9Xb3JsZCE=}");
        props.setProperty("base64e", "${base64Encoder:HelloWorld!}");
        File textFile = temp.newFile();
        try (FileWriter f = new FileWriter(textFile)) {
            props.store(f, "Pipeline test");
        }

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node('slaves') {\n" +
                        "  def props = readProperties interpolate: true, file: '" + separatorsToSystemEscaped(textFile.getAbsolutePath()) + "'\n" +
                        "  assert props['base64d'] == '${base64Decoder:SGVsbG9Xb3JsZCE=}'\n" +
                        "  assert props['base64e'] == 'SGVsbG9Xb3JsZCE='\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        assertEquals("pwn3d", j.jenkins.getSystemMessage());
    }
}
