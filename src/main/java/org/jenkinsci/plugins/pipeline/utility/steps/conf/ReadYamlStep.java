/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Philippe GRANET
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

package org.jenkinsci.plugins.pipeline.utility.steps.conf;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileOrTextStep;
import org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileOrTextStepDescriptorImpl;
import org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileOrTextStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.FilePath;
import org.kohsuke.stapler.DataBoundSetter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Reads a yaml file from the workspace.
 *
 * @author Philippe GRANET &lt;philippe.granet@gmail.com&gt;.
 */
public class ReadYamlStep extends AbstractFileOrTextStep {

	public static final int LIBRARY_DEFAULT_CODE_POINT_LIMIT = new LoaderOptions().getCodePointLimit();
	public static final String MAX_CODE_POINT_LIMIT_PROPERTY = ReadYamlStep.class.getName() + ".MAX_CODE_POINT_LIMIT";
	@SuppressFBWarnings(value={"MS_SHOULD_BE_FINAL"}, justification="Non final so that an admin can adjust the value through the groovy script console without restarting the instance.")
	private static /*almost final*/ int MAX_CODE_POINT_LIMIT = Integer.getInteger(MAX_CODE_POINT_LIMIT_PROPERTY, LIBRARY_DEFAULT_CODE_POINT_LIMIT);
	public static final String DEFAULT_CODE_POINT_LIMIT_PROPERTY = ReadYamlStep.class.getName() + ".DEFAULT_CODE_POINT_LIMIT";
	@SuppressFBWarnings(value={"MS_SHOULD_BE_FINAL"}, justification="Non final so that an admin can adjust the value through the groovy script console without restarting the instance.")
	private static /*almost final*/ int DEFAULT_CODE_POINT_LIMIT = Integer.getInteger(DEFAULT_CODE_POINT_LIMIT_PROPERTY, -1);
	//By default, use whatever Yaml thinks is best
	private int codePointLimit = -1;

	// the upper limit is hardcoded to 1000 to stop people shooting themselves in the foot
	public static final int HARDCODED_CEILING_MAX_ALIASES_FOR_COLLECTIONS = 1000;
	public static final int LIBRARY_DEFAULT_MAX_ALIASES_FOR_COLLECTIONS = new LoaderOptions().getMaxAliasesForCollections();
	public static final String MAX_MAX_ALIASES_PROPERTY = ReadYamlStep.class.getName() + ".MAX_MAX_ALIASES_FOR_COLLECTIONS";
	@SuppressFBWarnings(value={"MS_SHOULD_BE_FINAL"}, justification="Non final so that an admin can adjust the value through the groovy script console without restarting the instance.")
	private static /*almost final*/ int MAX_MAX_ALIASES_FOR_COLLECTIONS = setMaxMaxAliasesForCollections(Integer.getInteger(MAX_MAX_ALIASES_PROPERTY, LIBRARY_DEFAULT_MAX_ALIASES_FOR_COLLECTIONS));
	public static final String DEFAULT_MAX_ALIASES_PROPERTY = ReadYamlStep.class.getName() + ".DEFAULT_MAX_ALIASES_FOR_COLLECTIONS";
	// JENKINS-70557: MAX_MAX_ALIASES_FOR_COLLECTIONS must be set first, as setDefaultMaxAliasesForCollections() utilizes it
	@SuppressFBWarnings(value={"MS_SHOULD_BE_FINAL"}, justification="Non final so that an admin can adjust the value through the groovy script console without restarting the instance.")
	private static /*almost final*/ int DEFAULT_MAX_ALIASES_FOR_COLLECTIONS = setDefaultMaxAliasesForCollections(Integer.getInteger(DEFAULT_MAX_ALIASES_PROPERTY, -1));
	//By default, use whatever Yaml thinks is best
	private int maxAliasesForCollections = -1;

	@DataBoundConstructor
	public ReadYamlStep() {
	}

	public static int getMaxCodePointLimit() {
		return MAX_CODE_POINT_LIMIT;
	}

