/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grails.cli.command;

import java.util.Collection;

import org.springframework.boot.cli.command.HelpExample;
import org.springframework.boot.cli.command.options.OptionHelp;
import org.springframework.boot.cli.command.status.ExitStatus;

/**
 * A single command that can be run from the CLI.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Stephane Nicoll
 * @since 1.0.0
 * @see #run(String...)
 */
public interface Command {

	/**
	 * Returns the name of the command.
	 * @return the command's name
	 */
	String getName();

	/**
	 * Returns a description of the command.
	 * @return the command's description
	 */
	String getDescription();

	/**
	 * Returns usage help for the command. This should be a simple one-line string
	 * describing basic usage. e.g. '[options] &lt;file&gt;'. Do not include the name of
	 * the command in this string.
	 * @return the command's usage help
	 */
	String getUsageHelp();

	/**
	 * Gets full help text for the command, e.g. a longer description and one line per
	 * option.
	 * @return the command's help text
	 */
	String getHelp();

	/**
	 * Returns help for each supported option.
	 * @return help for each of the command's options
	 */
	Collection<OptionHelp> getOptionsHelp();

	/**
	 * Return some examples for the command.
	 * @return the command's examples
	 */
	Collection<HelpExample> getExamples();

	/**
	 * Run the command.
	 * @param args command arguments (this will not include the command itself)
	 * @return the outcome of the command
	 * @throws Exception if the command fails
	 */
	ExitStatus run(String... args) throws Exception;

}
