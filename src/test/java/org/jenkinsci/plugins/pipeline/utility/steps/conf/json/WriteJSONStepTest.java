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

package org.jenkinsci.plugins.pipeline.utility.steps.conf.json;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.jenkinsci.plugins.pipeline.utility.steps.FilenameTestsUtils;
import org.jenkinsci.plugins.pipeline.utility.steps.json.Messages;
import org.jenkinsci.plugins.pipeline.utility.steps.json.WriteJSONStep;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.Result;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * Tests for {@link WriteJSONStep}.
 *
 * @author Nikolas Falco
 */
public class WriteJSONStepTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void writeFile() throws Exception {
        int elements = 3;
        String input = getJSON(elements);
        File output = temp.newFile();

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "  def json = readJSON text: '" + input + "'\n" +
                        "  json[0] = null\n" +
                        "  json["+ elements + "] = 45\n" +
                        "  writeJSON file: '" + FilenameTestsUtils.toPath(output) + "', json: json\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));

        // file exists by default so we check that should not be empty
        assertThat(output.length(), greaterThan(0l));

        String jsonResult = new String(Files.readAllBytes(Paths.get(output.toURI())));
        JSON json = JSONSerializer.toJSON(jsonResult);
        assertThat(json, instanceOf(JSONArray.class));

        JSONArray jsonArray = (JSONArray) json;
        assertFalse("Saved json is an empty array", jsonArray.isEmpty());
        assertThat(jsonArray.size(), is(elements + 1));
        assertNotNull("Unexpected element value", jsonArray.get(0));
        assertEquals("Unexpected element value", 45, jsonArray.get(elements));
    }

    @Test
    public void checkRequiredFile() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "  def json = readJSON text: '{}'\n" +
                        "  writeJSON json: json" +
                        "}", true));
        WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains(Messages.WriteJSONStepExecution_missingFile("writeJSON"), run);
    }

    @Test
    public void checkRequiredJSON() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "  writeJSON file: 'output.json'" +
                        "}", true));
        WorkflowRun run = j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        j.assertLogContains(Messages.WriteJSONStepExecution_missingJSON("writeJSON"), run);
    }

    private String getJSON(int elements) throws IOException {
        JSONArray root = new JSONArray();
        for (int i = 0; i < elements; i++) {
            JSONObject jsonElement = new JSONObject();
            jsonElement.put("index", i);
            root.add(jsonElement);
        }
        return root.toString();
    }

}