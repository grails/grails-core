/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.resolve

import org.apache.ivy.core.module.id.ModuleRevisionId

/**
 * Encapsulates information about the core dependencies of Grails.
 * 
 * This may eventually expand to expose information such as Spring version etc.
 * and be made available in the binding for user dependency declarations.
 */
class GrailsCoreDependencies {
    
    final String grailsVersion
    
    GrailsCoreDependencies(String grailsVersion) {
        this.grailsVersion = grailsVersion
    }

    /**
     * Returns a closure suitable for passing to a DependencyDefinitionParser that will configure
     * the necessary core dependencies for Grails.
     */
    Closure createDeclaration() {
        return {
            // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
            log "warn"
            repositories {
                grailsPlugins()
                grailsHome()
                // uncomment the below to enable remote dependency resolution
                // from public Maven repositories
                //mavenCentral()
                //mavenLocal()
                //mavenRepo "http://snapshots.repository.codehaus.org"
                //mavenRepo "http://repository.codehaus.org"
                //mavenRepo "http://download.java.net/maven/2/"
                //mavenRepo "http://repository.jboss.com/maven2/
            }
            
            dependencies {
                def compileTimeDependenciesMethod = dependencyManager.defaultDependenciesProvided ? 'provided' : 'compile'
                def runtimeDependenciesMethod = dependencyManager.defaultDependenciesProvided ? 'provided' : 'runtime'
                
                // dependencies needed by the Grails build system
                for(mrid in [ ModuleRevisionId.newInstance("org.tmatesoft.svnkit", "svnkit", "1.3.1"),
                              ModuleRevisionId.newInstance("org.apache.ant","ant","1.7.1"),
                              ModuleRevisionId.newInstance("org.apache.ant","ant-launcher","1.7.1"),
                              ModuleRevisionId.newInstance("org.apache.ant","ant-junit","1.7.1"),
                              ModuleRevisionId.newInstance("org.apache.ant","ant-nodeps","1.7.1"),
                              ModuleRevisionId.newInstance("org.apache.ant","ant-trax","1.7.1"),
                              ModuleRevisionId.newInstance("jline","jline","0.9.94"),
                              ModuleRevisionId.newInstance("org.fusesource.jansi","jansi","1.2.1"),
                              ModuleRevisionId.newInstance("xalan","serializer","2.7.1"),
                              ModuleRevisionId.newInstance("org.grails","grails-docs",grailsVersion),
                              ModuleRevisionId.newInstance("org.grails","grails-bootstrap", grailsVersion),
                              ModuleRevisionId.newInstance("org.grails","grails-scripts",grailsVersion),
                              ModuleRevisionId.newInstance("org.grails","grails-core",grailsVersion),
                              ModuleRevisionId.newInstance("org.grails","grails-resources",grailsVersion),
                              ModuleRevisionId.newInstance("org.grails","grails-web",grailsVersion),
                              ModuleRevisionId.newInstance("org.slf4j","slf4j-api","1.5.8"),
                              ModuleRevisionId.newInstance("org.slf4j","slf4j-log4j12","1.5.8"),
                              ModuleRevisionId.newInstance("org.springframework","org.springframework.test","3.0.3.RELEASE"),
                              ModuleRevisionId.newInstance("com.googlecode.concurrentlinkedhashmap","concurrentlinkedhashmap-lru","1.0_jdk5")] ) {
                        def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, false ,"build")
                        
                        dependencyManager.registerDependency("build", dependencyDescriptor)
                  }

                
                for(mrid in [ModuleRevisionId.newInstance("org.xhtmlrenderer","core-renderer","R8"),
                             ModuleRevisionId.newInstance("com.lowagie","itext","2.0.8"),
                             ModuleRevisionId.newInstance("radeox","radeox","1.0-b2")]) {
                   def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, false ,"docs")
                   
                   dependencyManager.registerDependency("docs", dependencyDescriptor)
                }

                // dependencies needed during development, but not for deployment
                for(mrid in [ModuleRevisionId.newInstance("javax.servlet","servlet-api","2.5"),
                             ModuleRevisionId.newInstance( "javax.servlet","jsp-api","2.1")]) {
                   def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, false ,"provided")
                    dependencyManager.registerDependency("provided", dependencyDescriptor)
                }

                // dependencies needed for compilation
                "${compileTimeDependenciesMethod}"("org.codehaus.groovy:groovy-all:1.8.0-beta-4-SNAPSHOT") {
                    excludes 'jline'
                }

                "${compileTimeDependenciesMethod}"("commons-beanutils:commons-beanutils:1.8.0", "commons-el:commons-el:1.0", "commons-validator:commons-validator:1.3.1") {
                    excludes "commons-logging", "xml-apis"
                }

