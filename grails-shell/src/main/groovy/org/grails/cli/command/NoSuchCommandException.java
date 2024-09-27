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

import org.springframework.boot.cli.command.CommandException;

/**
 * Exception used when a command is not found.
 *
 * @author Phillip Webb
 * @since 1.0.0
 */
public class NoSuchCommandException extends CommandException {

	private static final long serialVersionUID = 1L;

	public NoSuchCommandException(String name) {
		super(String.format("'%1$s' is not a valid command. See 'help'.", name));
	}

}
