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
package org.codehaus.groovy.grails.resolve.maven.aether.config

import grails.build.logging.GrailsConsole
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import org.sonatype.aether.graph.Dependency
import org.sonatype.aether.graph.Exclusion

/**
 * Used to configure an individual dependency
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class DependencyConfiguration {

    public static final String WILD_CARD = "*"
    @Delegate Dependency dependency
    private boolean transitive = true
    boolean exported = true

    DependencyConfiguration(Dependency dependency) {
        this.dependency = dependency
    }

    void excludes(Object...excludes) {
        for(o in excludes) {
            if (o instanceof CharSequence) {
                exclude( o.toString() )
            }
            else if (o instanceof Map) {
                exclude((Map)o)
            }
        }
    }

    boolean getTransitive() {
        return transitive
    }

    void setTransitive(boolean transitive) {
        List<Exclusion> exclusions = getExclusionList()
        exclusions << new Exclusion(WILD_CARD, WILD_CARD, WILD_CARD, WILD_CARD)
        dependency = dependency.setExclusions(exclusions)

        this.transitive = transitive
    }

    void exclude(Map<String,String> exc) {
        List<Exclusion> exclusions = getExclusionList()
        exclusions << new Exclusion(exc.group, exc.name, exc.classifier, exc.extension)
        dependency = dependency.setExclusions(exclusions)
    }

    void exclude(String name) {
        List<Exclusion> exclusions = getExclusionList()
        if (name.contains(":")) {
            final result = name.split(":")
            String group = result[0]
            String id = result[1]
            exclusions << new Exclusion(group, id, WILD_CARD, WILD_CARD)
        }
        else {
            exclusions << new Exclusion(WILD_CARD, name, WILD_CARD, WILD_CARD)
        }
        dependency = dependency.setExclusions(exclusions)
    }

    void exclude(Exclusion exclusion) {
        final list = getExclusionList()
        list << exclusion
        dependency = dependency.setExclusions(list)
    }

    protected List<Exclusion> getExclusionList() {
        !dependency.exclusions ? [] : new ArrayList<Exclusion>(dependency.exclusions)
    }

    void setScope(String s) {
        dependency = dependency.setScope(s)
    }

    void setExport(boolean e) {
        exported = e
    }

    void setOtional(boolean b) {
        dependency = dependency.setOptional(b)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    void propertyMissing(String name, value) {
        dependency."$name" = value
    }

    void setChanging(boolean b) {
        GrailsConsole.getInstance().warn("Option [changing] on dependency [$dependency] not supported by Aether dependency manager")
    }
}