                for(mrid in [   ModuleRevisionId.newInstance("org.coconut.forkjoin","jsr166y","070108"),
                                ModuleRevisionId.newInstance("org.codehaus.gpars","gpars","0.9"),
                                ModuleRevisionId.newInstance("aopalliance","aopalliance","1.0"),
                                ModuleRevisionId.newInstance("com.googlecode.concurrentlinkedhashmap","concurrentlinkedhashmap-lru","1.0_jdk5"),
                                ModuleRevisionId.newInstance("commons-codec","commons-codec","1.4"),
                                ModuleRevisionId.newInstance("commons-collections","commons-collections","3.2.1"),
                                ModuleRevisionId.newInstance("commons-io","commons-io","1.4"),
                                ModuleRevisionId.newInstance("commons-lang","commons-lang","2.4"),
                                ModuleRevisionId.newInstance("javax.transaction","jta","1.1"),
                                ModuleRevisionId.newInstance("org.hibernate","ejb3-persistence","1.0.2.GA"),
                                ModuleRevisionId.newInstance("opensymphony","sitemesh","2.4"),
                                ModuleRevisionId.newInstance("org.grails","grails-bootstrap","$grailsVersion"),
                                ModuleRevisionId.newInstance("org.grails","grails-core","$grailsVersion"),
                                ModuleRevisionId.newInstance("org.grails","grails-crud","$grailsVersion"),
                                ModuleRevisionId.newInstance("org.grails","grails-gorm","$grailsVersion"),
                                ModuleRevisionId.newInstance("org.grails","grails-resources","$grailsVersion"),
                                ModuleRevisionId.newInstance("org.grails","grails-spring","$grailsVersion"),
                                ModuleRevisionId.newInstance("org.grails","grails-web","$grailsVersion"),
                                ModuleRevisionId.newInstance("org.springframework","org.springframework.core","3.0.3.RELEASE"),
                                ModuleRevisionId.newInstance("org.springframework","org.springframework.aop","3.0.3.RELEASE"),
                                ModuleRevisionId.newInstance("org.springframework","org.springframework.aspects","3.0.3.RELEASE"),
                                ModuleRevisionId.newInstance("org.springframework","org.springframework.asm","3.0.3.RELEASE"),
                                ModuleRevisionId.newInstance("org.springframework","org.springframework.beans","3.0.3.RELEASE"),
                                ModuleRevisionId.newInstance("org.springframework","org.springframework.context","3.0.3.RELEASE"),
                                ModuleRevisionId.newInstance("org.springframework","org.springframework.context.support","3.0.3.RELEASE"),
                                ModuleRevisionId.newInstance("org.springframework","org.springframework.expression","3.0.3.RELEASE"),
                                ModuleRevisionId.newInstance("org.springframework","org.springframework.instrument","3.0.3.RELEASE"),
                                ModuleRevisionId.newInstance("org.springframework","org.springframework.jdbc","3.0.3.RELEASE"),
                                ModuleRevisionId.newInstance("org.springframework","org.springframework.jms","3.0.3.RELEASE"),
                                ModuleRevisionId.newInstance("org.springframework","org.springframework.orm","3.0.3.RELEASE"),
                                ModuleRevisionId.newInstance("org.springframework","org.springframework.oxm","3.0.3.RELEASE"),
                                ModuleRevisionId.newInstance("org.springframework","org.springframework.transaction","3.0.3.RELEASE"),
                                ModuleRevisionId.newInstance("org.springframework","org.springframework.web","3.0.3.RELEASE"),
                                ModuleRevisionId.newInstance("org.springframework","org.springframework.web.servlet","3.0.3.RELEASE"),
                                ModuleRevisionId.newInstance("org.slf4j","slf4j-api","1.5.8")] ) {
                           def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, false ,compileTimeDependenciesMethod)
                           dependencyManager.registerDependency(compileTimeDependenciesMethod, dependencyDescriptor)
                    }


                    // dependencies needed for running tests
                    for(mrid in [   ModuleRevisionId.newInstance("junit","junit","4.8.1"),
                                    ModuleRevisionId.newInstance("org.grails","grails-test","$grailsVersion"),
                                    ModuleRevisionId.newInstance("org.springframework","org.springframework.test","3.0.3.RELEASE")] ) {
                           def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, false ,"test")
                           dependencyManager.registerDependency("test", dependencyDescriptor)
                    }

                    // dependencies needed at runtime only
                    for( mrid in [      ModuleRevisionId.newInstance("org.aspectj","aspectjweaver","1.6.8"),
                                        ModuleRevisionId.newInstance("org.aspectj","aspectjrt","1.6.8"),
                                        ModuleRevisionId.newInstance("cglib","cglib-nodep","2.1_3"),
                                        ModuleRevisionId.newInstance("commons-fileupload","commons-fileupload","1.2.1"),
                                        ModuleRevisionId.newInstance("oro","oro","2.0.8"),
                                        ModuleRevisionId.newInstance("javax.servlet","jstl","1.1.2"),
                                        // data source
                                        ModuleRevisionId.newInstance("commons-dbcp","commons-dbcp","1.3"),
                                        ModuleRevisionId.newInstance("commons-pool","commons-pool","1.5.5"),
                                        ModuleRevisionId.newInstance("hsqldb","hsqldb","1.8.0.10"),
                                        ModuleRevisionId.newInstance("com.h2database","h2","1.2.144"),
                                        // JSP support
                                        ModuleRevisionId.newInstance("apache-taglibs","standard","1.1.2"),
                                        ModuleRevisionId.newInstance("xpp3","xpp3_min","1.1.3.4.O") ] ) {
                           def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, false ,runtimeDependenciesMethod)
                           dependencyManager.registerDependency(runtimeDependenciesMethod, dependencyDescriptor)
                    }

                    // caching
                    "${runtimeDependenciesMethod}" ("net.sf.ehcache:ehcache-core:1.7.1") {
                        excludes 'jms', 'commons-logging', 'servlet-api'
                    }

                    // logging
                    "${runtimeDependenciesMethod}"("log4j:log4j:1.2.16",
                            "org.slf4j:jcl-over-slf4j:1.5.8",
                            "org.slf4j:jul-to-slf4j:1.5.8",
                            "org.slf4j:slf4j-log4j12:1.5.8" ) {
                        excludes 'mail', 'jms', 'jmxtools', 'jmxri'
                    }
            }
        }
    }
    
}