/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.resolve.maven

import org.codehaus.groovy.grails.resolve.Dependency
import org.codehaus.groovy.grails.resolve.DependencyReport
import org.codehaus.groovy.grails.resolve.maven.aether.AetherDependencyManager
import org.codehaus.groovy.grails.resolve.maven.aether.config.GrailsAetherCoreDependencies
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.repository.Authentication
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.resolution.DependencyResult

import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author Graeme Rocher
 * @since 2.3
 */
class AetherDependencyManagerSpec extends Specification {

    void "Test that dependency management can be applied to dependencies"() {
        given:"A dependency manager that applies management to transitive dependencies"
            def dependencyManager = new AetherDependencyManager()
            dependencyManager.parseDependencies {
                repositories {
                    mavenCentral()
                }
                management {
                    dependency "commons-logging:commons-logging:1.1.3"
                }
                dependencies {
                    compile 'org.springframework:spring-core:3.2.0.RELEASE'
                }
            }

        when:"The dependencies are resolved"
            def report = dependencyManager.resolve("compile")

        then:"The correct versions of transitives are resolved"
            report.jarFiles.any { File f -> f.name.contains('commons-logging-1.1.3')}

    }

    @Issue('GPRELEASE-59')
    void "Test that an excluded dependency that isn't available is excluded"() {
        given:"A dependency with an exclusion of an unavailable artifact"
            def dependencyManager = new AetherDependencyManager()
            dependencyManager.parseDependencies {
                repositories {
                    mavenCentral()
                }
                dependencies {
                    compile('com.octo.captcha:jcaptcha:1.0') {
                        excludes 'javax.servlet:servlet-api', 'com.jhlabs:imaging'
                    }
                }
            }

        when:"The dependency is resolved"
            def compileFiles = dependencyManager.resolve('compile').allArtifacts
        then:"The transitive dependency is excluded"
            !compileFiles.any { File f -> f.name.contains('imaging')}
    }

    @Issue('GRAILS-11055')
    void "Test that a transitive dependency excluded with the map syntax is actually excluded"() {
        given:"A dependency with an exclusion"
            def dependencyManager = new AetherDependencyManager()
            dependencyManager.parseDependencies {
                repositories {
                    grailsCentral()
                }
                plugins {
                    compile(":jasper:1.7.0") {
                        excludes([group: 'org.apache.poi', name: 'poi'])
                    }
                }
            }

        when:"The dependency is resolved"
            def report = dependencyManager.resolve('compile')
        then:"The transitive dependency is excluded"
            report.pluginZips.any { File f -> f.name.contains('jasper')}
            !report.jarFiles.any { File f -> f.name.contains('poi')}
    }

    @Issue('GRAILS-10638')
    void "Test that a dependency included in both compile and test scopes ends up in both scopes"() {
        given:"A dependency manager with a dependency that contains the dependency in both scopes"
            def dependencyManager = new AetherDependencyManager()
            dependencyManager.parseDependencies {
                repositories {
                    mavenCentral()
                }
                dependencies {
                    compile 'mysql:mysql-connector-java:5.1.24'
                    test 'mysql:mysql-connector-java:5.1.24'
                }
            }
        when:"The compile and test scopes are obtained"
            def compileFiles = dependencyManager.resolve('compile').allArtifacts
            def testFiles = dependencyManager.resolve('test').allArtifacts

        then:"The dependency is present in both scopes"
            compileFiles.size() == 1
            testFiles.size() == 1

    }


    @Issue('GRAILS-10638')
    void "Test that a transitive dependency included in both compile and test scopes ends up in both scopes 2"() {
        given:"A dependency manager with a dependency that contains exclusions"
        def dependencyManager = new AetherDependencyManager()
        dependencyManager.parseDependencies {
            repositories {
                mavenCentral()
            }
            dependencies {
                test 'org.grails:grails-test:2.3.2'
                test 'org.grails:grails-plugin-testing:2.3.2'

                compile 'org.grails:grails-test:2.3.2'
            }
        }
        when:"The compile and test scopes are obtained"
            def compileFiles = dependencyManager.resolve('compile').allArtifacts
            def testFiles = dependencyManager.resolve('test').allArtifacts

        then:"The grails-test jar is in both scopes"
            compileFiles.find { File f -> f.name.contains('grails-test')}
            testFiles.find { File f -> f.name.contains('grails-test')}

    }

