package org.jenkinsci.plugins.pipeline.utility.steps.conf;

import org.jenkinsci.plugins.pipeline.utility.steps.shaded.org.yaml.snakeyaml.Yaml;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.jvnet.hudson.test.WithoutJenkins;

import java.util.ArrayList;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;

public class WriteYamlStepUnitTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test @WithoutJenkins
    public void writeEmptyListObject() throws Exception {
        WriteYamlStep writeStep = new WriteYamlStep("/dev/null", new ArrayList<>());
    }

    @Test @WithoutJenkins
    public void writeEmptyListOfDocuments() throws Exception {
        WriteYamlStep writeStep = new WriteYamlStep("/dev/null");
        writeStep.setDatas(new ArrayList<>());
    }

    @Test @WithoutJenkins
    public void writeInvalidObject() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(equalTo("data parameter has invalid content (no-basic classes)"));

        WriteYamlStep writeStep = new WriteYamlStep("/dev/null", new Yaml());
    }

    @Test @WithoutJenkins
    public void writeInvalidDocuments() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(equalTo("datas parameter has invalid content (no-basic classes)"));

        WriteYamlStep writeStep = new WriteYamlStep("/dev/null");
        writeStep.setDatas(new ArrayList<>(Collections.singletonList(new Yaml())));
    }
}
