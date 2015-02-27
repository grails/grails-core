/*
 * Copyright 2014 the original author or authors.
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
import grails.util.Environment
import groovy.transform.CompileStatic
import jline.console.completer.ArgumentCompleter
import jline.console.completer.Completer
import org.grails.build.parsing.CommandLine
import org.grails.build.parsing.ScriptNameResolver
import org.grails.cli.interactive.completers.StringsCompleter
import org.grails.cli.profile.commands.CommandRegistry
import org.grails.config.NavigableMap
import org.yaml.snakeyaml.Yaml;

/**
 * Simple disk based implementation of the {@link Profile} interface
 *
 * @since 3.0
 * @author Lari Hotari
 * @author Graeme Rocher
 */
@CompileStatic
class DefaultProfile implements Profile {
    final File profileDir
    final String name
    List<Profile> parentProfiles
    private Map<String, Object> profileConfig
    private Map<String, Command> commandsByName
    private NavigableMap navigableConfig = new NavigableMap()
    private ProfileRepository profileRepository

    private DefaultProfile(String name, File profileDir) {
        this.name = name
        this.profileDir = profileDir
    }

    @Override
    NavigableMap getConfiguration() {
        navigableConfig
    }

    public static Profile create(ProfileRepository repository, String name, File profileDir) {
        Profile profile = new DefaultProfile(name, profileDir)
        profile.initialize(repository)
        return profile
    }

    @Override
    public Iterable<Profile> getExtends() {
        return parentProfiles;
    }

    @Override
    public Iterable<Completer> getCompleters(ProjectContext context) {
        def commands = getCommands(context)

        Collection<Completer> completers = []

        // TODO: report Groovy @CompileStatic bug
        commands.each { Command cmd ->
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

            def parents = parentProfiles
            if(parents) {
                excludes = (List)configuration.navigate("command", "excludes") ?: []
                registerParentCommands(parents, registerCommand)
            }
        }
        return commandsByName.values()
    }

    protected void registerParentCommands(Iterable<Profile> parents, Closure<DefaultProfile> registerCommand) {
        for (parent in parents) {
            CommandRegistry.findCommands(parent, true).each registerCommand

            def extended = parent.extends
            if(extended) {
                registerParentCommands extended, registerCommand
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

    private void initialize(ProfileRepository repository) {
        this.profileRepository = repository
        parentProfiles = []
        File profileYml = new File(profileDir, "profile.yml")
        if(profileYml.isFile()) {
            profileConfig = (Map<String, Object>)profileYml.withInputStream { InputStream it ->
                new Yaml().loadAs(it, Map)
            }
            navigableConfig.merge(profileConfig)
            String[] extendsProfiles = profileConfig.get("extends")?.toString()?.split(/\s*,\s*/)
            if(extendsProfiles) {
                parentProfiles = extendsProfiles.collect { String profileName ->
                    Profile extendsProfile = repository.getProfile(profileName)
                    if(extendsProfile==null) {
                        throw new RuntimeException("Profile $profileName not found. ${this.name} extends it.")
                    }
                    extendsProfile
                }
            }
        }
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        DefaultProfile that = (DefaultProfile) o

        if (name != that.name) return false

        return true
    }

    int hashCode() {
        return (name != null ? name.hashCode() : 0)
    }
}
