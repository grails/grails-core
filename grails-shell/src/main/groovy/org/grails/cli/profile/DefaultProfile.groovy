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

import groovy.transform.CompileStatic
import jline.console.completer.ArgumentCompleter
import jline.console.completer.Completer
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
    private List<Command> commands = null
    private Map<String, Command> commandsByName = new HashMap<>()

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
           if(cmd instanceof Completer) {
               completers << new ArgumentCompleter(new StringsCompleter(cmd.name), (Completer)cmd)
           }else {
               if(cmd.description.completer) {
                   completers  << new ArgumentCompleter(new StringsCompleter(cmd.name), cmd.description.completer)
               }
               else {
                   completers  << new StringsCompleter(cmd.name)
               }
           }
        }

        return completers
    }

    @Override
    Iterable<Command> getCommands(ProjectContext context) {
        if(commands == null) {
            commands = []
            commands.addAll CommandRegistry.findCommands(this)
            if(parentProfiles) {
                for(parent in parentProfiles) {
                    commands.addAll(parent.getCommands(context).toList())
                }
            }

            for(Command cmd in commands) {
                if(cmd instanceof ProjectContextAware) {
                    ((ProjectContextAware)cmd).projectContext = context
                }
                commandsByName[cmd.name] = cmd
            }
        }
        return commands
    }

    @Override
    boolean handleCommand(ExecutionContext context) {
        getCommands(context) // ensure initialization
        def cmd = commandsByName[context.commandLine.commandName]
        cmd?.handle(context)
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
