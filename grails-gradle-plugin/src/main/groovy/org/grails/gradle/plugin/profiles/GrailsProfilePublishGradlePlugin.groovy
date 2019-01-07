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
package org.grails.gradle.plugin.profiles

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.SelfResolvingDependency
import org.gradle.api.artifacts.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.grails.gradle.plugin.publishing.GrailsCentralPublishGradlePlugin


/**
 * A plugin for publishing profiles
 *
 * @author Graeme Rocher
 * @since 3.1
 */
@CompileStatic
class GrailsProfilePublishGradlePlugin extends GrailsCentralPublishGradlePlugin {

    @Override
    protected String getDefaultGrailsCentralReleaseRepo() {
        "https://repo.grails.org/grails/libs-releases-local"
    }

    @Override
    protected String getDefaultGrailsCentralSnapshotRepo() {
        "https://repo.grails.org/grails/libs-snapshots-local"
    }

    @Override
    protected Map<String, String> getDefaultExtraArtifact(Project project) {
        [source: "${project.buildDir}/classes/profile/META-INF/grails-profile/profile.yml".toString(),
         classifier: defaultClassifier,
         extension : 'yml']
    }

    @Override
    protected String getDefaultClassifier() {
        'profile'
    }

    @Override
    protected String getDefaultRepo() {
        'profiles'
    }

    @Override
    protected void doAddArtefact(Project project, MavenPublication publication) {
        publication.artifact(project.tasks.findByName("jar"))
        publication.pom(new Action<org.gradle.api.publish.maven.MavenPom>() {
            @Override
            void execute(org.gradle.api.publish.maven.MavenPom mavenPom) {
                mavenPom.withXml(new Action<XmlProvider>() {
                    @Override
                    void execute(XmlProvider xml) {
                        Node dependenciesNode = xml.asNode().appendNode('dependencies')

                        DependencySet dependencySet = project.configurations[GrailsProfileGradlePlugin.RUNTIME_CONFIGURATION].allDependencies

                        for (Dependency dependency : dependencySet) {
                            if (! (dependency instanceof SelfResolvingDependency)) {
                                Node dependencyNode = dependenciesNode.appendNode('dependency')
                                dependencyNode.appendNode('groupId', dependency.group)
                                dependencyNode.appendNode('artifactId', dependency.name)
                                dependencyNode.appendNode('version', dependency.version)
                                dependencyNode.appendNode('scope', GrailsProfileGradlePlugin.RUNTIME_CONFIGURATION)
                            }
                        }
                    }
                })
            }
        })
    }
}
