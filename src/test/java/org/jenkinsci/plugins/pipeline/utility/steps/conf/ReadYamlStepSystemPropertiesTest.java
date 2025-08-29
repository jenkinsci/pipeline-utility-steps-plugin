package org.jenkinsci.plugins.pipeline.utility.steps.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ReadYamlStepSystemPropertiesTest {

    private final int testValue = 51;

    private String defaultAliases;

    private String maxAliases;

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
        defaultAliases = System.setProperty(
                ReadYamlStep.class.getName() + ".DEFAULT_MAX_ALIASES_FOR_COLLECTIONS", String.valueOf(testValue));
        maxAliases = System.setProperty(
                ReadYamlStep.class.getName() + ".MAX_MAX_ALIASES_FOR_COLLECTIONS", String.valueOf(testValue));
    }

    @AfterEach
    void tearDown() {
        if (defaultAliases != null) {
            System.setProperty(ReadYamlStep.class.getName() + ".DEFAULT_MAX_ALIASES_FOR_COLLECTIONS", defaultAliases);
        } else {
            System.clearProperty(ReadYamlStep.class.getName() + ".DEFAULT_MAX_ALIASES_FOR_COLLECTIONS");
        }
        if (maxAliases != null) {
            System.setProperty(ReadYamlStep.class.getName() + ".MAX_MAX_ALIASES_FOR_COLLECTIONS", maxAliases);
        } else {
            System.clearProperty(ReadYamlStep.class.getName() + ".MAX_MAX_ALIASES_FOR_COLLECTIONS");
        }
    }

    @Test
    void testSettingYamlAliasesSetAtStartup() {
        ReadYamlStep readYamlStep = new ReadYamlStep();
        assertEquals(testValue, readYamlStep.getDefaultMaxAliasesForCollections());
        assertEquals(testValue, readYamlStep.getMaxMaxAliasesForCollections());
    }
}
