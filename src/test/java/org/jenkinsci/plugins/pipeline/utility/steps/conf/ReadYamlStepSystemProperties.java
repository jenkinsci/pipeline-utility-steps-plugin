package org.jenkinsci.plugins.pipeline.utility.steps.conf;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.FlagRule;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;

public class ReadYamlStepSystemProperties {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    private final int testValue=51;

    @Rule
    public final FlagRule<String> defaultAliases=FlagRule.systemProperty("org.jenkinsci.plugins.pipeline.utility.steps.conf.ReadYamlStep.DEFAULT_MAX_ALIASES_FOR_COLLECTIONS", String.valueOf(testValue));

    @Rule
    public final FlagRule<String> maxAliases=FlagRule.systemProperty("org.jenkinsci.plugins.pipeline.utility.steps.conf.ReadYamlStep.MAX_MAX_ALIASES_FOR_COLLECTIONS", String.valueOf(testValue));

    @Test
    public void testSettingYamlAliasesSetAtStartup() throws Exception {
        ReadYamlStep readYamlStep = new ReadYamlStep();
        assertEquals(testValue, readYamlStep.getDefaultMaxAliasesForCollections());
        assertEquals(testValue, readYamlStep.getMaxMaxAliasesForCollections());
    }
}
