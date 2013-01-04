package org.codehaus.groovy.grails.resolve.maven.aether.config

import grails.build.logging.GrailsConsole
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.sonatype.aether.graph.Dependency
import org.sonatype.aether.graph.Exclusion

/**
 *
 * Used to configure an individual dependency
 *
 * @author Graeme Rocher
 */
@CompileStatic
class DependencyConfiguration {

    @Delegate Dependency dependency

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
    void exclude(Map<String,String> exc) {
        List<Exclusion> exclusions = getExclusionList()
        exclusions << new Exclusion(exc.group, exc.name, exc.classifier, exc.extension);
        dependency = dependency.setExclusions(exclusions)
    }

    void exclude(String name) {
        List<Exclusion> exclusions = getExclusionList()
        if (name.contains(":")) {
            final result = name.split(":")
            String group = result[0]
            String id = result[1]
            exclusions << new Exclusion(group, id, "*",  "*")
        }
        else {
            exclusions << new Exclusion("*", name, "*",  "*")
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

    void setOtional(boolean b) {
        dependency = dependency.setOptional(b)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    void setProperty(String name, value) {
        dependency."$name" = value
    }
    void setChanging(boolean b) {
        GrailsConsole.getInstance().warn("Option [changing] on dependency [$dependency] not supported by Aether dependency manager")
    }

}
