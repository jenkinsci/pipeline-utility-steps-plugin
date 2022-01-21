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

package org.jenkinsci.plugins.pipeline.utility.steps.fs;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.NonNull;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

/**
 * Prepend given content to a given file.
 */
public class PrependToFileStep extends Step {
	private final String file;
	private final String content;

	@DataBoundConstructor
	public PrependToFileStep(final String file, final String content) throws Descriptor.FormException {
		if (StringUtils.isBlank(file)) {
			throw new Descriptor.FormException("can't be blank", "file");
		}
		this.file = file;

		if (StringUtils.isBlank(content)) {
			throw new Descriptor.FormException("can't be blank", "content");
		}
		this.content = content;
	}

	/**
	 * The file to prepend to.
	 *
	 * @return the file
	 */
	public String getFile() {
		return this.file;
	}

	/**
	 * Gets the content to prepend.
	 *
	 * @return the content
	 */
	public String getContent() {
		return this.content;
	}

	@Override
	public StepExecution start(final StepContext context) throws Exception {
		return new ExecutionImpl(this, context);
	}

	@Extension
	public static class DescriptorImpl extends StepDescriptor {

		public DescriptorImpl() {

		}

		@Override
		public Set<? extends Class<?>> getRequiredContext() {
			return Collections.singleton(FilePath.class);
		}

		@Override
		public String getFunctionName() {
			return "prependToFile";
		}

		@Override
		public String getDisplayName() {
			return "Create a file (if not already exist) in the workspace, and prepend given content to that file.";
		}

		@SuppressWarnings("unused")
		public FormValidation doCheckFile(@QueryParameter final String value) {
			if (StringUtils.isBlank(value)) {
				return FormValidation.error("Needs a value");
			} else {
				return FormValidation.ok();
			}
		}

		@SuppressWarnings("unused")
		public FormValidation doCheckContent(@QueryParameter final String value) {
			if (StringUtils.isBlank(value)) {
				return FormValidation.error("Needs a value");
			} else {
				return FormValidation.ok();
			}
		}
	}

	/**
	 * The execution of {@link PrependToFileStep}.
	 */
	public static class ExecutionImpl extends SynchronousNonBlockingStepExecution<FileWrapper> {
		private static final long serialVersionUID = 1L;

		private transient PrependToFileStep step;

		protected ExecutionImpl(@NonNull final PrependToFileStep step, @NonNull final StepContext context) {
			super(context);
			this.step = step;
		}

		@Override
		protected FileWrapper run() throws Exception {
			final FilePath ws = this.getContext().get(FilePath.class);
			assert ws != null;
			final FilePath file = ws.child(this.step.getFile());
			if (!file.exists()) {
				file.getParent().mkdirs();
				file.touch(System.currentTimeMillis());
			}
			final String content = this.step.getContent();
			final String originalContent = file.readToString();
			final String newContent = content + originalContent;
			file.write(newContent, StandardCharsets.UTF_8.name());
			return new FileWrapper(file);
		}
	}
}
