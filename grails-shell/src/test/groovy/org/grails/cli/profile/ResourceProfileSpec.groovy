package org.grails.cli.profile

import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.graph.Dependency
import org.grails.io.support.Resource
import spock.lang.Specification

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

/**
 * @author graemerocher
 */
class ResourceProfileSpec extends Specification {

    void "Test resource version"() {
        given:"A resource profile"
        def mockResource = Mock(Resource)

        def mockProfileYml = Mock(Resource)
        mockProfileYml.getInputStream() >> new ByteArrayInputStream(getYaml())
        mockResource.createRelative("profile.yml") >> mockProfileYml
        mockResource.getURL() >> new URL("file:/path/to/my-profile-1.0.1.jar!profile.yml")
        def profileRepository = Mock(ProfileRepository)
        def profile = new ResourceProfile(profileRepository, "web", mockResource)
        profileRepository.getProfile("web" ) >> profile

        def baseProfile = Mock(Profile)
        baseProfile.getDependencies() >> [ new Dependency(new DefaultArtifact("foo:bar:2.0"), "test")]
        baseProfile.getBuildPlugins() >> [ "foo-plug"]
        profileRepository.getProfile("base" ) >> baseProfile
        expect:
        profile.version == '1.0.1'

    }

    void "Test dependencies"() {
        given:"A resource profile"

        def mockResource = Mock(Resource)
        def mockProfileYml = Mock(Resource)
        mockProfileYml.getInputStream() >> new ByteArrayInputStream(getYaml())
        mockResource.createRelative("profile.yml") >> mockProfileYml

        def profileRepository = Mock(ProfileRepository)

        def profile = new ResourceProfile(profileRepository, "web", mockResource)
        profileRepository.getProfile("web", true) >> profile

        def baseProfile = Mock(Profile)
        baseProfile.getDependencies() >> [ new Dependency(new DefaultArtifact("foo:bar:2.0"), "test")]
        baseProfile.getBuildPlugins() >> [ "foo-plug"]
        profileRepository.getProfile("base", true) >> baseProfile


        when:"The dependencies are accessed"
        def deps = profile.dependencies
        def plugins = profile.buildPlugins

        then:"They are correct"
        plugins.size() == 2
        deps.size() == 2
        plugins == ['foo-plug', 'bar']
        deps[1].scope == 'compile'
        deps[1].artifact.groupId == 'org.grails'
        deps[1].artifact.artifactId == 'grails-core'
        deps[1].artifact.version == '3.1.0'
        deps[0].scope == 'test'
        deps[0].artifact.groupId == 'foo'
        deps[0].artifact.artifactId == 'bar'
        deps[0].artifact.version == '2.0'
    }


    void "Test dependency exclusions"() {
        given:"A resource profile"

        def mockResource = Mock(Resource)
        def mockProfileYml = Mock(Resource)
        mockProfileYml.getInputStream() >> new ByteArrayInputStream(getExcludesYaml())
        mockResource.createRelative("profile.yml") >> mockProfileYml

        def profileRepository = Mock(ProfileRepository)

        def profile = new ResourceProfile(profileRepository, "web", mockResource)
        profileRepository.getProfile("web", true) >> profile

        def baseProfile = Mock(Profile)
        baseProfile.getDependencies() >> [ new Dependency(new DefaultArtifact("foo:bar:2.0"), "test")]
        baseProfile.getBuildPlugins() >> [ "foo-plug"]
        profileRepository.getProfile("base", true) >> baseProfile


        when:"The dependencies are accessed"
        def deps = profile.dependencies
        def plugins = profile.buildPlugins

        then:"They are correct"
        deps.size() == 1
        plugins == ['bar']

        deps.size() == 1
        deps[0].scope == 'compile'
        deps[0].artifact.groupId == 'org.grails'
        deps[0].artifact.artifactId == 'grails-core'
        deps[0].artifact.version == '3.1.0'
    }

    byte[] getYaml() {
        """
name: web
extends: base
build:
    plugins:
        - bar
dependencies:
    compile:
        - org.grails:grails-core:3.1.0
""".bytes
    }

    byte[] getExcludesYaml() {
        """
name: web
extends: base
build:
    plugins:
        - bar
    excludes:
        - foo-plug
dependencies:
    excludes:
        - foo:bar:*
    compile:
        - org.grails:grails-core:3.1.0
""".bytes
    }
}
