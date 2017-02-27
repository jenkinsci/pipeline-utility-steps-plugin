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

package org.jenkinsci.plugins.pipeline.utility.steps.conf.json;

import static org.hamcrest.Matchers.*;
import static org.jenkinsci.plugins.pipeline.utility.steps.FilenameTestsUtils.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.jenkinsci.plugins.pipeline.utility.steps.json.WriteJSONStep;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

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
        File file = writeJSON(elements);
        File result = temp.newFile();

        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "  def json = readJSON file: '" + separatorsToSystemEscaped(file.getAbsolutePath()) + "'\n" +
                        "  json[0] = null\n" +
                        "  json["+ elements + "] = 45\n" +
                        "  writeJSON file: '" + separatorsToSystemEscaped(result.getAbsolutePath()) + "', json: json\n" +
                        "}", true));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));

        // file exists by default so we check that should not be empty
        assertThat(result.length(), greaterThan(0l));

        String jsonResult = new String(Files.readAllBytes(Paths.get(result.toURI())));
        JSON json = JSONSerializer.toJSON(jsonResult);
        assertThat(json, instanceOf(JSONArray.class));

        JSONArray jsonArray = (JSONArray) json;
        assertFalse("Saved json is an empty array", jsonArray.isEmpty());
        assertThat(jsonArray.size(), is(elements + 1));
        assertNotNull("Unexpected element value", jsonArray.get(0));
        assertEquals("Unexpected element value", 45, jsonArray.get(elements));
    }

    private File writeJSON(int elements) throws IOException {
        JSONArray tags = new JSONArray();
        for (int i = 0; i < 3; i++) {
            JSONObject jsonElement = new JSONObject();
            jsonElement.put("index", i);
            tags.add(jsonElement);
        }
        File file = temp.newFile();
        try (FileWriter f = new FileWriter(file)) {
            tags.write(f);
        }
        return file;
    }

}