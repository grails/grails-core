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
package org.grails.cli.profile

import groovy.transform.CompileStatic

/**
 * A {@link CommandLineHandler} that can read commands defined in JSON or YAML
 *
 * @author Lari Hotari
 * @author Graeme Rcher
 */
@CompileStatic
class DefaultCommandHandler implements CommandLineHandler {

    Profile profile

    protected List<CommandDescription> commandDescriptions
    protected Map<String, Command> commands = [:]

    DefaultCommandHandler(Collection<Command> commands, Profile profile) {
        for(Command c in commands) {
            this.commands.put(c.name, c)
        }
        this.commandDescriptions = this.commands.values().collect() { Command c -> c.description }
        this.profile = profile
    }


    @Override
    public boolean handle(ExecutionContext context) {
        Command cmd = commands.get(context.commandLine.getCommandName())
        if(cmd) {
            return cmd.handle(context)
        }
        return false;
    }

    @Override
    public List<CommandDescription> listCommands(ProjectContext context) {
        return commandDescriptions;
    }

}
