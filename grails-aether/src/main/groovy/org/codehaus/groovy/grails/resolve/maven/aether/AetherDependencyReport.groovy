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
    List<File> pluginZips = []
    List<File> jarFiles = []

    AetherDependencyReport(PreorderNodeListGenerator resolveResult, String scope) {
        this.resolveResult = resolveResult
        this.scope = scope
        this.jarFiles = findAndRemovePluginDependencies(resolveResult.files)
    }
    AetherDependencyReport(PreorderNodeListGenerator resolveResult, String scope, Throwable error) {
        this.resolveResult = resolveResult
        this.scope = scope
        this.error = error
        this.jarFiles = findAndRemovePluginDependencies(resolveResult.files)
    }


    private List<File> findAndRemovePluginDependencies(Collection<File> jarFiles) {
        jarFiles = jarFiles?.findAll { File it -> it != null} ?: new ArrayList<File>()
        def zips = jarFiles.findAll { File it -> it.name.endsWith(".zip") }
        for (z in zips) {
            if (!pluginZips.contains(z)) {
                pluginZips.add(z)
            }
        }
        jarFiles = jarFiles.findAll { File it -> !it.name.endsWith(".zip") }
        return jarFiles
    }
    String getClasspath() {
        resolveResult.getClassPath()
    }

    @Override
    List<File> getAllArtifacts() {
        getFiles().toList()
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