	/**
	 * Setter with an added check to ensure the default does not exceed the max value.
	 * @param defaultCodePointLimit the default value to set.
	 * @return the actual value set after checking the max allowed.
	 */
	public static int setDefaultCodePointLimit(int defaultCodePointLimit) {
		if (defaultCodePointLimit > MAX_CODE_POINT_LIMIT) {
			throw new IllegalArgumentException(defaultCodePointLimit + " > " + MAX_CODE_POINT_LIMIT +
					". Reduce the required DEFAULT_CODE_POINT_LIMIT or convince your administrator to increase" +
					" the max allowed value with the system property \"" + MAX_CODE_POINT_LIMIT_PROPERTY + "\"");
		}
		DEFAULT_CODE_POINT_LIMIT = defaultCodePointLimit;
		return DEFAULT_CODE_POINT_LIMIT;
	}

	public static int getDefaultCodePointLimit() {
		return DEFAULT_CODE_POINT_LIMIT;
	}

	/**
	 * Setter with an added check to ensure the default does not exceed the hardcoded max value.
	 * TODO: decide if we want to add a message here before failing back.
	 * @param maxMaxAliasesForCollections maximum allowed aliases to be set.
	 * @return the resulting value after checking the ceiling.
	 */
	public static int setMaxMaxAliasesForCollections(int maxMaxAliasesForCollections) {
		MAX_MAX_ALIASES_FOR_COLLECTIONS = Math.min(maxMaxAliasesForCollections, HARDCODED_CEILING_MAX_ALIASES_FOR_COLLECTIONS);
		return MAX_MAX_ALIASES_FOR_COLLECTIONS;
	}

	public static int getMaxMaxAliasesForCollections() {
		return MAX_MAX_ALIASES_FOR_COLLECTIONS;
	}

	/**
	 * Setter with an added check to ensure the default does not exceed the max value.
	 * @param defaultMaxAliasesForCollections the default value to set.
	 * @return the actual value set after checking the max allowed.
	 */
	public static int setDefaultMaxAliasesForCollections(int defaultMaxAliasesForCollections) {
		if (defaultMaxAliasesForCollections > HARDCODED_CEILING_MAX_ALIASES_FOR_COLLECTIONS) {
			throw new IllegalArgumentException(defaultMaxAliasesForCollections + " > " + HARDCODED_CEILING_MAX_ALIASES_FOR_COLLECTIONS +
					". Hardcoded upper limit breached. Reduce the required DEFAULT_MAX_ALIASES_FOR_COLLECTIONS or convince the plugin maintainers to increase" +
					" the HARDCODED_CEILING_MAX_ALIASES_FOR_COLLECTIONS property (added to stop people shooting themselves in the foot).");
		} else if (defaultMaxAliasesForCollections > MAX_MAX_ALIASES_FOR_COLLECTIONS) {
			throw new IllegalArgumentException(defaultMaxAliasesForCollections + " > " + MAX_MAX_ALIASES_FOR_COLLECTIONS +
					". Reduce the required DEFAULT_MAX_ALIASES_FOR_COLLECTIONS or convince your administrator to increase" +
					" the max allowed value with the system property \"" + MAX_MAX_ALIASES_PROPERTY + "\"");
		}
		DEFAULT_MAX_ALIASES_FOR_COLLECTIONS = defaultMaxAliasesForCollections;
		return DEFAULT_MAX_ALIASES_FOR_COLLECTIONS;
	}


	public static int getDefaultMaxAliasesForCollections() {
		return DEFAULT_MAX_ALIASES_FOR_COLLECTIONS;
	}

	public int getCodePointLimit() {
		return codePointLimit;
	}

	@DataBoundSetter
	public void setCodePointLimit(final int codePointLimit) {
		if (codePointLimit > MAX_CODE_POINT_LIMIT) {
			throw new IllegalArgumentException(codePointLimit + " > " + MAX_CODE_POINT_LIMIT +
					". Reduce the code points in your yaml or convince your administrator to increase" +
					" the max allowed value with the system property \"" + MAX_CODE_POINT_LIMIT_PROPERTY + "\"");
		}
		this.codePointLimit = codePointLimit;
	}

	public int getMaxAliasesForCollections() {
		return maxAliasesForCollections;
	}

