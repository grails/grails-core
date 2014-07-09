package org.grails.dependency.resolution

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
        println "Generating POM for $grailsVersion to $targetFile"
        targetFile.getParentFile().mkdirs()
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

                licenses {
                    license {
                        name { mkp.yield( "The Apache Software License, Version 2.0") }
                        url { mkp.yield( "http://www.apache.org/licenses/LICENSE-2.0.txt") }
                        distribution { mkp.yield( "repo") }
                    }
                }
                scm {
                    url { mkp.yield( "http://github.com/grails/grails-core") }
                    developerConnection { mkp.yield( "git@github.com:grails/grails-core.git") }
                    connection { mkp.yield( "scm:git:git@github.com:grails/grails-core.git") }
                }

                parent {
                    groupId { mkp.yield( "org.sonatype.oss") }
                    artifactId { mkp.yield( "oss-parent") }
                    version { mkp.yield( "7") }
                }

                dependencies {
                    buildDependencies((MarkupBuilder)delegate, "compile", coreDependencies.compileDependencies)
                    buildDependencies((MarkupBuilder)delegate, "runtime", coreDependencies.runtimeDependencies)
                    buildDependencies((MarkupBuilder)delegate, "provided", coreDependencies.providedDependencies)
                    buildDependencies((MarkupBuilder)delegate, "test", coreDependencies.testDependencies)
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
