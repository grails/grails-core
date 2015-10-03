/*
 * Copyright 2015 original authors
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

import grails.util.CosineSimilarity
import groovy.transform.CompileStatic
import jline.console.completer.ArgumentCompleter
import jline.console.completer.Completer
import org.grails.build.parsing.ScriptNameResolver
import org.grails.cli.interactive.completers.StringsCompleter
import org.grails.cli.profile.commands.CommandRegistry
import org.grails.config.NavigableMap
import org.grails.io.support.Resource


/**
 * Abstract implementation of the profile class
 *
 * @author Graeme Rocher
 * @since 3.1
 */
@CompileStatic
abstract class AbstractProfile implements Profile {
    protected final Resource profileDir
    protected String name
    protected List<Profile> parentProfiles
    protected Map<String, Object> profileConfig
    protected Map<String, Command> commandsByName
    protected NavigableMap navigableConfig
    protected ProfileRepository profileRepository

    AbstractProfile(Resource profileDir) {
        this.profileDir = profileDir
    }

    ProfileRepository getProfileRepository() {
        return profileRepository
    }

    void setProfileRepository(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository
    }

    Resource getProfileDir() {
        return profileDir
    }


    @Override
    NavigableMap getConfiguration() {
        navigableConfig
    }

    @Override
    Resource getTemplate(String path) {
        return profileDir.createRelative("templates/$path")
    }

    @Override
    public Iterable<Profile> getExtends() {
        return parentProfiles;
    }

    @Override
    public Iterable<Completer> getCompleters(ProjectContext context) {
        def commands = getCommands(context)

        Collection<Completer> completers = []

        for(Command cmd in commands) {
           def description = cmd.description

            def commandNameCompleter = new StringsCompleter(cmd.name)
            if(cmd instanceof Completer) {
               completers << new ArgumentCompleter(commandNameCompleter, (Completer)cmd)
           }else {
               if(description.completer) {
                   if(description.flags) {
                       completers  << new ArgumentCompleter(commandNameCompleter,
                                                            description.completer,
                                                            new StringsCompleter(description.flags.collect() { CommandArgument arg -> "-$arg.name".toString() }))
                   }
                   else {
                       completers  << new ArgumentCompleter(commandNameCompleter, description.completer)
                   }

               }
               else {
                   if(description.flags) {
                       completers  << new ArgumentCompleter(commandNameCompleter, new StringsCompleter(description.flags.collect() { CommandArgument arg -> "-$arg.name".toString() }))
                   }
                   else {
                       completers  << commandNameCompleter
                   }
               }
           }
        }

        return completers
    }

    @Override
    Command getCommand(ProjectContext context, String name) {
        getCommands(context)
        return commandsByName[name]
    }

    @Override
    Iterable<Command> getCommands(ProjectContext context) {
        if(commandsByName == null) {
            commandsByName = [:]
            List excludes = []
            def registerCommand = { Command command ->
                def name = command.name
                if(!commandsByName.containsKey(name) && !excludes.contains(name)) {
                    if(command instanceof ProfileRepositoryAware) {
                        ((ProfileRepositoryAware)command).setProfileRepository(profileRepository)
                    }
                    commandsByName[name] = command
                    def desc = command.description
                    def synonyms = desc.synonyms
                    if(synonyms) {
                        for(syn in synonyms) {
                            commandsByName[syn] = command
                        }
                    }
                    if(command instanceof ProjectContextAware) {
                        ((ProjectContextAware)command).projectContext = context
                    }
                    if(command instanceof ProfileCommand) {
                        ((ProfileCommand)command).profile = this
                    }
                }
            }

            CommandRegistry.findCommands(this).each(registerCommand)

            def parents = getExtends()
            if(parents) {
                excludes = (List)configuration.navigate("command", "excludes") ?: []
                registerParentCommands(context, parents, registerCommand)
            }
        }
        return commandsByName.values()
    }

    protected void registerParentCommands(ProjectContext context, Iterable<Profile> parents, Closure registerCommand) {
        for (parent in parents) {
            parent.getCommands(context).each registerCommand

            def extended = parent.extends
            if(extended) {
                registerParentCommands context, extended, registerCommand
            }
        }
    }

    @Override
    boolean hasCommand(ProjectContext context, String name) {
        getCommands(context) // ensure initialization
        return commandsByName.containsKey(name)
    }

    @Override
    boolean handleCommand(ExecutionContext context) {
        getCommands(context) // ensure initialization

        def commandLine = context.commandLine
        def commandName = commandLine.commandName
        def cmd = commandsByName[commandName]
        if(cmd) {
            def requiredArguments = cmd?.description?.arguments
            int requiredArgumentCount = requiredArguments?.findAll() { CommandArgument ca -> ca.required }?.size() ?: 0
            if(commandLine.remainingArgs.size() < requiredArgumentCount) {
                context.console.error "Command [$commandName] missing required arguments: ${requiredArguments*.name}. Type 'grails help $commandName' for more info."
                return false
            }
            else {
                return cmd.handle(context)
            }
        }
        else {
            // Apply command name expansion (rA for run-app, tA for test-app etc.)
            cmd = commandsByName.values().find() { Command c ->
                ScriptNameResolver.resolvesTo(commandName, c.name)
            }
            if(cmd) {
                return cmd.handle(context)
            }
            else {
                context.console.error("Command not found ${context.commandLine.commandName}")
                def mostSimilar = CosineSimilarity.mostSimilar(commandName, commandsByName.keySet())
                List<String> topMatches = mostSimilar.subList(0, Math.min(3, mostSimilar.size()));
                if(topMatches) {
                    context.console.log("Did you mean: ${topMatches.join(' or ')}?")
                }
                return false
            }

        }
    }
}
