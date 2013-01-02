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

import org.codehaus.groovy.grails.plugins.GrailsVersionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Encapsulates information about the core dependencies of Grails.
 *
 * @author Graeme Rocher
 * @author Luke Daley
 */
public class GrailsCoreDependencies {

    public final String grailsVersion;
    public final String servletVersion;
    protected final String uaaVersion = "1.0.1.RELEASE";
    protected final String springVersion = "3.1.2.RELEASE";
    protected final String slf4jVersion = "1.7.2";
    protected final String antVersion = "1.8.4";
    protected final String junitVersion = "4.10";
    protected final String groovyVersion = "2.0.5";
    protected final String commonsBeanUtilsVersion = "1.8.3";
    protected final String commonsValidatorVersion = "1.3.1";
    protected final String concurrentLinkedHashMapVersion = "1.2_jdk5";
    protected final String commonsCodecVersion = "1.5";
    protected final String commonsCollectionsVersion = "3.2.1";
    protected final String commonsIoVersion = "2.1";
    protected final String commonsLangVersion = "2.6";
    protected final String jtaVersion = "1.1";
    protected final String sitemeshVersion = "2.4";
    protected final String hibernateJpaVersion = "1.0.1.Final";
    protected final String aopAllianceVersion = "1.0";
    protected final String log4jVersion = "1.2.16";
    protected final String aspectjVersion = "1.7.1";
    protected final String cglibVersion = "2.2";
    protected final String asmVersion = "3.1";
    protected final String commonsFileUploadVersion = "1.2.2";
    protected final String oroVersion = "2.0.8";
    protected final String h2Version = "1.3.164";
    protected final String commonsPoolVersion = "1.5.6";
    protected final String jstlVersion = "1.1.2";
    protected final String ehcacheVersion = "2.4.6";
    protected final String xpp3Version = "1.1.4c";
    protected final String jaxbVersion = "2.0";
    protected final String tomcatVersion = "7.0.30";
    protected final String datastoreMappingVersion = "1.1.2.RELEASE";

    public boolean java5compatible;
    protected Collection<Dependency> buildDependencies;
    protected Collection<Dependency> docDependencies;
    protected Collection<Dependency> providedDependencies;
    protected Collection<Dependency> compileDependencies;
    protected Collection<Dependency> runtimeDependencies;

    protected Collection<Dependency> testDependencies;


    public GrailsCoreDependencies(String grailsVersion) {
        this(grailsVersion, "2.5", false);
    }

    public GrailsCoreDependencies(String grailsVersion, String servletVersion) {
        this(grailsVersion, servletVersion, false);
    }

