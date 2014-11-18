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
package org.grails.cli.profile.simple

import groovy.transform.CompileStatic
import jline.console.completer.Completer

import org.grails.cli.profile.CommandLineHandler
import org.grails.cli.profile.Profile
import org.grails.cli.profile.ProfileRepository
import org.grails.cli.profile.ProjectContext
import org.yaml.snakeyaml.Yaml;

/**
 * Simple disk based implementation of the {@link Profile} interface
 *
 * @since 3.0
 * @author Lari Hotari
 * @author Graeme Rocher
 */
@CompileStatic
class SimpleProfile implements Profile {
    File profileDir
    String name
    private List<CommandLineHandler> commandLineHandlers = null
    List<Profile> parentProfiles
    Map<String, Object> profileConfig

    private SimpleProfile(String name, File profileDir) {
        this.name = name
        this.profileDir = profileDir
    }

    public static SimpleProfile create(ProfileRepository repository, String name, File profileDir) {
        SimpleProfile profile = new SimpleProfile(name, profileDir)
        profile.initialize(repository)
        profile
    }

    @Override
    public Iterable<Profile> getExtends() {
        return parentProfiles;
    }

    @Override
    public Iterable<Completer> getCompleters(ProjectContext context) {
        [ new CommandLineHandlersCompleter(context:context, commandLineHandlersClosure:{ -> this.getCommandLineHandlers(context) }) ]
    }

    @Override
    public Iterable<CommandLineHandler> getCommandLineHandlers(ProjectContext context) {
        if(commandLineHandlers == null) {
            commandLineHandlers = []
            Collection<File> commandFiles = findCommandFiles()
            SimpleCommandHandler commandHandler = createCommandHandler(commandFiles)
            commandHandler.initialize()
            commandLineHandlers << commandHandler
            addParentCommandLineHandlers(context, commandLineHandlers)
        }
        commandLineHandlers
    }

    protected void addParentCommandLineHandlers(ProjectContext context, List<CommandLineHandler> commandLineHandlers) {
        parentProfiles.each {
            it.getCommandLineHandlers(context)?.each { CommandLineHandler handler ->
                commandLineHandlers.add(handler)
            }
        }
    }

    protected Collection<File> findCommandFiles() {
        File commandsDir = new File(profileDir, "commands")
        Collection<File> commandFiles = commandsDir.listFiles().findAll { File file ->
            file.isFile() && file.name ==~ /^.*\.(yml|json)$/
        }.sort(false) { File file -> file.name }
        return commandFiles
    }

    protected SimpleCommandHandler createCommandHandler(Collection<File> commandFiles) {
        return new SimpleCommandHandler(commandFiles,this)
    }

    private void initialize(ProfileRepository repository) {
        parentProfiles = []
        File profileYml = new File(profileDir, "profile.yml")
        if(profileYml.isFile()) {
            profileConfig = (Map<String, Object>)profileYml.withInputStream {
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
            } else {
            }
        }
    }
}
