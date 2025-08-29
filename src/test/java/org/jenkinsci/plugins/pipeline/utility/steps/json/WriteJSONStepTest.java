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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

import hudson.model.Result;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.jenkinsci.plugins.pipeline.utility.steps.FilenameTestsUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Tests for {@link WriteJSONStep}.
 *
 * @author Nikolas Falco
 */
@WithJenkins
class WriteJSONStepTest {

    private JenkinsRule j;

    @TempDir
    private File temp;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void writeFilePretty() throws Exception {
        File output = File.createTempFile("junit", null, temp);

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" + " def json = readJSON text: '{\"a\": {\"1\": true,\"2\": 2}}' \n"
                        + " writeJSON file: '"
                        + FilenameTestsUtils.toPath(output) + "', json: json, pretty: 4\n" + "}",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));

        String lines = Files.readString(Paths.get(output.toURI()));
        assertThat(lines.split("\r\n|\r|\n").length, equalTo(4));
    }

    @Test
    void writeFilePrettyEmpty() throws Exception {
        File output = File.createTempFile("junit", null, temp);

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" + " def json = readJSON text: '{}' \n"
                        + " writeJSON file: '"
                        + FilenameTestsUtils.toPath(output) + "', json: json, pretty: 4\n" + "}",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));

        String lines = Files.readString(Paths.get(output.toURI()));
        assertThat(lines.split("\r\n|\r|\n").length, equalTo(1));
    }

    @Test
    void writeFile() throws Exception {
        int elements = 3;
        String input = getJSON(elements);
        File output = File.createTempFile("junit", null, temp);

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" + "  def json = readJSON text: '"
                        + input + "'\n" + "  json[0] = null\n"
                        + "  json["
                        + elements + "] = 45\n" + "  writeJSON file: '"
                        + FilenameTestsUtils.toPath(output) + "', json: json\n" + "}",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));

        // file exists by default so we check that should not be empty
        assertThat(output.length(), greaterThan(0L));

        String jsonResult = new String(Files.readAllBytes(Paths.get(output.toURI())));
        JSON json = JSONSerializer.toJSON(jsonResult);
        assertThat(json, instanceOf(JSONArray.class));

        JSONArray jsonArray = (JSONArray) json;
        assertFalse(jsonArray.isEmpty(), "Saved json is an empty array");
        assertThat(jsonArray.size(), is(elements + 1));
        assertNotNull(jsonArray.get(0), "Unexpected element value");
        assertEquals(45, jsonArray.get(elements), "Unexpected element value");
    }

    @Test
    void returnText() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node {
                          String written = writeJSON returnText: true, json: ['a': 1, 'b': 2]
                          def json = readJSON text: written, returnPojo: true
                          assert json == ['a': 1, 'b': 2]
                        }""",
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void returnTextWithoutNode() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        String written = writeJSON returnText: true, json: ['a': 1, 'b': 2]
                        def json = readJSON text: written, returnPojo: true
                        assert json == ['a': 1, 'b': 2]
                        """,
                true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
    }

    @Test
    void checkRequiredFileOrReturnText() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                """
                        node {
                          def json = readJSON text: '{}'
                          writeJSON json: json\
                        }""",
                true));
        WorkflowRun run =
                j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains(Messages.WriteJSONStepExecution_missingReturnTextAndFile("writeJSON"), run);
    }

    @Test
    void checkRequiredJSON() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("node {\n" + "  writeJSON file: 'output.json'" + "}", true));
        WorkflowRun run =
                j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains(Messages.WriteJSONStepExecution_missingJSON("writeJSON"), run);
    }

    @Test
    void checkCannotReturnTextAndFile() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" + "  writeJSON returnText:true, file: 'output.json', json: [foo: 'bar']" + "}", true));
        WorkflowRun run =
                j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains(Messages.WriteJSONStepExecution_bothReturnTextAndFile("writeJSON"), run);
    }

    private String getJSON(int elements) {
        JSONArray root = new JSONArray();
        for (int i = 0; i < elements; i++) {
            JSONObject jsonElement = new JSONObject();
            jsonElement.put("index", i);
            root.add(jsonElement);
        }
        return root.toString();
    }
}
