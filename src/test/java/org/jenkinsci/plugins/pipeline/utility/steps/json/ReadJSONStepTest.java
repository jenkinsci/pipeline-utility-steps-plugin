/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Nikolas Falco
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

package org.jenkinsci.plugins.pipeline.utility.steps.json;

import static org.jenkinsci.plugins.pipeline.utility.steps.Messages.AbstractFileOrTextStepDescriptorImpl_missingRequiredArgument;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.pipeline.utility.steps.FilenameTestsUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.Result;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Tests for {@link ReadJSONStep}.
 *
 * @author Nikolas Falco
 */
public class ReadJSONStepTest {
    private static final String SOME_JSON = "{\"aNullValue\": null,\"tags\": [0, 1, 2, null]}";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void readFile() throws Exception {
        String file = writeJSON(getJSON());

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "  def json = readJSON file: '" + file + "'\n" +
                        "  assert json.isArray() == false\n" +
                        "  assert json.tags.isArray() == true\n" +
                        "  assert json.tags.size() == 3\n" +
                        "  assert json.tags[0] == 0\n" +
                        "  assert json.tags[1] == 1\n" +
                        "  assert json.tags[2] == 2\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void readText() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "def json = readJSON text: '" + getJSON() + "'\n" +
                        "assert json.tags[0] == 0\n" +
                        "assert json.tags[1] == 1\n" +
                        "assert json.tags[2] == 2\n", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void readUrl() throws Exception {
        String url = "https://reqres.in/api/users/2";

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "  def json = readJSON url: '" + url + "'\n" +
                        "  assert json.data.id == 2\n" +
                        "  assert json.data.last_name == 'Weaver'\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void readDirectText() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "def json = readJSON text: '[ { \"key\": 0 }, { \"key\": \"value\" }, { \"key\": true } ]'\n" +
                        "assert json.isArray() == true\n" +
                        "assert json.size() == 3\n" +
                        "assert json[0].key == 0\n" +
                        "assert json[1].key == 'value'\n" +
                        "assert json[2].key == true\n", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void readNone() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "  def json = readJSON()\n" +
                        "}", true));
        WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains(AbstractFileOrTextStepDescriptorImpl_missingRequiredArgument("readJSON"), run);
    }

    @Test
    public void readFileAndText() throws Exception {
        String file = writeJSON(getJSON());

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "  def json = readJSON( text: '{ \"key\": \"value\" }', file: '" + file + "' )\n" +
                        "}", true));
        WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains(Messages.ReadJSONStepExecution_tooManyArguments("readJSON"), run);
    }

    private String getJSON() throws IOException {
        JSONArray tags = new JSONArray();
        for (int i = 0; i < 3; i++) {
            tags.add(i);
        }
        JSONObject root = new JSONObject();
        root.put("tags", tags);
        return root.toString();
    }

    private String writeJSON(String json) throws IOException {
        File file = temp.newFile();
        try (Writer f = new FileWriter(file); Reader r = new StringReader(json)) {
            IOUtils.copy(r, f);
        }
        return FilenameTestsUtils.toPath(file);
    }


    @Test
    public void readFileAsPojo() throws Exception {
        String file = writeJSON(SOME_JSON);

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "  def json = readJSON file: '" + file + "', returnPojo: true\n" +
                        "  assert json.subMap([]).isEmpty()\n" +
                        "  assert json.aNullValue == null\n" +
                        "  assert json.tags.size() == 4\n" +
                        "  assert json.tags.unique().size() == 4\n" +
                        "  assert json.tags[0] == 0\n" +
                        "  assert json.tags[1] == 1\n" +
                        "  assert json.tags[2] == 2\n" +
                        "  assert json.tags[3] == null\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void readTextAsPojo() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "def json = readJSON text: '" + SOME_JSON + "', returnPojo: true\n" +
                        "assert json.subMap([]).isEmpty()\n" +
                        "assert json.aNullValue == null\n" +
                        "assert json.tags.size() == 4\n" +
                        "assert json.tags.unique().size() == 4\n" +
                        "assert json.tags[0] == 0\n" +
                        "assert json.tags[1] == 1\n" +
                        "assert json.tags[2] == 2\n" +
                        "assert json.tags[3] == null\n", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void readDirectTextAsPojo() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "def json = readJSON text: '[ { \"key\": 0 }, { \"key\": \"value\" }, { \"key\": true }, { \"key\": null } ]', returnPojo: true\n" +
                        "assert json.size() == 4\n" +
                        "assert json.unique().size() == 4\n" +
                        "assert json[0].key == 0\n" +
                        "assert json[1].key == 'value'\n" +
                        "assert json[2].key == true\n" +
                        "assert json[3].key == null\n", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    public void readNoneAsPojo() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "  def json = readJSON(returnPojo: true)\n" +
                        "}", true));
        WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains(AbstractFileOrTextStepDescriptorImpl_missingRequiredArgument("readJSON"), run);
    }

    @Test
    public void readFileAndTextAsPojo() throws Exception {
        String file = writeJSON(SOME_JSON);

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "  def json = readJSON( text: '{ \"key\": \"value\" }', file: '" + file + "', returnPojo: true )\n" +
                        "}", true));
        WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains(Messages.ReadJSONStepExecution_tooManyArguments("readJSON"), run);
    }

}