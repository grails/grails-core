/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.resolve

import groovy.transform.Canonical
import groovy.transform.CompileStatic

/**
 * Represents a dependency independent of any dependency resolution engine (ivy or aether)
 *
 * @author Graeme Rocher
 */
@CompileStatic
@Canonical(includes = ['group', 'name', 'version'])
class Dependency {

    public static final String WILDCARD = "*"

    String group
    String name
    String version
    String classifier
    boolean transitive = true
    boolean inherited = false
    boolean exported = true
    String extension = null

    Dependency(String group, String name, String version, String...exc) {
        this.group = group
        this.name = name
        this.version = version
        for (e in exc) {
            exclude(e)
        }
    }

    Dependency(String group, String name, String version, boolean inherited, String...exc) {
        this.group = group
        this.name = name
        this.version = version
        this.inherited = inherited
        for (e in exc) {
            exclude(e)
        }
    }

    List<Dependency> excludes = []

    /**
     * Array of tokens for the group, name and version
     */
    String[] getTokens() {
        [group, name, version] as String[]
    }

    String[] getExcludeArray() {
        excludes as String[]
    }

    /**
     * Dependency pattern
     */
    String getPattern() {
        "${group}:${name}:${version}"
    }

    @Override
    String toString() {
        return getPattern()
    }

    void exclude(String name) {
        if (name.contains(":")) {
            def result = name.split(":")
            excludes << new Dependency(result[0], result[1], WILDCARD)
        }
        else {
            excludes << new Dependency(WILDCARD, name, WILDCARD)
        }
    }
    void exclude(String group, String name) {
        excludes << new Dependency(group, name, WILDCARD)
    }
}
