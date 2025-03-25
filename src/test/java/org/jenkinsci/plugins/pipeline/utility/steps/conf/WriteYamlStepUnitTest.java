package org.jenkinsci.plugins.pipeline.utility.steps.conf;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class WriteYamlStepUnitTest {

    @Test
    void writeEmptyListObject() {
        WriteYamlStep writeStep = new WriteYamlStep("/dev/null", new ArrayList<>());
    }

    @Test
    void writeEmptyListOfDocuments() {
        WriteYamlStep writeStep = new WriteYamlStep("/dev/null");
        writeStep.setDatas(new ArrayList<>());
    }

    @Test
    void writeInvalidObject() {
        IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> new WriteYamlStep("/dev/null", new Yaml()));
        assertThat(thrown.getMessage(), equalTo("data parameter has invalid content (no-basic classes)"));
    }

    @Test
    void writeInvalidDocuments() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            WriteYamlStep writeStep = new WriteYamlStep("/dev/null");
            writeStep.setDatas(new ArrayList<>(Collections.singletonList(new Yaml())));
        });
        assertThat(thrown.getMessage(), equalTo("datas parameter has invalid content (no-basic classes)"));
    }
}
