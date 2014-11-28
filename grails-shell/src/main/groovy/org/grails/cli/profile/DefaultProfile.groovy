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

import grails.util.Environment
import groovy.transform.CompileStatic
import jline.console.completer.ArgumentCompleter
import jline.console.completer.Completer
import org.grails.build.parsing.CommandLine
import org.grails.cli.interactive.completers.StringsCompleter
import org.grails.cli.profile.commands.CommandRegistry
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
    Map<String, Object> profileConfig
    private Map<String, Command> commandsByName

    private DefaultProfile(String name, File profileDir) {
        this.name = name
        this.profileDir = profileDir
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
                   completers  << new ArgumentCompleter(commandNameCompleter, description.completer)
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
            def registerCommand = { Command command ->
                if(!commandsByName.containsKey(command.name)) {
                    commandsByName[command.name] = command
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
                }
            }
            CommandRegistry.findCommands(this).each(registerCommand)
            if(parentProfiles) {
                for(parent in parentProfiles) {
                    parent.getCommands(context).each(registerCommand)
                }
            }
        }
        return commandsByName.values()
    }

    @Override
    boolean handleCommand(ExecutionContext context) {
        getCommands(context) // ensure initialization

        def commandLine = context.commandLine
        def commandName = commandLine.commandName
        def cmd = commandsByName[commandName]
        if(cmd) {
            return cmd.handle(context)
        }
        else {
            context.console.error("Command not found ${context.commandLine.commandName}")
            return false
        }
    }

    private void initialize(ProfileRepository repository) {
        parentProfiles = []
        File profileYml = new File(profileDir, "profile.yml")
        if(profileYml.isFile()) {
            profileConfig = (Map<String, Object>)profileYml.withInputStream { InputStream it ->
                new Yaml().loadAs(it, Map)
            }
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
