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

    public static final RepositoryConfiguration DEFAULT_REPO = new RepositoryConfiguration("grailsCentral", new URI("https://repo.grails.org/grails/core"), true)

    List<RepositoryConfiguration> repositoryConfigurations
    AetherGrapeEngine grapeEngine
    GroovyClassLoader classLoader
    private boolean resolved = false

    MavenProfileRepository(List<RepositoryConfiguration> repositoryConfigurations) {
        this.repositoryConfigurations = repositoryConfigurations
        classLoader = new GroovyClassLoader(Thread.currentThread().contextClassLoader)
        this.grapeEngine = AetherGrapeEngineFactory.create(classLoader, repositoryConfigurations, new DependencyResolutionContext())
    }

    MavenProfileRepository() {
        this.repositoryConfigurations = [DEFAULT_REPO]
        classLoader = new GroovyClassLoader(Thread.currentThread().contextClassLoader)
        this.grapeEngine = AetherGrapeEngineFactory.create(classLoader, repositoryConfigurations, new DependencyResolutionContext())
    }

    @Override
    Profile getProfile(String profileName) {
        if(!resolved && !profilesByName.containsKey(profileName)) {
            resolveProfile(profileName)
        }
        return super.getProfile(profileName)
    }

    protected void resolveProfile(String profileName) {
        if (!profileName.contains(':')) {
            profileName = "org.grails.profiles:$profileName:LATEST"
        }
        def art = new DefaultArtifact(profileName)
        grapeEngine.grab(group: art.groupId ?: 'org.grails.profiles', module: art.artifactId, version: art.version ?: 'LATEST')

        processUrls()
    }

    protected void processUrls() {
        def urls = classLoader.getURLs()
        for (URL url in urls) {
            registerProfile(url, new URLClassLoader([url] as URL[], Thread.currentThread().contextClassLoader))
        }
    }

    @Override
    List<Profile> getAllProfiles() {


        if(!resolved) {
            List<String> profileNames = []
            for(repo in repositoryConfigurations) {

                def baseUri = repo.uri
                def text = new URL("${baseUri}/org/grails/profiles").text
                text.eachMatch(/<a href="([a-z-]+)\/">.+/) { List<String> it ->
                    profileNames.add it[1]
                }
            }

            for(name in profileNames) {
                grapeEngine.grab(group: 'org.grails.profiles', module: name, version: 'LATEST')
            }

            processUrls()
            resolved = true
        }
        return super.getAllProfiles()
    }
}
