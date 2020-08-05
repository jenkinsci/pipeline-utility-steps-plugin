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

import hudson.FilePath;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileOrTextStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Execution of {@link ReadJSONStep}.
 *
 * @author Nikolas Falco
 */
public class ReadJSONStepExecution extends AbstractFileOrTextStepExecution<Object> {
    private static final long serialVersionUID = 1L;

    private transient ReadJSONStep step;

    protected ReadJSONStepExecution(@Nonnull ReadJSONStep step, @Nonnull StepContext context) {
        super(step, context);
        this.step = step;
    }

    @Override
    protected Object doRun() throws Exception {
        String fName = step.getDescriptor().getFunctionName();
        if (isNotBlank(step.getFile()) && isNotBlank(step.getText())) {
            throw new IllegalArgumentException(Messages.ReadJSONStepExecution_tooManyArguments(fName));
        }

        JSON json = null;
        if (!isBlank(step.getFile())) {
            FilePath f = ws.child(step.getFile());
            if (f.exists() && !f.isDirectory()) {
                try (InputStream is = f.read()) {
                    json = JSONSerializer.toJSON(IOUtils.toString(is, StandardCharsets.UTF_8));
                }
            } else if (f.isDirectory()) {
                throw new IllegalArgumentException(Messages.JSONStepExecution_fileIsDirectory(f.getRemote()));
            } else if (!f.exists()) {
                throw new FileNotFoundException(Messages.JSONStepExecution_fileNotFound(f.getRemote()));
            }
        }
        if (!isBlank(step.getText())) {
            json = JSONSerializer.toJSON(step.getText().trim());
        }

        if (step.getReturnPojo()) {
            return transformToJavaLangStructures(json);
        }
        return json;
    }

    private Object transformToJavaLangStructures(Object object) {
        if (isNull(object)) {
            return null;
        } else if (object instanceof JSONArray) {
            return transformToArrayList((JSONArray) object);
        } else if (object instanceof JSONObject) {
            return transformToLinkedHashMap((JSONObject) object);
        }
        return object;
    }

    private List<Object> transformToArrayList(JSONArray array) {
        List<Object> result = new ArrayList<>(array.size());
        for (Object arrayItem : array) {
            result.add(transformToJavaLangStructures(arrayItem));
        }
        return result;
    }

    private Map<String, Object> transformToLinkedHashMap(JSONObject object) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> objectEntry : (Set<Map.Entry<String, Object>>) object.entrySet()) {
            result.put(objectEntry.getKey(), transformToJavaLangStructures(objectEntry.getValue()));
        }
        return result;
    }

    private boolean isNull(Object value) {
        if (value instanceof JSONNull) {
            return true;
        }
        if (value instanceof JSONObject) {
            try {
                ((Map) value).get((Object) "somekey");
            } catch (JSONException e) {
                // JSONException is returned by verifyIsNull method in JSONObject when accessing one of its properties
                return true;
            }
        }
        return false;
    }

}
