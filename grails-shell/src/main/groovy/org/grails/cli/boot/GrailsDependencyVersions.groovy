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
package org.grails.cli.boot

import groovy.grape.Grape
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.slurpersupport.GPathResult
import org.springframework.boot.dependency.tools.Dependencies
import org.springframework.boot.dependency.tools.Dependency


/**
 * Introduces dependency management based on a published BOM file
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class GrailsDependencyVersions implements Dependencies {

    protected Map<String, Dependency> groupAndArtifactToDependency = [:]
    protected Map<String, String> artifactToGroupAndArtifact = [:]


    GrailsDependencyVersions() {
        this([group: "org.grails", module: "grails-bom", version: GrailsDependencyVersions.package.implementationVersion, type: "pom"])
    }

    GrailsDependencyVersions(Map<String, String> bomCoords) {
        def grape = Grape.getInstance()
        grape.addResolver((Map<String,Object>)[name:"grailsCentral", root:"https://repo.grails.org/grails/core"])
        def results = grape.resolve(null, bomCoords)

        for(URI u in results) {

            def pom = new XmlSlurper().parseText(u.toURL().text)
            addDependencyManagement(pom)
        }
    }

    @CompileDynamic
    void addDependencyManagement(GPathResult pom) {
        pom.dependencyManagement.dependencies.dependency.each { dep ->
            addDependency(dep.groupId.text(), dep.artifactId.text(), dep.version.text())
        }
    }

    protected void addDependency(String group, String artifactId, String version) {
        def groupAndArtifactId = "$group:$artifactId".toString()
        artifactToGroupAndArtifact[artifactId] = groupAndArtifactId
        groupAndArtifactToDependency[groupAndArtifactId] = new Dependency(group, artifactId, version)
    }

    @Override
    Dependency find(String groupId, String artifactId) {
        return groupAndArtifactToDependency["$groupId:$artifactId".toString()]
    }

    @Override
    Dependency find(String artifactId) {
        def groupAndArtifact = artifactToGroupAndArtifact[artifactId]
        if(groupAndArtifact)
            return groupAndArtifactToDependency[groupAndArtifact]
    }

    @Override
    Iterator<Dependency> iterator() {
        return groupAndArtifactToDependency.values().iterator()
    }
}
