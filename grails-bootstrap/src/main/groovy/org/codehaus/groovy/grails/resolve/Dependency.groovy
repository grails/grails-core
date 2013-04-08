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