	@DataBoundSetter
	public void setMaxAliasesForCollections(final int maxAliasesForCollections) {
		if (maxAliasesForCollections > MAX_MAX_ALIASES_FOR_COLLECTIONS) {
			throw new IllegalArgumentException(maxAliasesForCollections + " > " + MAX_MAX_ALIASES_FOR_COLLECTIONS +
					". Reduce the aliases in your yaml or convince your administrator to increase" +
					" the max allowed value with the system property \"" + MAX_MAX_ALIASES_PROPERTY + "\"");
		}
		this.maxAliasesForCollections = maxAliasesForCollections;
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		int ac = -1;
		if (maxAliasesForCollections >= 0) {
			ac = maxAliasesForCollections;
		} else if (DEFAULT_MAX_ALIASES_FOR_COLLECTIONS >= 0) {
			ac = DEFAULT_MAX_ALIASES_FOR_COLLECTIONS;
		}

		int cpl = -1;
		if (codePointLimit >= 0) {
			cpl = codePointLimit;
		} else if (DEFAULT_CODE_POINT_LIMIT >= 0) {
			cpl = DEFAULT_CODE_POINT_LIMIT;
		}

		return new Execution(this, context, ac, cpl);
	}

	@Extension
	public static class DescriptorImpl extends AbstractFileOrTextStepDescriptorImpl {

		public DescriptorImpl() {
		}

		@Override
		public String getFunctionName() {
			return "readYaml";
		}

		@Override
		@NonNull
		public String getDisplayName() {
			return "Read yaml from a file in the workspace or text.";
		}
	}

	public static class Execution extends AbstractFileOrTextStepExecution<Object> {
		private static final long serialVersionUID = 1L;
		private transient ReadYamlStep step;

		private final int codePointLimit;

		private final int maxAliasesForCollections;

		protected Execution(@NonNull ReadYamlStep step, @NonNull StepContext context, int maxAliasesForCollections, int codePointLimit) {
			super(step, context);
			this.step = step;
			this.maxAliasesForCollections = maxAliasesForCollections;
			this.codePointLimit = codePointLimit;
		}

		/**
		  * @return <ul>
		  * 	<li>Map&lt;String, Object&gt; if only one YAML document</li>
		  * 	<li>list of Map&lt;String, Object&gt; if multiple YAML document</li>
		 * 	</ul>
		 */
		@Override
		protected Object doRun() throws Exception {
			String yamlText = "";
			if (!isBlank(step.getFile())) {
				FilePath path = ws.child(step.getFile());
				if (!path.exists()) {
					throw new FileNotFoundException(path.getRemote() + " does not exist.");
				}
				if (path.isDirectory()) {
					throw new FileNotFoundException(path.getRemote() + " is a directory.");
				}

				// Generic unicode textreader, which will use BOM mark to identify the encoding
				// to be used. If BOM is not found then use a given default or system encoding.
				try(Reader reader=new UnicodeReader(path.read())){
					yamlText = IOUtils.toString(reader);
				}
			}
			if (!isBlank(step.getText())) {
				yamlText += System.getProperty("line.separator") + step.getText();
			}

			Iterable<Object> yaml = newYaml().loadAll(yamlText);

			List<Object> result = new LinkedList<>();
			for (Object data : yaml) {
				result.add(data);
			}

			// Ensure that result is serializable
			// Everything used in the pipeline needs to be Serializable
			try(ObjectOutputStream out=new ObjectOutputStream(new ByteArrayOutputStream())){
				out.writeObject(result);
			}

			// if only one YAML document, return it directly
			if (result.size() == 1) {
				return result.get(0);
			}

			return result;
		}

		protected Yaml newYaml() {
			//Need everything just to be able to specify constructor and LoaderOptions
			LoaderOptions loaderOptions = new LoaderOptions();
			if (maxAliasesForCollections >= 0) {
				loaderOptions.setMaxAliasesForCollections(maxAliasesForCollections);
			}
			if (codePointLimit >= 0) {
				loaderOptions.setCodePointLimit(codePointLimit);
			}

			Representer representer = new Representer(new DumperOptions());
			// The Yaml constructor does this internally, so just in case...
			DumperOptions dumperOptions = new DumperOptions();
			dumperOptions.setDefaultFlowStyle(representer.getDefaultFlowStyle());
			dumperOptions.setDefaultScalarStyle(representer.getDefaultScalarStyle());
			dumperOptions.setAllowReadOnlyProperties(representer.getPropertyUtils().isAllowReadOnlyProperties());
			dumperOptions.setTimeZone(representer.getTimeZone());
			// Use SafeConstructor to limit objects to standard Java objects like List or Long
			return new Yaml(new SafeConstructor(new LoaderOptions()), representer, dumperOptions, loaderOptions);
		}
	}
}
