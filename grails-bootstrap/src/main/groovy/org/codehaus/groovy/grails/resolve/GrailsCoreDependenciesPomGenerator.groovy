package org.codehaus.groovy.grails.resolve

import groovy.xml.MarkupBuilder

/**
 *
 * Generates the 'grails-dependencies' POM file
 *
 * @author Graeme Rocher
 * @since 2.4.1
 */
class GrailsCoreDependenciesPomGenerator {

    String grailsVersion
    File targetFile

    GrailsCoreDependenciesPomGenerator(String grailsVersion, File targetFile) {
        this.grailsVersion = grailsVersion
        this.targetFile = targetFile
    }

    static void main(String[] args) {
        new GrailsCoreDependenciesPomGenerator(args[0], new File(args[1])).buildPom()
    }

    /**
     * Generates the POM file for the requested Grails version and dependency scope.
     */
    void buildPom() {
        GrailsCoreDependencies coreDependencies = new GrailsCoreDependencies(grailsVersion)

        def writer = new FileWriter(targetFile)
        try {
            writer.write('<?xml version="1.0" encoding="utf-8"?>\r\n')
            def xml = new MarkupBuilder(writer)
            xml.project('xmlns':'http://maven.apache.org/POM/4.0.0', 'xmlns:xsi':'http://www.w3.org/2001/XMLSchema-instance',
                    'xsi:schemaLocation':'http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd') {
                modelVersion { mkp.yield '4.0.0'}
                groupId { mkp.yield 'org.grails' }
                artifactId { mkp.yield "grails-dependencies" }
                packaging { mkp.yield 'pom' }
                version { mkp.yield grailsVersion }
                name { mkp.yield 'Grails Dependencies' }
                description { mkp.yield "POM file containing Grails dependencies." }
                url { mkp.yield 'http://grails.org' }
                dependencies {
                    buildDependencies((MarkupBuilder)delegate, "compile", coreDependencies.compileDependencies)
                    buildDependencies((MarkupBuilder)delegate, "runtime", coreDependencies.runtimeDependencies)
                    buildDependencies((MarkupBuilder)delegate, "provided", coreDependencies.providedDependencies)
                }
            }

            println("Successfully generated ${targetFile.getPath()}.")
        } finally {
            writer?.flush()
            writer?.close()
        }
    }

    void buildDependencies(MarkupBuilder builder, String mavenScope, Collection<Dependency> dependencies) {
        for(Dependency d in dependencies) {
            builder.dependency {
                groupId { mkp.yield d.group }
                artifactId { mkp.yield d.name }
                version { mkp.yield d.version }
                scope {

                    mkp.yield mavenScope
                }
                def excludes = d.excludes
                if(excludes) {
                    exclusions {
                        for(Dependency exc in excludes) {
                            exclusion {
                                groupId { mkp.yield exc.group }
                                artifactId { mkp.yield exc.name }
                            }
                        }
                    }
                }
            }
        }
    }
}
