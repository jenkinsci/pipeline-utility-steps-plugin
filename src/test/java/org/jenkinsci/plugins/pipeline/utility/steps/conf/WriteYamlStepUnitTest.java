package org.jenkinsci.plugins.pipeline.utility.steps.conf;

import org.junit.Test;
import org.jvnet.hudson.test.WithoutJenkins;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class WriteYamlStepUnitTest {
    @Test
    @WithoutJenkins
    public void writeEmptyListObject() {
        WriteYamlStep writeStep = new WriteYamlStep("/dev/null", new ArrayList<>());
    }

    @Test
    @WithoutJenkins
    public void writeEmptyListOfDocuments() {
        WriteYamlStep writeStep = new WriteYamlStep("/dev/null");
        writeStep.setDatas(new ArrayList<>());
    }

    @Test
    @WithoutJenkins
    public void writeInvalidObject() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> new WriteYamlStep("/dev/null", new Yaml()));
        assertThat(thrown.getMessage(), equalTo("data parameter has invalid content (no-basic classes)"));
    }

    @Test
    @WithoutJenkins
    public void writeInvalidDocuments() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            WriteYamlStep writeStep = new WriteYamlStep("/dev/null");
            writeStep.setDatas(new ArrayList<>(Collections.singletonList(new Yaml())));
        });
        assertThat(thrown.getMessage(), equalTo("datas parameter has invalid content (no-basic classes)"));
    }
}
