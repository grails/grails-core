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
package org.grails.cli.profile.repository

import groovy.transform.CompileStatic
import org.grails.cli.profile.AbstractProfile
import org.grails.cli.profile.Command
import org.grails.cli.profile.Profile
import org.grails.cli.profile.ProfileRepository
import org.grails.cli.profile.ProjectContext
import org.grails.cli.profile.ProjectContextAware
import org.grails.cli.profile.commands.DefaultMultiStepCommand
import org.grails.cli.profile.commands.script.GroovyScriptCommand
import org.grails.config.NavigableMap
import org.grails.io.support.ClassPathResource
import org.grails.io.support.Resource
import org.yaml.snakeyaml.Yaml


/**
 * A repository that loads profiles from JAR files
 *
 * @author Graeme Rocher
 * @since 3.1
 */
@CompileStatic
abstract class AbstractJarProfileRepository implements ProfileRepository {

    protected final List<Profile> allProfiles = []
    protected final Map<String, Profile> profilesByName = [:]

    @Override
    Profile getProfile(String profileName) {
        return profilesByName[profileName]
    }

    List<Profile> getAllProfiles() {
        return allProfiles
    }

    @Override
    Resource getProfileDirectory(String profile) {
        return getProfile(profile)?.profileDir
    }

    @Override
    List<Profile> getProfileAndDependencies(Profile profile) {
        List<Profile> sortedProfiles = []
        Set<Profile> visitedProfiles = [] as Set
        visitTopologicalSort(profile, sortedProfiles, visitedProfiles)
        return sortedProfiles
    }

    protected void registerProfile(URL url, ClassLoader parent) {
        def classLoader = new URLClassLoader([url] as URL[], parent)
        def profileYml = classLoader.getResource("META-INF/grails-profile/profile.yml")
        if (profileYml != null) {
            def profile = new JarProfile(new ClassPathResource("META-INF/grails-profile/", classLoader), classLoader)
            profile.profileRepository = this
            allProfiles.add profile
            profilesByName[profile.name] = profile
        }
    }
    private void visitTopologicalSort(Profile profile, List<Profile> sortedProfiles, Set<Profile> visitedProfiles) {
        if(profile != null && !visitedProfiles.contains(profile)) {
            visitedProfiles.add(profile)
            profile.getExtends().each { Profile dependentProfile ->
                visitTopologicalSort(dependentProfile, sortedProfiles, visitedProfiles);
            }
            sortedProfiles.add(profile)
        }
    }

    static class JarProfile extends AbstractProfile {
        final ClassLoader classLoader
        private final List<Command> internalCommands = []
        private List<String> parentNames = []


        JarProfile(Resource profileDir, ClassLoader classLoader) {
            super(profileDir)
            this.classLoader = classLoader
            initialize()
        }

        @Override
        String getName() {
            super.name
        }

        private String initialize() {
            def profileYml = profileDir.createRelative("profile.yml")
            profileConfig = (Map<String, Object>) new Yaml().loadAs(profileYml.getInputStream(), Map)

            super.name = profileConfig.get("name")?.toString()

            def parents = profileConfig.get("extends")
            if(parents) {
                parentNames = parents.toString().split(',').collect() { String name -> name.trim() }
            }
            if(this.name == null) {
                throw new IllegalStateException("Profile name not set. Profile for path ${profileDir.URL} is invalid")
            }
            def map = new NavigableMap()
            map.merge(profileConfig)
            navigableConfig = map
            def commandsByName = profileConfig.get("commands")
            if(commandsByName instanceof Map) {
                def commandsMap = (Map) commandsByName
                for(clsName in  commandsMap.keySet()) {
                    def fileName = commandsMap[clsName].toString()
                    if(fileName.endsWith(".groovy")) {
                        GroovyScriptCommand cmd = (GroovyScriptCommand)classLoader.loadClass(clsName.toString()).newInstance()
                        cmd.profile = this
                        internalCommands.add cmd
                    }
                    else if(fileName.endsWith('.yml')) {
                        def yamlCommand = profileDir.createRelative("commands/$fileName")
                        if(yamlCommand.exists()) {
                            def data = new Yaml().loadAs(yamlCommand.getInputStream(), Map.class)
                            Command cmd = new DefaultMultiStepCommand(clsName.toString(), this, data)
                            Object minArguments = data?.minArguments
                            cmd.minArguments = minArguments instanceof Integer ? (Integer)minArguments : 1
                            internalCommands.add cmd
                        }

                    }
                }
            }
        }

        @Override
        Iterable<Profile> getExtends() {
            return parentNames.collect() { String name ->
                def parent = profileRepository.getProfile(name)
                if(parent == null) {
                    throw new IllegalStateException("Profile [$name] declares and invalid dependency on parent profile [$name]")
                }
                return parent
            }
        }

        @Override
        Iterable<Command> getCommands(ProjectContext context) {
            super.getCommands(context)
            for(cmd in internalCommands) {
                if(cmd instanceof ProjectContextAware) {
                    ((ProjectContextAware)cmd).setProjectContext(context)
                }
                commandsByName[cmd.name] = cmd
            }

            return commandsByName.values()
        }
    }
}