    public GrailsCoreDependencies(String grailsVersion, String servletVersion, boolean java5compatible) {
        this.grailsVersion = grailsVersion;
        this.servletVersion = servletVersion == null ? "2.5" : servletVersion;
        this.java5compatible = java5compatible;

        String[] uaaExcludes = {"org.springframework.roo.wrapping:org.springframework.roo.wrapping.protobuf-java-lite",
                                "org.springframework.roo.wrapping:org.springframework.roo.wrapping.json-simple",
                                "org.springframework.roo.wrapping:org.springframework.roo.wrapping.bcpg-jdk15",
                                "org.springframework.roo.wrapping:org.springframework.roo.wrapping.bcprov-jdk15"};
        buildDependencies = Arrays.asList( new Dependency( "org.springframework.uaa", "org.springframework.uaa.client", uaaVersion, uaaExcludes),
            new Dependency( "com.google.protobuf", "protobuf-java", "2.4.1" ),
            new Dependency( "com.googlecode.json-simple", "json-simple", "1.1"),
            new Dependency( "org.bouncycastle", "bcpg-jdk15", "1.45" ),
            new Dependency( "org.bouncycastle", "bcprov-jdk15", "1.45" ),
            new Dependency( "jline", "jline", "1.0" ),
            new Dependency( "org.apache.ivy", "ivy", "2.2.0"),
            new Dependency( "org.fusesource.jansi", "jansi", "1.2.1"),
            new Dependency( "net.java.dev.jna", "jna", "3.2.3" ),
            new Dependency("xalan","serializer", "2.7.1"),
            new Dependency("org.grails", "grails-docs", grailsVersion),
            new Dependency("org.grails", "grails-bootstrap", grailsVersion ),
            new Dependency("org.grails", "grails-scripts", grailsVersion ),
            new Dependency("org.slf4j", "slf4j-api", slf4jVersion ),
            new Dependency("org.slf4j", "jcl-over-slf4j", slf4jVersion),
            new Dependency("org.apache.ant", "ant", antVersion, "junit"),
            new Dependency("org.apache.ant", "ant-launcher", antVersion, "junit"),
            new Dependency("org.apache.ant", "ant-junit", antVersion, "junit"),
            new Dependency("org.apache.ant", "ant-trax", "1.7.1", "junit")
        );


        docDependencies = Arrays.asList(
            new Dependency("org.xhtmlrenderer", "core-renderer","R8"),
            new Dependency("com.lowagie","itext", "2.0.8"),
            new Dependency("org.grails", "grails-gdoc-engine", "1.0.1"),
            new Dependency("org.yaml", "snakeyaml", "1.8")
        );

        providedDependencies = Arrays.asList(
            new Dependency("org.apache.tomcat.embed", "tomcat-embed-core", tomcatVersion),
            new Dependency("org.apache.tomcat.embed", "tomcat-embed-jasper", tomcatVersion),
            new Dependency("org.apache.tomcat.embed", "tomcat-embed-logging-log4j", tomcatVersion)
        );

        String[] commonExcludes = {"commons-logging", "xml-apis", "commons-digester"};

        Dependency grailsDatastoreGorm = new Dependency("org.grails", "grails-datastore-gorm", datastoreMappingVersion);
        grailsDatastoreGorm.exclude("org.grails", "grails-bootstrap");
        grailsDatastoreGorm.exclude("org.grails", "grails-core");
        grailsDatastoreGorm.exclude("org.grails", "grails-test");
        grailsDatastoreGorm.exclude("org.grails", "grails-datastore-core");
        grailsDatastoreGorm.exclude("org.slf4j", "slf4j-simple");
        grailsDatastoreGorm.exclude("org.slf4j", "jcl-over-slf4j");
        grailsDatastoreGorm.exclude("org.slf4j", "jul-to-slf4j");
        grailsDatastoreGorm.exclude("org.slf4j", "slf4j-api");


        Dependency grailsDatastoreCore = new Dependency("org.grails", "grails-datastore-core", datastoreMappingVersion);
        grailsDatastoreCore.exclude("org.grails", "grails-bootstrap");
        grailsDatastoreCore.exclude("org.grails", "grails-core");
        grailsDatastoreCore.exclude("org.grails", "grails-test");
        grailsDatastoreCore.exclude("org.slf4j", "slf4j-simple");
        grailsDatastoreCore.exclude("org.slf4j", "jcl-over-slf4j");
        grailsDatastoreCore.exclude("org.slf4j", "jul-to-slf4j");
        grailsDatastoreCore.exclude("org.slf4j", "slf4j-api");
        grailsDatastoreCore.exclude("javax.persistence", "persistence-api");
        grailsDatastoreCore.exclude("javax.transaction", "jta");
        grailsDatastoreCore.exclude("javassist", "javassist");
        grailsDatastoreCore.exclude("commons-collections", "commons-collections");
        grailsDatastoreCore.exclude("org.springframework", "spring-beans");
        grailsDatastoreCore.exclude("org.springframework", "spring-core");
        grailsDatastoreCore.exclude("org.springframework", "spring-context");
        grailsDatastoreCore.exclude("org.springframework", "spring-web");
        grailsDatastoreCore.exclude("org.springframework", "spring-tx");
        grailsDatastoreCore.exclude("com.googlecode.concurrentlinkedhashmap", "concurrentlinkedhashmap-lru");

        Dependency grailsDatastoreSimple = new Dependency("org.grails", "grails-datastore-simple", datastoreMappingVersion);
        grailsDatastoreSimple.setTransitive(false);
        compileDependencies = Arrays.asList(
            grailsDatastoreGorm,
            grailsDatastoreCore,
            grailsDatastoreSimple,
            new Dependency("org.codehaus.groovy", "groovy-all", groovyVersion, "jline"),
            new Dependency("commons-beanutils", "commons-beanutils", commonsBeanUtilsVersion,commonExcludes),
            new Dependency("commons-el", "commons-el", "1.0",commonExcludes),
            new Dependency("commons-validator", "commons-validator", commonsValidatorVersion,commonExcludes),
            new Dependency("aopalliance", "aopalliance", aopAllianceVersion),
            new Dependency("com.googlecode.concurrentlinkedhashmap", "concurrentlinkedhashmap-lru", concurrentLinkedHashMapVersion),
            new Dependency("commons-codec", "commons-codec", commonsCodecVersion),
            new Dependency("commons-collections", "commons-collections", commonsCollectionsVersion),
            new Dependency("commons-io", "commons-io", commonsIoVersion),
            new Dependency("commons-lang", "commons-lang", commonsLangVersion),
            new Dependency("javax.transaction", "jta", jtaVersion),
            new Dependency("org.hibernate.javax.persistence", "hibernate-jpa-2.0-api", hibernateJpaVersion),
            new Dependency("opensymphony", "sitemesh", sitemeshVersion),
            new Dependency("org.grails", "grails-bootstrap", grailsVersion),
            new Dependency("org.grails", "grails-core", grailsVersion),
            new Dependency("org.grails", "grails-crud", grailsVersion),
            new Dependency("org.grails", "grails-hibernate", grailsVersion),
            new Dependency("org.grails", "grails-resources", grailsVersion),
            new Dependency("org.grails", "grails-spring", grailsVersion),
            new Dependency("org.grails", "grails-web", grailsVersion),
            new Dependency("org.grails", "grails-logging", grailsVersion),
            new Dependency("org.grails", "grails-plugin-codecs", grailsVersion),
            new Dependency("org.grails", "grails-plugin-controllers", grailsVersion),
            new Dependency("org.grails", "grails-plugin-domain-class", grailsVersion),
            new Dependency("org.grails", "grails-plugin-converters", grailsVersion),
            new Dependency("org.grails", "grails-plugin-datasource", grailsVersion),
            new Dependency("org.grails", "grails-plugin-filters", grailsVersion),
            new Dependency("org.grails", "grails-plugin-gsp", grailsVersion),
            new Dependency("org.grails", "grails-plugin-i18n", grailsVersion),
            new Dependency("org.grails", "grails-plugin-log4j", grailsVersion),
            new Dependency("org.grails", "grails-plugin-scaffolding", grailsVersion),
            new Dependency("org.grails", "grails-plugin-services", grailsVersion),
            new Dependency("org.grails", "grails-plugin-servlets", grailsVersion),
            new Dependency("org.grails", "grails-plugin-mimetypes", grailsVersion),
            new Dependency("org.grails", "grails-plugin-url-mappings", grailsVersion),
            new Dependency("org.grails", "grails-plugin-validation", grailsVersion),
            new Dependency("org.springframework", "spring-core", springVersion,commonExcludes),
            new Dependency("org.springframework", "spring-aop", springVersion,commonExcludes),
            new Dependency("org.springframework", "spring-aspects", springVersion,commonExcludes),
            new Dependency("org.springframework", "spring-asm", springVersion,commonExcludes),
            new Dependency("org.springframework", "spring-beans", springVersion,commonExcludes),
            new Dependency("org.springframework", "spring-context", springVersion,commonExcludes),
            new Dependency("org.springframework", "spring-context-support", springVersion,commonExcludes),
            new Dependency("org.springframework", "spring-expression", springVersion,commonExcludes),
            new Dependency("org.springframework", "spring-jdbc", springVersion,commonExcludes),
            new Dependency("org.springframework", "spring-jms", springVersion,commonExcludes),
            new Dependency("org.springframework", "spring-orm", springVersion,commonExcludes),
            new Dependency("org.springframework", "spring-tx", springVersion,commonExcludes),
            new Dependency("org.springframework", "spring-web", springVersion,commonExcludes),
            new Dependency("org.springframework", "spring-webmvc", springVersion,commonExcludes),
            new Dependency("org.slf4j", "slf4j-api", slf4jVersion)
        );

        if (GrailsVersionUtils.isValidVersion(servletVersion, "3.0 > *")) {
            compileDependencies.add(  new Dependency("org.grails", "grails-plugin-async", grailsVersion) );
        }


        testDependencies = Arrays.asList(
            new Dependency("junit", "junit", junitVersion),
            new Dependency("org.grails", "grails-plugin-testing", grailsVersion),
            new Dependency("org.grails", "grails-test", grailsVersion),
            new Dependency("org.springframework", "spring-test", springVersion)

        );

        String[] logginExcludes = {"javax.mail:mail", "javax.jms:jms", "com.sun.jdmk:jmxtools", "com.sun.jmx:jmxri"};
        runtimeDependencies = Arrays.asList(
            new Dependency("org.aspectj", "aspectjweaver", aspectjVersion),
            new Dependency("org.aspectj", "aspectjrt", aspectjVersion),
            new Dependency("cglib", "cglib", cglibVersion),
            new Dependency("asm", "asm", asmVersion),
            new Dependency("commons-fileupload", "commons-fileupload", commonsFileUploadVersion),
            new Dependency("oro", "oro", oroVersion),
            new Dependency("commons-dbcp", "commons-dbcp", java5compatible ? "1.3": "1.4"),
            new Dependency("commons-pool", "commons-pool", commonsPoolVersion),
            new Dependency("com.h2database", "h2", h2Version),
            new Dependency("javax.servlet", "jstl", jstlVersion),
            new Dependency("xpp3", "xpp3_min", xpp3Version),
            new Dependency("net.sf.ehcache", "ehcache-core", ehcacheVersion, "javax.jms:jms", "commons-logging", "javax.servlet:servlet-api", "org.slf4j:slf4j-api"),
            new Dependency("log4j", "log4j", log4jVersion, logginExcludes),
            new Dependency("org.slf4j", "jcl-over-slf4j", slf4jVersion, logginExcludes),
            new Dependency("org.slf4j", "jul-to-slf4j", slf4jVersion, logginExcludes)

        );

        if(java5compatible) {
            runtimeDependencies.add(new Dependency("javax.xml", "jaxb-api", jaxbVersion) );
        }

    }

