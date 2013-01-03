package org.codehaus.groovy.grails.resolve.maven

import org.codehaus.groovy.grails.resolve.Dependency
import org.codehaus.groovy.grails.resolve.maven.aether.AetherDependencyManager
import org.codehaus.groovy.grails.resolve.maven.aether.config.GrailsAetherCoreDependencies
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

    void "Test simple dependency with exclusions resolve correctly"() {
        given: "A dependency manager instance"
            def dependencyManager = new AetherDependencyManager()
            dependencyManager.parseDependencies {
                repositories {
                    mavenCentral()
                    grailsCentral()
                }
                dependencies {
                    compile "org.grails:grails-core:2.2.0"
                }
            }

        when: "A dependency is resolved"
            def report = dependencyManager.resolveDependencies()

        then: "The transitive dependencies are resolved"
            report.files.find { it.name.contains('spring-core')}

        when: "When the dependency is resolved with an exclude rule"
            dependencyManager = new AetherDependencyManager()
            dependencyManager.parseDependencies {
                repositories {
                    mavenCentral()
                    grailsCentral()
                }
                dependencies {
                    compile "org.grails:grails-core:2.2.0", {
                        exclude 'spring-core'
                    }
                }
            }

            report = dependencyManager.resolveDependencies()

        then: "The transitive dependencies are excluded"
            !report.files.find { it.name.contains('spring-core')}
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

    void "Test dependencies inherited from framework"() {
        given: "A dependency manager instance"
        def dependencyManager = new AetherDependencyManager()
        dependencyManager.inheritedDependencies.global = new GrailsAetherCoreDependencies("2.2.0").createDeclaration()
        dependencyManager.parseDependencies {
            inherits("global")
            repositories {
                mavenRepo "http://repo.grails.org/grails/core"
            }
        }

        when:"The dependencies are resolved"
            def report = dependencyManager.resolveDependencies()
        then:"The resolve is successful"
            report != null
            report.files.find { it.name.contains 'ehcache' }

    }

    void "Test dependencies inherited from framework can be excluded"() {
        given: "A dependency manager instance"
        def dependencyManager = new AetherDependencyManager()
        dependencyManager.inheritedDependencies.global = new GrailsAetherCoreDependencies("2.2.0").createDeclaration()
        dependencyManager.parseDependencies {
            inherits("global") {
                excludes 'grails-docs', 'grails-gdoc-engine'
            }
            repositories {
                mavenRepo "http://repo.grails.org/grails/core"
            }
        }

        when:"The dependencies are resolved"
            def report = dependencyManager.resolveDependencies()
        then:"The resolve is successful"
            report != null
            !dependencyManager.allDependencies.find { Dependency d -> d.name == 'grails-docs'}
            !report.files.find { it.name.contains 'grails-docs' }

    }


    void "Test dependencies inherited vs dependencies not inherited"() {
        given: "A dependency manager instance"
        def dependencyManager = new AetherDependencyManager()
        dependencyManager.inheritedDependencies.global = new GrailsAetherCoreDependencies("2.2.0").createDeclaration()
        dependencyManager.parseDependencies {
            inherits("global") {
                excludes 'ehcache-core'
            }
            repositories {
                mavenRepo "http://repo.grails.org/grails/core"
            }
            dependencies {
                runtime 'mysql:mysql-connector-java:5.1.20'
            }
        }

        when:"The dependencies are resolved"
            def applicationDependencies = dependencyManager.applicationDependencies
            def allDependenices = dependencyManager.allDependencies
        then:"The resolve is successful"
            applicationDependencies.size() == 1
            allDependenices.size() > 1

    }
}
