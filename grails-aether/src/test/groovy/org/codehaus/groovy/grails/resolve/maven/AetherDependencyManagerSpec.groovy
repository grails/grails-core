package org.codehaus.groovy.grails.resolve.maven

import org.codehaus.groovy.grails.resolve.Dependency
import org.codehaus.groovy.grails.resolve.maven.aether.AetherDependencyManager
import org.codehaus.groovy.grails.resolve.maven.aether.config.GrailsAetherCoreDependencies
import org.sonatype.aether.repository.Authentication
import org.sonatype.aether.repository.RemoteRepository

import spock.lang.Specification

class AetherDependencyManagerSpec extends Specification {

    void "Test grails dependency transitive setting"() {
        given:"A dependency manager with a dependency that contains exclusions"
            def dependencyManager = new AetherDependencyManager()
            dependencyManager.parseDependencies {
                dependencies {
                    compile("org.apache.maven:maven-ant-tasks:2.1.3") {
                        transitive = false
                    }
                }
            }
        when:"The grails dependencies are obtained"
            Dependency dependency = dependencyManager.applicationDependencies.find { Dependency d -> d.name == "maven-ant-tasks" }

        then:"The exclusions are present"
            dependency != null
            dependency.transitive == false
    }

    void "Test grails dependency exclusions"() {
        given:"A dependency manager with a dependency that contains exclusions"
            def dependencyManager = new AetherDependencyManager()
            dependencyManager.parseDependencies {
                dependencies {
                    compile("org.apache.maven:maven-ant-tasks:2.1.3") {
                        excludes "commons-logging", "xml-apis", "groovy"
                    }
                }
            }
        when:"The grails dependencies are obtained"
            Dependency dependency = dependencyManager.applicationDependencies.find { Dependency d -> d.name == "maven-ant-tasks" }

        then:"The exclusions are present"
            dependency != null
            dependency.excludes.size() == 3
    }

    void "Test resolve with source and javadocs"() {
        given: "A dependency manager instance"
            def dependencyManager = new AetherDependencyManager()
            dependencyManager.includeJavadoc = true
            dependencyManager.includeSource = true
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
            def report = dependencyManager.resolve("compile")
            println report.files.size()
            println report.files
        then: "The dependencies are resolved"

            report.files.find { it.name.contains('grails-bootstrap-2.2.0')}
            report.files.find { it.name.contains('grails-bootstrap-2.2.0-sources')}
            report.files.find { it.name.contains('grails-bootstrap-2.2.0-javadoc')}
            report.files.find { it.name.contains('jline-1.0')}
            report.files.find { it.name.contains('jline-1.0-sources')}
            report.files.find { it.name.contains('jline-1.0-javadoc')}
    }

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
            def report = dependencyManager.resolve()

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
            def report = dependencyManager.resolve()

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

            report = dependencyManager.resolve()

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
        def report = dependencyManager.resolve()

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
            def report = dependencyManager.resolve()
        then:"The resolve is successful"
            report != null
            report.files.find { it.name.contains 'spring-tx' }
    }

    void "Test dependencies inherited from framework can be excluded"() {
        given: "A dependency manager instance"
        def dependencyManager = new AetherDependencyManager()
        dependencyManager.inheritedDependencies.global = new GrailsAetherCoreDependencies("2.2.0").createDeclaration()
        dependencyManager.parseDependencies {
            inherits("global") {
                excludes 'grails-plugin-servlets'
            }
            repositories {
                mavenRepo "http://repo.grails.org/grails/core"
            }
        }

        when:"The dependencies are resolved"
            def report = dependencyManager.resolve()
        then:"The resolve is successful"
            report != null
            !dependencyManager.allDependencies.find { Dependency d -> d.name == 'grails-plugin-servlets'}
            !report.files.find { it.name.contains 'grails-plugin-servlets' }
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

    void "Test configure authentication" () {
        given: "A dependency manager instance"
        def dependencyManager = new AetherDependencyManager()
        dependencyManager.inheritedDependencies.global = new GrailsAetherCoreDependencies("2.2.0").createDeclaration()

        when:"Credentials are specified"
            Authentication authentication
            RemoteRepository repository
            dependencyManager.parseDependencies {
                authentication = credentials {
                    username = "foo"
                    password = "bar"
                    id = "grailsCentral"
                }
                repositories {
                    repository  = mavenRepo( id:'grailsCentral', url:"http://repo.grails.org/grails/core" )
                }
            }
        then:"The credentials are correctly populated"
            authentication.username == "foo"
            authentication.password == "bar"
            repository.id == 'grailsCentral'
            repository.url == "http://repo.grails.org/grails/core"
            dependencyManager.session.authenticationSelector.getAuthentication(repository) != null
    }
}