    @Issue('GRAILS-10513')
    void "Test that plugin scopes are correct"() {
        given:"A dependency manager instance"
        def dependencyManager = new AetherDependencyManager()

        when:"Plugins are parsed in unique scopes"
            dependencyManager.parseDependencies {
                plugins {
                    build ":tomcat:7.0.42"
                    test ':cache:1.1.1'
                    // plugins needed at runtime but not for compilation
                    runtime ":hibernate:3.6.10.1" // or ":hibernate4:4.1.11.1"
                    provided ":database-migration:1.3.5"
                    optional ":jquery:1.10.2"
                }
            }


        then:"The scopes are correct"
            dependencyManager.getPluginDependencies('build').size() == 1
            dependencyManager.getPluginDependencies('build')[0].name == 'tomcat'
            dependencyManager.getPluginDependencies('test')[0].name == 'cache'
            dependencyManager.getPluginDependencies('test').size() == 1
            dependencyManager.getPluginDependencies('runtime')[0].name == 'hibernate'
            dependencyManager.getPluginDependencies('runtime').size() == 1
            dependencyManager.getPluginDependencies('provided')[0].name == 'database-migration'
            dependencyManager.getPluginDependencies('provided').size() == 1
            dependencyManager.getPluginDependencies('optional')[0].name == 'jquery'
            dependencyManager.getPluginDependencies('optional').size() == 1

    }
    @Issue('GRAILS-10414')
    void "Test Aether with map based dependency syntax"() {
        given:"A dependency manager instance"
        def dependencyManager = new AetherDependencyManager()
        dependencyManager.parseDependencies {
            repositories {
                mavenCentral()
            }
            dependencies {
                runtime group: 'mysql', name: 'mysql-connector-java', version: '5.1.24'
            }
        }

        when:"Plugin info is downloaded and no version specified"
            def result = dependencyManager.resolve()

        then:"The result is correct"
            !result.hasError()
    }

    void "Test download plugin info"() {
        given:"A dependency manager instance"
            def dependencyManager = new AetherDependencyManager()
            dependencyManager.parseDependencies {
                repositories {
                    grailsCentral()
                }
            }

        when:"Plugin info is downloaded and no version specified"
            def result = dependencyManager.downloadPluginInfo("feeds", null)

        then:"The result is correct"
            result != null
            result.@name.text() == 'feeds'


    }

    void "Test resolve agent"() {
        given:"A dependency manager specifies a custom agent"
            def dependencyManager = new AetherDependencyManager()
            dependencyManager.parseDependencies {
                repositories {
                    mavenRepo("https://repo.grails.org/grails/core")
                }
                dependencies {
                    agent "org.springframework:springloaded:1.2.1"
                }
            }

        when:"When the agent is resolved"
            def report = dependencyManager.resolveAgent()

        then:"The result is correct"
            report != null
            report.jarFiles.find { File f -> f.name.contains('springloaded')}

    }

    void "Test customize repository policy"() {
        given:"A dependency manager with a custom repository policy"
            def dependencyManager = new AetherDependencyManager()
            dependencyManager.parseDependencies {
                repositories {
                    mavenCentral {
                        updatePolicy "interval:1"
                        proxy "foo", 8080, auth(username:"bob", password:"builder")
                    }
                }
            }

        when:"The repository is obtained"
            def repo = dependencyManager.repositories.find { RemoteRepository rr -> rr.id == 'mavenCentral'}

        then:"It is configured correctly"
            repo.getPolicy(true).updatePolicy == 'interval:1'
            repo.proxy.host == 'foo'
            repo.proxy.port == 8080
            repo.proxy.authentication != null
    }

