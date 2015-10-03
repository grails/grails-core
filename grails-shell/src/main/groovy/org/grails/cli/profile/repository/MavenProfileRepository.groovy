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

import grails.util.BuildSettings
import groovy.transform.CompileStatic
import org.eclipse.aether.artifact.DefaultArtifact
import org.grails.cli.profile.Profile
import org.springframework.boot.cli.compiler.grape.AetherGrapeEngine
import org.springframework.boot.cli.compiler.grape.AetherGrapeEngineFactory
import org.springframework.boot.cli.compiler.grape.DependencyResolutionContext
import org.springframework.boot.cli.compiler.grape.RepositoryConfiguration


/**
 *  Resolves profiles from a configured list of repositories using Aether
 *
 * @author Graeme Rocher
 * @since 3.1
 */
@CompileStatic
class MavenProfileRepository extends AbstractJarProfileRepository {

    List<RepositoryConfiguration> repositoryConfigurations
    AetherGrapeEngine grapeEngine
    GroovyClassLoader classLoader

    MavenProfileRepository(List<RepositoryConfiguration> repositoryConfigurations) {
        this.repositoryConfigurations = repositoryConfigurations
        classLoader = new GroovyClassLoader(Thread.currentThread().contextClassLoader)
        this.grapeEngine = AetherGrapeEngineFactory.create(classLoader, repositoryConfigurations, new DependencyResolutionContext())
    }

    MavenProfileRepository() {
        this.repositoryConfigurations = [new RepositoryConfiguration("grailsCentral", new URI("https://repo.grails.org/grails/core"), true)]
        classLoader = new GroovyClassLoader(Thread.currentThread().contextClassLoader)
        this.grapeEngine = AetherGrapeEngineFactory.create(classLoader, repositoryConfigurations, new DependencyResolutionContext())
    }

    @Override
    Profile getProfile(String profileName) {
        if(!profilesByName.containsKey(profileName)) {
            resolveProfile(profileName)
        }
        return super.getProfile(profileName)
    }

    protected void resolveProfile(String profileName) {
        if (!profileName.contains(':')) {
            profileName = "org.grails.profiles:$profileName:${BuildSettings.package.implementationVersion}"
        }
        def art = new DefaultArtifact(profileName)
        grapeEngine.grab(group: art.groupId, module: art.artifactId, version: art.version)

        def urls = classLoader.getURLs()
        for (URL url in urls) {
            registerProfile(url, new URLClassLoader([url] as URL[], Thread.currentThread().contextClassLoader))
        }
    }

    @Override
    List<Profile> getAllProfiles() {
        return super.getAllProfiles()
    }
}
