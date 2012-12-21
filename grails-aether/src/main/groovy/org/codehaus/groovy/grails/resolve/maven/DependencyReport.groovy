package org.codehaus.groovy.grails.resolve.maven

import groovy.transform.CompileStatic
import org.sonatype.aether.util.graph.PreorderNodeListGenerator

/**
 * @author Graeme Rocher
 */
@CompileStatic
class DependencyReport {
    private PreorderNodeListGenerator resolveResult

    DependencyReport(PreorderNodeListGenerator resolveResult) {
        this.resolveResult = resolveResult
    }

    String getClasspath() {
        resolveResult.getClassPath()
    }

    File[] getFiles() {
        resolveResult.getFiles()
    }


}
