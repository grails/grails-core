/*
 * Copyright 2014 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.cli.profile;

import java.util.Collection;
import java.util.List;

/**
 * Interface to list the available commands and provide an entry point for command execution
 *
 * @author Lari Hotari
 * @author Graeme Rocher
 */
public interface CommandLineHandler {
    /**
     * Handles a command for the given {@link org.grails.cli.profile.ExecutionContext} instance
     *
     * @param context The {@link org.grails.cli.profile.ExecutionContext} instance
     * @return True if the command was executed, false otherwise
     */
    boolean handle(ExecutionContext context);

    /**
     * List the available commands
     *
     * @param context The {@link org.grails.cli.profile.ExecutionContext} instance
     * @return A list of {@link org.grails.cli.profile.CommandDescription} instances
     */
    Collection<CommandDescription> listCommands(ProjectContext context);
}
