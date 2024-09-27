/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.CommandException;
import org.springframework.boot.cli.command.NoSuchCommandException;
import org.springframework.boot.cli.command.status.ExitStatus;
import org.springframework.boot.cli.util.Log;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Main class used to run {@link org.springframework.boot.cli.command.Command}s.
 *
 * @author Phillip Webb
 * @since 1.0.0
 * @see #addCommand(org.springframework.boot.cli.command.Command)
 * @see CommandRunner#runAndHandleErrors(String[])
 */
public class CommandRunner implements Iterable<org.springframework.boot.cli.command.Command> {

	private static final Set<CommandException.Option> NO_EXCEPTION_OPTIONS = EnumSet
		.noneOf(CommandException.Option.class);

	private final String name;

	private final List<org.springframework.boot.cli.command.Command> commands = new ArrayList<>();

	private Class<?>[] optionCommandClasses = {};

	private Class<?>[] hiddenCommandClasses = {};

	/**
	 * Create a new {@link CommandRunner} instance.
	 * @param name the name of the runner or {@code null}
	 */
	public CommandRunner(String name) {
		this.name = StringUtils.hasLength(name) ? name + " " : "";
	}

	/**
	 * Return the name of the runner or an empty string. Non-empty names will include a
	 * trailing space character so that they can be used as a prefix.
	 * @return the name of the runner
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Add the specified commands.
	 * @param commands the commands to add
	 */
	public void addCommands(Iterable<org.springframework.boot.cli.command.Command> commands) {
		Assert.notNull(commands, "Commands must not be null");
		for (org.springframework.boot.cli.command.Command command : commands) {
			addCommand(command);
		}
	}

	/**
	 * Add the specified command.
	 * @param command the command to add.
	 */
	public void addCommand(org.springframework.boot.cli.command.Command command) {
		Assert.notNull(command, "Command must not be null");
		this.commands.add(command);
	}

	/**
	 * Set the command classes which should be considered option commands. An option
	 * command is a special type of command that usually makes more sense to present as if
	 * it is an option. For example '--version'.
	 * @param commandClasses the classes of option commands.
	 * @see #isOptionCommand(org.springframework.boot.cli.command.Command)
	 */
	public void setOptionCommands(Class<?>... commandClasses) {
		Assert.notNull(commandClasses, "CommandClasses must not be null");
		this.optionCommandClasses = commandClasses;
	}

	/**
	 * Set the command classes which should be hidden (i.e. executed but not displayed in
	 * the available commands list).
	 * @param commandClasses the classes of hidden commands
	 */
	public void setHiddenCommands(Class<?>... commandClasses) {
		Assert.notNull(commandClasses, "CommandClasses must not be null");
		this.hiddenCommandClasses = commandClasses;
	}

	/**
	 * Returns if the specified command is an option command.
	 * @param command the command to test
	 * @return {@code true} if the command is an option command
	 * @see #setOptionCommands(Class...)
	 */
	public boolean isOptionCommand(org.springframework.boot.cli.command.Command command) {
		return isCommandInstanceOf(command, this.optionCommandClasses);
	}

	private boolean isHiddenCommand(org.springframework.boot.cli.command.Command command) {
		return isCommandInstanceOf(command, this.hiddenCommandClasses);
	}