    public String getGrailsVersion() {
        return grailsVersion;
    }

    public String getServletVersion() {
        return servletVersion;
    }

    public String getUaaVersion() {
        return uaaVersion;
    }

    public String getSlf4jVersion() {
        return slf4jVersion;
    }

    public String getAntVersion() {
        return antVersion;
    }

    public String getJunitVersion() {
        return junitVersion;
    }

    public String getGroovyVersion() {
        return groovyVersion;
    }

    public String getCommonsBeanUtilsVersion() {
        return commonsBeanUtilsVersion;
    }

    public String getCommonsValidatorVersion() {
        return commonsValidatorVersion;
    }

    public String getConcurrentLinkedHashMapVersion() {
        return concurrentLinkedHashMapVersion;
    }

    public String getCommonsCodecVersion() {
        return commonsCodecVersion;
    }

    public String getCommonsCollectionsVersion() {
        return commonsCollectionsVersion;
    }

    public String getCommonsIoVersion() {
        return commonsIoVersion;
    }

    public String getCommonsLangVersion() {
        return commonsLangVersion;
    }

    public String getJtaVersion() {
        return jtaVersion;
    }

    public String getSitemeshVersion() {
        return sitemeshVersion;
    }

    public String getHibernateJpaVersion() {
        return hibernateJpaVersion;
    }

