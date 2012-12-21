package org.codehaus.groovy.grails.resolve.maven

import spock.lang.Specification

/**
 */
class AetherDependencyManagerSpec extends Specification {


    void "Test simple dependency resolve"() {
        given: "A dependency manager instance"
        def dependencyManager = new AetherDependencyManager()
        dependencyManager.parseDependencies {
            repositories {
                mavenCentral()
                grailsCentral()
            }
            dependencies {
                compile "org.grails:grails-bootstrap:2.2.0"
            }
        }

        when: "A dependency is resolved"
            def report = dependencyManager.resolveDependencies()

        then: "The dependencies are resolved"
            report.files.find { it.name.contains('grails-bootstrap')}
    }


    void "Test plugin dependency resolve"() {
        given: "A dependency manager instance"
        def dependencyManager = new AetherDependencyManager()
        dependencyManager.parseDependencies {
            repositories {
                mavenCentral()
                grailsCentral()
            }
            dependencies {
                compile "org.grails:grails-bootstrap:2.2.0"
            }
            plugins {
                compile ":feeds:1.5"
            }
        }

        when: "A dependency is resolved"
        def report = dependencyManager.resolveDependencies()

        then: "The dependencies are resolved"
        report.files.find { it.name.contains('feeds')}
    }
}
