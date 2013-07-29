/*
 * Copyright 2004-2005 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.codehaus.groovy.grails.plugins.GrailsVersionUtils;

/**
 * Encapsulates information about the core dependencies of Grails.
 *
 * @author Graeme Rocher
 * @author Luke Daley
 */
public class GrailsCoreDependencies {

    public final String grailsVersion;
    public final String servletVersion;
    protected final String groovyVersion = "2.1.6";
    protected final String log4jVersion = "1.2.17";
    protected final String h2Version = "1.3.170";
    protected final String jaxbVersion = "2.0";
    protected String servletApiVersion = "3.0.1";
    protected String spockVersion = "0.7-groovy-2.0";

    public boolean java5compatible;
    protected Collection<Dependency> buildDependencies;
    protected Collection<Dependency> docDependencies;
    protected Collection<Dependency> providedDependencies;
    protected Collection<Dependency> compileDependencies;

    protected Collection<Dependency> runtimeDependencies;
    protected Collection<Dependency> testDependencies;

    public GrailsCoreDependencies(String grailsVersion) {
        this(grailsVersion, "2.5", false, true);
    }

    public GrailsCoreDependencies(String grailsVersion, String servletVersion) {
        this(grailsVersion, servletVersion, false, true);
    }

    public GrailsCoreDependencies(String grailsVersion, String servletVersion, boolean java5compatible, boolean isGrailsProject) {
        this.grailsVersion = grailsVersion;
        this.servletVersion = servletVersion == null ? "2.5" : servletVersion;
        this.java5compatible = java5compatible;

        buildDependencies = Arrays.asList(
            new Dependency("xalan","serializer", "2.7.1", true, "xml-apis:xml-apis"),
            new Dependency("org.grails", "grails-bootstrap", grailsVersion, true ),
            new Dependency("org.grails", "grails-scripts", grailsVersion, true )
        );

        docDependencies = Arrays.asList(
            new Dependency("org.grails", "grails-docs", grailsVersion, true),
            new Dependency("com.lowagie","itext", "2.0.8", true)
        );

        providedDependencies = Arrays.asList(
            new Dependency("javax.servlet","javax.servlet-api", servletApiVersion, true)
        );

        compileDependencies = Arrays.asList(
            new Dependency("org.codehaus.groovy", "groovy-all", groovyVersion, true),
            new Dependency("org.grails", "grails-plugin-rest", grailsVersion, true),
            new Dependency("org.grails", "grails-plugin-databinding", grailsVersion, true),
            new Dependency("org.grails", "grails-plugin-i18n", grailsVersion, true),
            new Dependency("org.grails", "grails-plugin-filters", grailsVersion, true),
            new Dependency("org.grails", "grails-plugin-gsp", grailsVersion, true),
            new Dependency("org.grails", "grails-plugin-log4j", grailsVersion, true),
            new Dependency("org.grails", "grails-plugin-services", grailsVersion, true),
            new Dependency("org.grails", "grails-plugin-servlets", grailsVersion, true),
            new Dependency("org.grails", "grails-plugin-url-mappings", grailsVersion, true)
        );

        if (GrailsVersionUtils.isValidVersion(servletVersion, "3.0 > *")) {
            compileDependencies = new ArrayList<Dependency>(compileDependencies);
            compileDependencies.add(  new Dependency("org.grails", "grails-plugin-async", grailsVersion, true, "javax:javaee-web-api") );
        }

        if(isGrailsProject) {
            String[] spockExcludes = {"org.codehaus.groovy:groovy-all", "junit:junit-dep"};
            testDependencies = Arrays.asList(
                new Dependency("org.grails", "grails-plugin-testing", grailsVersion, true),
                new Dependency("org.spockframework", "spock-core", spockVersion, true,spockExcludes),
                new Dependency("org.grails", "grails-test", grailsVersion, true)
            );
        }
        else {
            testDependencies = Arrays.asList(
                new Dependency("org.grails", "grails-plugin-testing", grailsVersion, true),
                new Dependency("org.grails", "grails-test", grailsVersion, true)
            );

        }

        String[] loggingExcludes = {"javax.mail:mail", "javax.jms:jms", "com.sun.jdmk:jmxtools", "com.sun.jmx:jmxri"};
        runtimeDependencies = Arrays.asList(
            new Dependency("com.h2database", "h2", h2Version, true),
            new Dependency("log4j", "log4j", log4jVersion, true, loggingExcludes),
            new Dependency("org.grails", "grails-resources", grailsVersion, true)
        );

        if (java5compatible) {
            compileDependencies = new ArrayList<Dependency>(compileDependencies);
            compileDependencies.add(new Dependency("javax.xml", "jaxb-api", jaxbVersion, true) );
        }
    }

    public void setJava5compatible(boolean java5compatible) {
        this.java5compatible = java5compatible;
    }

    public String getGrailsVersion() {
        return grailsVersion;
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
}