    public String getAopAllianceVersion() {
        return aopAllianceVersion;
    }

    public String getLog4jVersion() {
        return log4jVersion;
    }

    public String getAspectjVersion() {
        return aspectjVersion;
    }

    public String getCglibVersion() {
        return cglibVersion;
    }

    public String getAsmVersion() {
        return asmVersion;
    }

    public String getCommonsFileUploadVersion() {
        return commonsFileUploadVersion;
    }

    public String getOroVersion() {
        return oroVersion;
    }

    public String getH2Version() {
        return h2Version;
    }

    public String getCommonsPoolVersion() {
        return commonsPoolVersion;
    }

    public String getJstlVersion() {
        return jstlVersion;
    }

    public String getEhcacheVersion() {
        return ehcacheVersion;
    }

    public String getXpp3Version() {
        return xpp3Version;
    }

    public String getJaxbVersion() {
        return jaxbVersion;
    }

    public String getTomcatVersion() {
        return tomcatVersion;
    }

    public String getDatastoreMappingVersion() {
        return datastoreMappingVersion;
    }

    public boolean isJava5compatible() {
        return java5compatible;
    }

    public Collection<Dependency> getBuildDependencies() {
        return buildDependencies;
    }

    public Collection<Dependency> getDocDependencies() {
        return docDependencies;
    }

    public Collection<Dependency> getProvidedDependencies() {
        return providedDependencies;
    }

    public Collection<Dependency> getCompileDependencies() {
        return compileDependencies;
    }

    public Collection<Dependency> getRuntimeDependencies() {
        return runtimeDependencies;
    }

    public Collection<Dependency> getTestDependencies() {
        return testDependencies;
    }

    public Collection<String> getBuildDependencyPatterns() {

        Collection<String> dependencies = new ArrayList<String>();
        for (Dependency buildDependency : buildDependencies) {
            dependencies.add(buildDependency.getPattern());
        }
        return dependencies;
    }


    /**
     * The version of core spring dependencies such as {@code spring-core}, {@code spring-beans} etc.
     */
    public String getSpringVersion() {
        return springVersion;
    }
}