	private boolean isCommandInstanceOf(org.springframework.boot.cli.command.Command command, Class<?>[] commandClasses) {
		for (Class<?> commandClass : commandClasses) {
			if (commandClass.isInstance(command)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterator<org.springframework.boot.cli.command.Command> iterator() {
		return getCommands().iterator();
	}

	protected final List<org.springframework.boot.cli.command.Command> getCommands() {
		return Collections.unmodifiableList(this.commands);
	}

	/**
	 * Find a command by name.
	 * @param name the name of the command
	 * @return the command or {@code null} if not found
	 */
	public org.springframework.boot.cli.command.Command findCommand(String name) {
		for (org.springframework.boot.cli.command.Command candidate : this.commands) {
			String candidateName = candidate.getName();
			if (candidateName.equals(name) || (isOptionCommand(candidate) && ("--" + candidateName).equals(name))) {
				return candidate;
			}
		}
		return null;
	}

	/**
	 * Run the appropriate and handle and errors.
	 * @param args the input arguments
	 * @return a return status code (non boot is used to indicate an error)
	 */
	public int runAndHandleErrors(String... args) {
		String[] argsWithoutDebugFlags = removeDebugFlags(args);
		boolean debug = argsWithoutDebugFlags.length != args.length;
		if (debug) {
			System.setProperty("debug", "true");
		}
		try {
			ExitStatus result = run(argsWithoutDebugFlags);
			// The caller will hang up if it gets a non-zero status
			if (result != null && result.isHangup()) {
				return (result.getCode() > 0) ? result.getCode() : 0;
			}
			return 0;
		}
		catch (NoArgumentsException ex) {
			showUsage();
			return 1;
		}
		catch (Exception ex) {
			return handleError(debug, ex);
		}
	}

	private String[] removeDebugFlags(String[] args) {
		List<String> rtn = new ArrayList<>(args.length);
		boolean appArgsDetected = false;
		for (String arg : args) {
			// Allow apps to have a --debug argument
			appArgsDetected |= "--".equals(arg);
			if ("--debug".equals(arg) && !appArgsDetected) {
				continue;
			}
			rtn.add(arg);
		}
		return StringUtils.toStringArray(rtn);
	}

	/**
	 * Parse the arguments and run a suitable command.
	 * @param args the arguments
	 * @return the outcome of the command
	 * @throws Exception if the command fails
	 */
	protected ExitStatus run(String... args) throws Exception {
		if (args.length == 0) {
			throw new NoArgumentsException();
		}
		String commandName = args[0];
		String[] commandArguments = Arrays.copyOfRange(args, 1, args.length);
		org.springframework.boot.cli.command.Command command = findCommand(commandName);
		if (command == null) {
			throw new NoSuchCommandException(commandName);
		}
		beforeRun(command);
		try {
			return command.run(commandArguments);
		}
		finally {
			afterRun(command);
		}
	}

	/**
	 * Subclass hook called before a command is run.
	 * @param command the command about to run
	 */
	protected void beforeRun(org.springframework.boot.cli.command.Command command) {
	}

	/**
	 * Subclass hook called after a command has run.
	 * @param command the command that has run
	 */
	protected void afterRun(org.springframework.boot.cli.command.Command command) {
	}

	private int handleError(boolean debug, Exception ex) {
		Set<CommandException.Option> options = NO_EXCEPTION_OPTIONS;
		if (ex instanceof CommandException) {
			options = ((CommandException) ex).getOptions();
			if (options.contains(CommandException.Option.RETHROW)) {
				throw (CommandException) ex;
			}
		}
		boolean couldNotShowMessage = false;
		if (!options.contains(CommandException.Option.HIDE_MESSAGE)) {
			couldNotShowMessage = !errorMessage(ex.getMessage());
		}
		if (options.contains(CommandException.Option.SHOW_USAGE)) {
			showUsage();
		}
		if (debug || couldNotShowMessage || options.contains(CommandException.Option.STACK_TRACE)) {
			printStackTrace(ex);
		}
		return 1;
	}

	protected boolean errorMessage(String message) {
		Log.error((message != null) ? message : "Unexpected error");
		return message != null;
	}

	protected void showUsage() {
		Log.infoPrint("usage: " + this.name);
		for (org.springframework.boot.cli.command.Command command : this.commands) {
			if (isOptionCommand(command)) {
				Log.infoPrint("[--" + command.getName() + "] ");
			}
		}
		Log.info("");
		Log.info("       <command> [<args>]");
		Log.info("");
		Log.info("Available commands are:");
		for (Command command : this.commands) {
			if (!isOptionCommand(command) && !isHiddenCommand(command)) {
				String usageHelp = command.getUsageHelp();
				String description = command.getDescription();
				Log.info(String.format("%n  %1$s %2$-15s%n    %3$s", command.getName(),
						(usageHelp != null) ? usageHelp : "", (description != null) ? description : ""));
			}
		}
		Log.info("");
		Log.info("Common options:");
		Log.info(String.format("%n  %1$s %2$-15s%n    %3$s", "--debug", "Verbose mode",
				"Print additional status information for the command you are running"));
		Log.info("");
		Log.info("");
		Log.info("See '" + this.name + "help <command>' for more information on a specific command.");
	}

	protected void printStackTrace(Exception ex) {
		Log.error("");
		Log.error(ex);
		Log.error("");
	}

}
