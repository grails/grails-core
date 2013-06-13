/*
 * Copyright 2012 the original author or authors.
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

import org.codehaus.groovy.grails.resolve.Dependency
import org.codehaus.groovy.grails.resolve.ExcludeResolver
import org.codehaus.groovy.grails.resolve.reporting.GraphNode

/**
 * An exclude resolver for Aether
 *
 * @author Graeme Rocher
 * @since 2.3
 */
class AetherExcludeResolver implements ExcludeResolver {

    AetherDependencyManager dependencyManager

    AetherExcludeResolver(AetherDependencyManager dependencyManager) {
        this.dependencyManager = dependencyManager
    }

    @Override
    Map<Dependency, List<Dependency>> resolveExcludes() {

        Map<Dependency, List<Dependency>> excludeMap = [:]
        final applicationDependencies = dependencyManager.applicationDependencies
        AetherDependencyManager newDependencyManager = (AetherDependencyManager)dependencyManager.createCopy()
        for (Dependency d in applicationDependencies) {
            newDependencyManager.parseDependencies {
                delegate.dependencies {
                    compile "$d.group:$d.name:$d.version"
                }
            }
        }
        AetherGraphNode graphNode = newDependencyManager.resolveToGraphNode("compile")
        for (GraphNode n in graphNode.children) {
            List<Dependency> transitives = []
            excludeMap[n.dependency] = transitives
            for (GraphNode t in n.children) {
                transitives << t.dependency
            }
        }

        return excludeMap
    }
}
