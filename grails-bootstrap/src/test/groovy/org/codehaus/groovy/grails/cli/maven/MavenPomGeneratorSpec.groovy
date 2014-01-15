package org.codehaus.groovy.grails.cli.maven

import grails.util.BuildSettings
import grails.util.Metadata
import groovy.util.slurpersupport.GPathResult

import org.apache.ivy.core.cache.ResolutionCacheManager
import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.plugins.resolver.ChainResolver
import org.codehaus.groovy.grails.resolve.Dependency
import org.codehaus.groovy.grails.resolve.IvyDependencyManager
import org.codehaus.groovy.grails.resolve.IvyDependencyReport
import org.codehaus.groovy.grails.resolve.ResolvedArtifactReport

import spock.lang.Shared
import spock.lang.Specification

/**
 * @author Jonathan Pearlin
 */
class MavenPomGeneratorSpec extends Specification {

    @Shared
    File pomFile = new File('build/pom.xml')

    def setupSpec() {
        if(pomFile.exists()) {
            pomFile.delete()
        }
    }

    def cleanup() {
        if(pomFile.exists()) {
            pomFile.delete()
        }
    }

    def "test generating a pom file using Ivy dependency manager"() {
        setup:
            File pluginsDir = new File('build/plugins')
            pluginsDir.mkdirs()
            BuildSettings settings = Mock(BuildSettings) {
                createConfigSlurper() >> { Mock(ConfigSlurper) }
                getApplicationName() >> { 'grails-bootstrap-unit-tests' }
                getBaseDir() >> { new File('build') }
                getConfig() >> { Mock(ConfigObject) {
                        toProperties() >> { new Properties() }
                    }
                }
                getDependencyManager() >> { Mock(IvyDependencyManager) {
                        getApplicationDependencies(_) >> {
                            switch(it[0]) {
                                case "compile":
                                    [new Dependency('commons-lang', 'commons-lang', '3.0', false, 'slf4j-api', 'log4j:log4j')]
                                    break
                                case "runtime":
                                    [new Dependency('commons-logging', 'commons-logging', '1.1.1')]
                                    break
                                case "test":
                                    [new Dependency('org.spockframework', 'spock-core', '0.7-groovy-2.0')]
                                    break
                                case "provided":
                                    [new Dependency('javax.servlet', 'servlet-api', '2.5')]
                                    break
                                case "build":
                                    [new Dependency('org.grails', 'grails-bootstrap', '2.3.0')]
                                    break
                                default:
                                    []
                                    break
                            }
                        }
                        getIvySettings() >> { Mock(IvySettings) {
                                getResolutionCacheManager() >> { Mock(ResolutionCacheManager) {
                                        getResolvedIvyPropertiesInCache(_) >> { new File('build/ivy-props.xml') }
                                    }
                                }
                            }
                        }
                        getChainResolver() >> { Mock(ChainResolver) }
                        resolveDependency(_) >> { Mock(IvyDependencyReport) {
                                getResolvedArtifacts() >> { [new ResolvedArtifactReport(dependency:new Dependency('org.slf4j', 'slf4j-api', '1.7.0')), new ResolvedArtifactReport(dependency:new Dependency('log4j', 'log4j', '1.2.16')) ] }
                            }
                        }
                    }
                }
                getGrailsAppVersion() >> { '1.0.0' }
                getGrailsHome() >> { new File('..') }
                getGrailsVersion() >> { null }
                getMetadata() >> { Mock(Metadata) {
                        getApplicationVersion() >> { '1.0.0' }
                    }
                }
                getProjectPluginsDir() >> { pluginsDir }
            }
            MavenPomGenerator generator = new MavenPomGenerator(settings)
        when:
            generator.generate('org.grails.test')
        then:
            pomFile.exists() == true
            GPathResult pom = new XmlSlurper().parse(pomFile)
            (pom.project.dependencies.find { dependency ->
                dependency.groupId == 'commons-lang' &&
                dependency.artifactId == 'commons-lang' &&
                dependency.version == '3.0' &&
                dependency.scope == 'compile' &&
                (dependency.exclusions.find { exclusion -> exclusion.groupId == 'log4j' && exclusion.artifactId == 'log4j' } != null) == true &&
                (dependency.exclusions.find { exclusion -> exclusion.groupId == 'org.slf4j' && exclusion.artifactId == 'slf4j-api' } != null) == true
            } != null) == true
            (pom.project.dependencies.find { dependency ->
                dependency.groupId == 'commons-logging' &&
                dependency.artifactId == 'commons-logging' &&
                dependency.version == '1.1.1' &&
                dependency.scope == 'runtime' &&
                dependency.exclusions == null
            } != null) == true
            (pom.project.dependencies.find { dependency ->
                dependency.groupId == 'org.spockframework' &&
                dependency.artifactId == 'spock-core' &&
                dependency.version == '0.7-groovy-2.0' &&
                dependency.scope == 'test' &&
                dependency.exclusions == null
            } != null) == true
            (pom.project.dependencies.find { dependency ->
                dependency.groupId == 'javax.servlet' &&
                dependency.artifactId == 'servlet-api' &&
                dependency.version == '2.5' &&
                dependency.scope == 'provided' &&
                dependency.exclusions == null
            } != null) == true
            (pom.project.dependencies.find { dependency ->
                dependency.groupId == 'org.grails' &&
                dependency.artifactId == 'grails-bootstrap' &&
                dependency.version == '2.3.0' &&
                dependency.scope == 'provided' &&
                dependency.exclusions == null
            } != null) == true
    }
}
