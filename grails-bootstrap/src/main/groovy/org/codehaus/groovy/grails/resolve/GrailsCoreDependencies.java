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
package org.codehaus.groovy.grails.resolve;

import groovy.lang.Closure;

import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.codehaus.groovy.grails.plugins.GrailsVersionUtils;
import org.codehaus.groovy.grails.resolve.config.DependencyConfigurationConfigurer;
import org.codehaus.groovy.grails.resolve.config.JarDependenciesConfigurer;
import org.codehaus.groovy.grails.resolve.config.RepositoriesConfigurer;

/**
 * Encapsulates information about the core dependencies of Grails.
 */
public class GrailsCoreDependencies {

    public final String grailsVersion;
    public final String servletVersion;
    public boolean java5compatible;

    private final String springVersion = "3.1.2.RELEASE";

    public GrailsCoreDependencies(String grailsVersion) {
        this.grailsVersion = grailsVersion;
        this.servletVersion = "2.5";
    }

    public GrailsCoreDependencies(String grailsVersion, String servletVersion) {
        this.grailsVersion = grailsVersion;
        this.servletVersion = servletVersion != null ? servletVersion : "2.5";
    }

    private void registerDependencies(IvyDependencyManager dependencyManager, String scope, ModuleRevisionId[] dependencies, boolean transitive) {
        for (ModuleRevisionId mrid : dependencies) {
            EnhancedDefaultDependencyDescriptor descriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, false, scope);
            descriptor.setInherited(true);
            descriptor.setTransitive(transitive);
            dependencyManager.registerDependency(scope, descriptor);
        }
    }

    private void registerDependencies(IvyDependencyManager dependencyManager, String scope, ModuleRevisionId[] dependencies, String... excludes) {
        for (ModuleRevisionId mrid : dependencies) {
            EnhancedDefaultDependencyDescriptor descriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, false, scope);
            descriptor.setInherited(true);
            if (excludes != null) {
                for (String exclude : excludes) {
                    descriptor.exclude(exclude);
                }
            }
            dependencyManager.registerDependency(scope, descriptor);
        }
    }

    private EnhancedDefaultDependencyDescriptor registerDependency(IvyDependencyManager dependencyManager, String scope, ModuleRevisionId mrid) {
        EnhancedDefaultDependencyDescriptor descriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, false, scope);
        descriptor.setInherited(true);
        dependencyManager.registerDependency(scope, descriptor);
        return descriptor;
    }

    /**
     * Returns a closure suitable for passing to a DependencyDefinitionParser that will configure
     * the necessary core dependencies for Grails.
     * 
     * This method is used internally and should not be called in user code.
     */
    @SuppressWarnings({ "serial", "rawtypes" })
    public Closure createDeclaration() {
        return new Closure(this, this) {
            @SuppressWarnings("unused")
            public Object doCall() {
                DependencyConfigurationConfigurer rootDelegate = (DependencyConfigurationConfigurer)getDelegate();

                rootDelegate.log("warn");

                // Repositories
                rootDelegate.repositories(new Closure(this, GrailsCoreDependencies.this) {
                    public Object doCall() {
                        RepositoriesConfigurer repositoriesDelegate = (RepositoriesConfigurer)getDelegate();

                        repositoriesDelegate.grailsPlugins();
                        repositoriesDelegate.grailsHome();

                        return null;
                    }
                });
                // Dependencies

                rootDelegate.dependencies(new Closure(this, GrailsCoreDependencies.this) {
                    public Object doCall() {
                        JarDependenciesConfigurer dependenciesDelegate = (JarDependenciesConfigurer)getDelegate();
                        IvyDependencyManager dependencyManager = dependenciesDelegate.getDependencyManager();

                        boolean defaultDependenciesProvided = dependencyManager.getDefaultDependenciesProvided();
                        String compileTimeDependenciesMethod = defaultDependenciesProvided ? "provided" : "compile";
                        String runtimeDependenciesMethod = defaultDependenciesProvided ? "provided" : "runtime";

                        // dependencies needed by the Grails build system

                        String antVersion = "1.8.2";
                        String slf4jVersion = "1.6.2";
                        String junitVersion = "4.10";
                        ModuleRevisionId[] buildDependencies = {
                            ModuleRevisionId.newInstance("org.springframework.uaa", "org.springframework.uaa.client", "1.0.1.RELEASE"),
                            ModuleRevisionId.newInstance("com.google.protobuf", "protobuf-java", "2.4.1"),
                            ModuleRevisionId.newInstance("com.googlecode.json-simple", "json-simple", "1.1"),
                            ModuleRevisionId.newInstance("org.bouncycastle", "bcpg-jdk15", "1.45"),
                            ModuleRevisionId.newInstance("org.bouncycastle", "bcprov-jdk15", "1.45"),
                            ModuleRevisionId.newInstance("jline", "jline", "1.0"),
                            ModuleRevisionId.newInstance("org.apache.ivy", "ivy", "2.2.0"),
                            ModuleRevisionId.newInstance("org.fusesource.jansi", "jansi", "1.2.1"),
                            ModuleRevisionId.newInstance("net.java.dev.jna", "jna", "3.2.3"),
                            ModuleRevisionId.newInstance("xalan","serializer", "2.7.1"),
                            ModuleRevisionId.newInstance("org.grails", "grails-docs", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-bootstrap", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-scripts", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-core", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-resources", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-web", grailsVersion),
                            ModuleRevisionId.newInstance("org.slf4j", "slf4j-api", slf4jVersion),
                            ModuleRevisionId.newInstance("org.springframework", "spring-test", springVersion),
                            ModuleRevisionId.newInstance("com.googlecode.concurrentlinkedhashmap", "concurrentlinkedhashmap-lru", "1.2_jdk5"),
                            ModuleRevisionId.newInstance("junit", "junit", junitVersion),
                        };
                        registerDependencies(dependencyManager, "build", buildDependencies);

                        ModuleRevisionId[] antDependencies = {
                                ModuleRevisionId.newInstance("org.apache.ant", "ant", antVersion),
                                ModuleRevisionId.newInstance("org.apache.ant", "ant-launcher", antVersion),
                                ModuleRevisionId.newInstance("org.apache.ant", "ant-junit", antVersion),
                                ModuleRevisionId.newInstance("org.apache.ant", "ant-trax", "1.7.1"),
                        };
                        registerDependencies(dependencyManager, "build", antDependencies, "junit");

                        // dependencies needed when creating docs
                        ModuleRevisionId[] docDependencies = {
                            ModuleRevisionId.newInstance("org.xhtmlrenderer", "core-renderer","R8"),
                            ModuleRevisionId.newInstance("com.lowagie","itext", "2.0.8"),
                            ModuleRevisionId.newInstance("org.grails", "grails-gdoc-engine", "1.0.1"),
                            ModuleRevisionId.newInstance("org.yaml", "snakeyaml", "1.8")
                        };
                        registerDependencies(dependencyManager, "docs", docDependencies);

                        // dependencies needed during development, but not for deployment
                        String tomcatVersion = "7.0.30";
                        ModuleRevisionId[] providedDependencies = {
                            ModuleRevisionId.newInstance("org.apache.tomcat.embed", "tomcat-embed-core", tomcatVersion),
                            ModuleRevisionId.newInstance("org.apache.tomcat.embed", "tomcat-embed-jasper",tomcatVersion),
                            ModuleRevisionId.newInstance("org.apache.tomcat.embed", "tomcat-embed-logging-log4j",tomcatVersion)
                        };
                        registerDependencies(dependencyManager, "provided", providedDependencies);

                        // dependencies needed at compile time
                        ModuleRevisionId[] groovyDependencies = {
                            ModuleRevisionId.newInstance("org.codehaus.groovy", "groovy-all", "1.8.8")
                        };
                        registerDependencies(dependencyManager, compileTimeDependenciesMethod, groovyDependencies, "jline");

                        ModuleRevisionId[] commonsExcludingLoggingAndXmlApis = {
                            ModuleRevisionId.newInstance("commons-beanutils", "commons-beanutils", "1.8.3"),
                            ModuleRevisionId.newInstance("commons-el", "commons-el", "1.0"),
                            ModuleRevisionId.newInstance("commons-validator", "commons-validator", "1.3.1")
                        };
                        registerDependencies(dependencyManager, compileTimeDependenciesMethod, commonsExcludingLoggingAndXmlApis, "commons-logging", "xml-apis", "commons-digester");

                        String datastoreMappingVersion = "1.1.2.RELEASE";
                        ModuleRevisionId[] compileDependencies = {
                            ModuleRevisionId.newInstance("aopalliance", "aopalliance", "1.0"),
                            ModuleRevisionId.newInstance("com.googlecode.concurrentlinkedhashmap", "concurrentlinkedhashmap-lru", "1.2_jdk5"),
                            ModuleRevisionId.newInstance("commons-codec", "commons-codec", "1.5"),
                            ModuleRevisionId.newInstance("commons-collections", "commons-collections", "3.2.1"),
                            ModuleRevisionId.newInstance("commons-io", "commons-io", "2.1"),
                            ModuleRevisionId.newInstance("commons-lang", "commons-lang", "2.6"),
                            ModuleRevisionId.newInstance("javax.transaction", "jta", "1.1"),
                            ModuleRevisionId.newInstance("org.hibernate.javax.persistence", "hibernate-jpa-2.0-api", "1.0.1.Final"),
                            ModuleRevisionId.newInstance("opensymphony", "sitemesh", "2.4"),
                            ModuleRevisionId.newInstance("org.grails", "grails-bootstrap", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-core", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-crud", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-hibernate", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-resources", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-spring", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-web", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-logging", grailsVersion),

                            // Plugins
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-codecs", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-controllers", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-domain-class", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-converters", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-datasource", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-filters", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-gsp", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-i18n", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-log4j", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-scaffolding", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-services", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-servlets", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-mimetypes", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-url-mappings", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-validation", grailsVersion),
                            ModuleRevisionId.newInstance("org.springframework", "spring-core", springVersion),
                            ModuleRevisionId.newInstance("org.springframework", "spring-aop", springVersion),
                            ModuleRevisionId.newInstance("org.springframework", "spring-aspects", springVersion),
                            ModuleRevisionId.newInstance("org.springframework", "spring-asm", springVersion),
                            ModuleRevisionId.newInstance("org.springframework", "spring-beans", springVersion),
                            ModuleRevisionId.newInstance("org.springframework", "spring-context", springVersion),
                            ModuleRevisionId.newInstance("org.springframework", "spring-context-support", springVersion),
                            ModuleRevisionId.newInstance("org.springframework", "spring-expression", springVersion),
                            ModuleRevisionId.newInstance("org.springframework", "spring-jdbc", springVersion),
                            ModuleRevisionId.newInstance("org.springframework", "spring-jms", springVersion),
                            ModuleRevisionId.newInstance("org.springframework", "spring-orm", springVersion),
                            ModuleRevisionId.newInstance("org.springframework", "spring-tx", springVersion),
                            ModuleRevisionId.newInstance("org.springframework", "spring-web", springVersion),
                            ModuleRevisionId.newInstance("org.springframework", "spring-webmvc", springVersion),
                            ModuleRevisionId.newInstance("org.slf4j", "slf4j-api", slf4jVersion)
                        };
                        registerDependencies(dependencyManager, compileTimeDependenciesMethod, compileDependencies);

                        EnhancedDefaultDependencyDescriptor grailsDatastoreGorm = registerDependency(dependencyManager, compileTimeDependenciesMethod, ModuleRevisionId.newInstance("org.grails", "grails-datastore-gorm", datastoreMappingVersion));
                        grailsDatastoreGorm.exclude(ModuleId.newInstance("org.grails", "grails-bootstrap"));
                        grailsDatastoreGorm.exclude(ModuleId.newInstance("org.grails", "grails-core"));
                        grailsDatastoreGorm.exclude(ModuleId.newInstance("org.grails", "grails-test"));
                        grailsDatastoreGorm.exclude(ModuleId.newInstance("org.grails", "grails-datastore-core"));
                        grailsDatastoreGorm.exclude(ModuleId.newInstance("org.slf4j", "slf4j-simple"));
                        grailsDatastoreGorm.exclude(ModuleId.newInstance("org.slf4j", "jcl-over-slf4j"));
                        grailsDatastoreGorm.exclude(ModuleId.newInstance("org.slf4j", "jul-to-slf4j"));
                        grailsDatastoreGorm.exclude(ModuleId.newInstance("org.slf4j", "slf4j-api"));


                        EnhancedDefaultDependencyDescriptor grailsDatastoreCore = registerDependency(dependencyManager, compileTimeDependenciesMethod, ModuleRevisionId.newInstance("org.grails", "grails-datastore-core", datastoreMappingVersion));
                        grailsDatastoreCore.exclude(ModuleId.newInstance("org.grails", "grails-bootstrap"));
                        grailsDatastoreCore.exclude(ModuleId.newInstance("org.grails", "grails-core"));
                        grailsDatastoreCore.exclude(ModuleId.newInstance("org.grails", "grails-test"));
                        grailsDatastoreCore.exclude(ModuleId.newInstance("org.slf4j", "slf4j-simple"));
                        grailsDatastoreCore.exclude(ModuleId.newInstance("org.slf4j", "jcl-over-slf4j"));
                        grailsDatastoreCore.exclude(ModuleId.newInstance("org.slf4j", "jul-to-slf4j"));
                        grailsDatastoreCore.exclude(ModuleId.newInstance("org.slf4j", "slf4j-api"));
                        grailsDatastoreCore.exclude(ModuleId.newInstance("javax.persistence", "persistence-api"));
                        grailsDatastoreCore.exclude(ModuleId.newInstance("javax.transaction", "jta"));
                        grailsDatastoreCore.exclude(ModuleId.newInstance("javassist", "javassist"));
                        grailsDatastoreCore.exclude(ModuleId.newInstance("commons-collections", "commons-collections"));
                        grailsDatastoreCore.exclude(ModuleId.newInstance("org.springframework", "spring-beans"));
                        grailsDatastoreCore.exclude(ModuleId.newInstance("org.springframework", "spring-core"));
                        grailsDatastoreCore.exclude(ModuleId.newInstance("org.springframework", "spring-context"));
                        grailsDatastoreCore.exclude(ModuleId.newInstance("org.springframework", "spring-web"));
                        grailsDatastoreCore.exclude(ModuleId.newInstance("org.springframework", "spring-tx"));
                        grailsDatastoreCore.exclude(ModuleId.newInstance("com.googlecode.concurrentlinkedhashmap", "concurrentlinkedhashmap-lru"));


                        ModuleRevisionId[] datastoreDependencies = {
                                ModuleRevisionId.newInstance("org.grails", "grails-datastore-simple", datastoreMappingVersion)
                        };

                        registerDependencies(dependencyManager, compileTimeDependenciesMethod, datastoreDependencies, false);

                        if (GrailsVersionUtils.isValidVersion(servletVersion, "3.0 > *")) {
                            ModuleRevisionId[] servletThreeCompileDependencies = {
                                 ModuleRevisionId.newInstance("org.grails", "grails-plugin-async", grailsVersion),
                            };
                            registerDependencies(dependencyManager, compileTimeDependenciesMethod, servletThreeCompileDependencies);
                        }

                        // dependencies needed for running tests
                        ModuleRevisionId[] testDependencies = {
                            ModuleRevisionId.newInstance("junit", "junit", junitVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-testing", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-test", grailsVersion),
                            ModuleRevisionId.newInstance("org.springframework", "spring-test", springVersion)

                        };
                        registerDependencies(dependencyManager, "test", testDependencies);

                        // dependencies needed at runtime only
                        ModuleRevisionId[] runtimeDependencies = {
                            ModuleRevisionId.newInstance("org.aspectj", "aspectjweaver", "1.6.10"),
                            ModuleRevisionId.newInstance("org.aspectj", "aspectjrt", "1.6.10"),
                            ModuleRevisionId.newInstance("cglib", "cglib", "2.2"),
                            ModuleRevisionId.newInstance("asm", "asm", "3.1"),
                            ModuleRevisionId.newInstance("commons-fileupload", "commons-fileupload", "1.2.2"),
                            ModuleRevisionId.newInstance("oro", "oro", "2.0.8"),
                            // data source

                            ModuleRevisionId.newInstance("commons-dbcp", "commons-dbcp", java5compatible ? "1.3": "1.4"),
                            ModuleRevisionId.newInstance("commons-pool", "commons-pool", "1.5.6"),
                            ModuleRevisionId.newInstance("com.h2database", "h2", "1.3.164"),
                            // JSP support
                            ModuleRevisionId.newInstance("javax.servlet", "jstl", "1.1.2"),
                            ModuleRevisionId.newInstance("xpp3", "xpp3_min", "1.1.4c")
                        };
                        registerDependencies(dependencyManager, runtimeDependenciesMethod, runtimeDependencies);
                        if(java5compatible) {
                            registerDependencies(dependencyManager, runtimeDependenciesMethod, new ModuleRevisionId[] { ModuleRevisionId.newInstance("javax.xml", "jaxb-api", "2.0"), } );
                        }

                        ModuleRevisionId[] ehcacheDependencies = {
                            ModuleRevisionId.newInstance("net.sf.ehcache", "ehcache-core", "2.4.6")
                        };
                        registerDependencies(dependencyManager, runtimeDependenciesMethod, ehcacheDependencies, "javax.jms:jms", "commons-logging", "javax.servlet:servlet-api", "org.slf4j:slf4j-api");

                        ModuleRevisionId[] loggingDependencies = {
                            ModuleRevisionId.newInstance("log4j", "log4j", "1.2.16"),
                            ModuleRevisionId.newInstance("org.slf4j", "jcl-over-slf4j", slf4jVersion),
                            ModuleRevisionId.newInstance("org.slf4j", "jul-to-slf4j", slf4jVersion)
                        };
                        registerDependencies(dependencyManager, runtimeDependenciesMethod, loggingDependencies, "javax.mail:mail", "javax.jms:jms", "com.sun.jdmk:jmxtools", "com.sun.jmx:jmxri");

                        return null;
                    }
                }); // end depenencies closure

                return null;
            }
        }; // end root closure
    }

    /**
     * The version of core spring dependencies such as {@code spring-core}, {@code spring-beans} etc.
     */
    public String getSpringVersion() {
        return this.springVersion;
    }
}
