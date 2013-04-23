/*
 * Copyright 2013 the original author or authors.
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
package org.codehaus.groovy.grails.resolve.maven.aether

import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.resolve.Dependency
import org.codehaus.groovy.grails.resolve.reporting.GraphNode
import org.sonatype.aether.artifact.Artifact
import org.sonatype.aether.graph.DependencyNode
import org.sonatype.aether.resolution.DependencyResult

/**
 * Adapts a Aether dependency graph into the Grails graph node API for reporting
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class AetherGraphNode extends GraphNode {

    AetherGraphNode(DependencyResult dependencyResult, List<Artifact> unresolved) {
        super(new Dependency("org.grails.internal", "root", "1.0")) // version numbers not relevant for root node / dummy object
        createGraph(this, dependencyResult.root.children, unresolved)
    }

    void createGraph(GraphNode current, List<DependencyNode> nodes, List<Artifact> unresolved) {
        for(DependencyNode node in nodes) {
            def dependency = node.dependency
            if (!dependency) {
                continue
            }

            def artifact = dependency.artifact
            final notResolved = unresolved.contains(artifact)
            if (artifact.file || notResolved) {
                def graphNode = new GraphNode(new Dependency(artifact.groupId, artifact.artifactId, artifact.version))
                graphNode.resolved = !notResolved
                current.children << graphNode
                createGraph(graphNode, node.children, unresolved)
            }
        }
    }
}