    void "Test grails dependency transitive setting"() {
        given:"A dependency manager with a dependency that has transitive resolves disabled"
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

    void "Test grails dependency exclusions with plugins"() {
        given:"A dependency manager with a dependency that contains exclusions"
        def dependencyManager = new AetherDependencyManager()
        dependencyManager.parseDependencies {
            dependencies {
                build("org.grails.plugins:tomcat:7.0.40") {
                    excludes "tomcat-embed-logging-juli"
                }
            }
        }
        when:"The grails dependencies are obtained"
            def files = dependencyManager.resolve("build").allArtifacts

        then:"The exclusions are present"
            !files.any { it.name.contains("tomcat-embed-logging-juli")}
    }

    @Ignore
    void "Test grails dependency with transitive disabled with plugins"() {
        given:"A dependency manager with a dependency that contains exclusions"
        def dependencyManager = new AetherDependencyManager()
        dependencyManager.parseDependencies {
            plugins {
                compile("org.grails.plugins:tomcat:7.0.40") {
                    transitive = false
                }
            }
        }
        when:"The grails dependencies are obtained"
            def files = dependencyManager.resolve().allArtifacts

        then:"The exclusions are present"
            files.size() == 1
    }

    @Unroll
    void "Test resolve with source and javadocs"() {
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
            dependencyManager.includeJavadoc = includeJavadoc
            dependencyManager.includeSource = includeSource
            def report = dependencyManager.resolve("compile")
            
        then: "The dependencies are resolved"
            dependencyResolved == report.files.any { it.name.contains('grails-bootstrap-2.2.0')}
            sourceResolved ==     report.files.any { it.name.contains('grails-bootstrap-2.2.0-sources')}
            javadocResolved ==    report.files.any { it.name.contains('grails-bootstrap-2.2.0-javadoc')}
            dependencyResolved == report.files.any { it.name.contains('jline-1.0')}
            sourceResolved ==     report.files.any { it.name.contains('jline-1.0-sources')}
            javadocResolved ==    report.files.any { it.name.contains('jline-1.0-javadoc')}
            
        where:
            includeJavadoc | includeSource | dependencyResolved | javadocResolved | sourceResolved
            false          | false         | true               | false           | false
            true           | false         | true               | true            | false
            false          | true          | true               | false           | true
            true           | true          | true               | true            | true
        
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

    @Issue('GRAILS-10671')
    void "Test exclude build dependency inherited from framework"() {
        given: "A dependency manager instance"
            def dependencyManager = new AetherDependencyManager()
            dependencyManager.inheritedDependencies.global = new GrailsAetherCoreDependencies("2.2.0").createDeclaration()
            dependencyManager.parseDependencies {
                inherits("global") {
                    excludes 'grails-docs'
                }
                repositories {
                    mavenRepo "https://repo.grails.org/grails/core"
                }
            }

        when:"The dependencies are resolved"
            def dependencies = dependencyManager.getApplicationDependencies('build')
        then:"The resolve is successful"
            !dependencies.find { it.name == 'grails-docs' }
    }

    void "Test dependencies inherited from framework"() {
        given: "A dependency manager instance"
        def dependencyManager = new AetherDependencyManager()
        dependencyManager.inheritedDependencies.global = new GrailsAetherCoreDependencies("2.2.0").createDeclaration()
        dependencyManager.parseDependencies {
            inherits("global")
            repositories {
                mavenRepo "https://repo.grails.org/grails/core"
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
                mavenRepo "https://repo.grails.org/grails/core"
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
                mavenRepo "https://repo.grails.org/grails/core"
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
                    repository  = mavenRepo( id:'grailsCentral', url:"https://repo.grails.org/grails/core" )
                }
            }
        then:"The credentials are correctly populated"
            authentication != null
            repository.id == 'grailsCentral'
            repository.url == "https://repo.grails.org/grails/core"
            repository.authentication == authentication
            dependencyManager.session.authenticationSelector.getAuthentication(repository) != null
    }

    void "Test configure authentication for a repo with no id" () {
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
                    id = "repo_grails_org_grails_core"
                }
                repositories {
                    repository  = mavenRepo("https://repo.grails.org/grails/core" )
                }
            }
        then:"The credentials are correctly populated"
            authentication != null
            repository.id == 'repo_grails_org_grails_core'
            repository.url == "https://repo.grails.org/grails/core"
            repository.authentication == authentication
            dependencyManager.session.authenticationSelector.getAuthentication(repository) != null
    }

    void "Test jcenter repository"() {
        given:
            def dependencyManager = new AetherDependencyManager()
            dependencyManager.inheritedDependencies.global = new GrailsAetherCoreDependencies("2.2.0").createDeclaration()

        when:
            dependencyManager.parseDependencies {
                repositories {
                    jcenter()
                }
            }

        then:
            dependencyManager.repositories.size() == 1

    }

    void "Test the resolution of a single dependency"() {
        given: "A dependency manager instance"
            def dependencyManager = new AetherDependencyManager()
            dependencyManager.repositorySystem = Mock(RepositorySystem) {
                resolveDependencies(_,_) >> { new DependencyResult(new DependencyRequest()) }
            }
        when: "A dependency is provided"
            org.codehaus.groovy.grails.resolve.Dependency dependency = new org.codehaus.groovy.grails.resolve.Dependency('org.slf4j', 'slf4j-api', '1.7.2')
            DependencyReport report = dependencyManager.resolveDependency(dependency)
        then: "The dependency is resolved"
            report != null
            !report.hasError()
    }

    void "Test duplicated repositories"() {
        given: "A dependency manager instance"
            def dependencyManager = new AetherDependencyManager()
            dependencyManager.parseDependencies {
                repositories {
                    mavenCentral()
                    mavenRepo "https://repo.grails.org/grails/core"
                }
            }

        when: "Another set of dependencies are parsed (like an inline plugin)"
            dependencyManager.parseDependencies {
                repositories {
                    mavenCentral()
                    mavenRepo "https://repo.grails.org/grails/core"
                }
            }

        then: "only the unique set of repositories remain"
            dependencyManager.repositories.size == 2

        and: "the declaration order is retained"
            dependencyManager.repositories[0].id == "mavenCentral"
            dependencyManager.repositories[1].url == "https://repo.grails.org/grails/core"
    }
}
