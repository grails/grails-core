package org.codehaus.groovy.grails.resolve.maven.aether

import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.resolve.DependencyReport
import org.sonatype.aether.util.graph.PreorderNodeListGenerator

/**
 * @author Graeme Rocher
 */
@CompileStatic
class AetherDependencyReport implements DependencyReport{
    private PreorderNodeListGenerator resolveResult
    private String scope
    private Throwable error

    AetherDependencyReport(PreorderNodeListGenerator resolveResult, String scope) {
        this.resolveResult = resolveResult
        this.scope = scope
    }
    AetherDependencyReport(PreorderNodeListGenerator resolveResult, String scope, Throwable error) {
        this.resolveResult = resolveResult
        this.scope = scope
        this.error = error
    }

    String getClasspath() {
        resolveResult.getClassPath()
    }

    @Override
    List<File> getAllArtifacts() {
        getFiles().toList()
    }

    @Override
    List<File> getJarFiles() {
        return getFiles().toList()
    }

    @Override
    List<File> getPluginZips() {
        return null  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    String getScope() {
        return scope
    }

    @Override
    boolean hasError() {
        return error != null
    }

    @Override
    Throwable getResolveError() {
        return error
    }

    File[] getFiles() {
        resolveResult.getFiles()
    }


}
