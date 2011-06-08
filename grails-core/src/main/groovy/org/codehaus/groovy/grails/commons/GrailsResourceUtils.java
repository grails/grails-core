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
package org.codehaus.groovy.grails.commons;

import org.springframework.core.io.Resource;

import java.net.URL;
import java.util.regex.Pattern;

/**
 * Utility methods for working with Grails resources and URLs that represent artifacts
 * within a Grails application.
 *
 * @author Graeme Rocher
 *
 * @since 0.2
 * @deprecated  Use org.codehaus.groovy.grails.io.support.GrailsResourceUtils instead
 */
public class GrailsResourceUtils {

    /**
     * The relative path to the WEB-INF directory
     */
    public static final String WEB_INF = org.codehaus.groovy.grails.io.support.GrailsResourceUtils.WEB_INF;

    /**
     * The name of the Grails application directory
     */
    public static final String GRAILS_APP_DIR = org.codehaus.groovy.grails.io.support.GrailsResourceUtils.GRAILS_APP_DIR;

    /**
     * The name of the Web app dir within Grails
     */
    public static final String WEB_APP_DIR = org.codehaus.groovy.grails.io.support.GrailsResourceUtils.WEB_APP_DIR;

    /**
     * The path to the views directory
     */
    public static final String VIEWS_DIR_PATH = org.codehaus.groovy.grails.io.support.GrailsResourceUtils.VIEWS_DIR_PATH;

    /*
     Domain path is always matched against the normalized File representation of an URL and
    can therefore work with slashes as separators.
     */
    public static Pattern DOMAIN_PATH_PATTERN = org.codehaus.groovy.grails.io.support.GrailsResourceUtils.DOMAIN_PATH_PATTERN;

    /*
     This pattern will match any resource within a given directory inside grails-app
     */
    public static Pattern RESOURCE_PATH_PATTERN = org.codehaus.groovy.grails.io.support.GrailsResourceUtils.RESOURCE_PATH_PATTERN;

    public static Pattern SPRING_SCRIPTS_PATH_PATTERN = org.codehaus.groovy.grails.io.support.GrailsResourceUtils.SPRING_SCRIPTS_PATH_PATTERN;

    public static Pattern[] COMPILER_ROOT_PATTERNS = org.codehaus.groovy.grails.io.support.GrailsResourceUtils.COMPILER_ROOT_PATTERNS;

    /*
    Resources are resolved against the platform specific path and must therefore obey the
    specific File.separator.
     */
    public static final Pattern GRAILS_RESOURCE_PATTERN_FIRST_MATCH = org.codehaus.groovy.grails.io.support.GrailsResourceUtils.GRAILS_RESOURCE_PATTERN_FIRST_MATCH;
    public static final Pattern GRAILS_RESOURCE_PATTERN_SECOND_MATCH = org.codehaus.groovy.grails.io.support.GrailsResourceUtils.GRAILS_RESOURCE_PATTERN_SECOND_MATCH;
    public static final Pattern GRAILS_RESOURCE_PATTERN_THIRD_MATCH = org.codehaus.groovy.grails.io.support.GrailsResourceUtils.GRAILS_RESOURCE_PATTERN_THIRD_MATCH;
    public static final Pattern GRAILS_RESOURCE_PATTERN_FOURTH_MATCH = org.codehaus.groovy.grails.io.support.GrailsResourceUtils.GRAILS_RESOURCE_PATTERN_FOURTH_MATCH;
    public static final Pattern GRAILS_RESOURCE_PATTERN_FIFTH_MATCH = org.codehaus.groovy.grails.io.support.GrailsResourceUtils.GRAILS_RESOURCE_PATTERN_FIFTH_MATCH;
    public static final Pattern GRAILS_RESOURCE_PATTERN_SIXTH_MATCH = org.codehaus.groovy.grails.io.support.GrailsResourceUtils.GRAILS_RESOURCE_PATTERN_SIXTH_MATCH;
    public static final Pattern GRAILS_RESOURCE_PATTERN_SEVENTH_MATCH = org.codehaus.groovy.grails.io.support.GrailsResourceUtils.GRAILS_RESOURCE_PATTERN_SEVENTH_MATCH;
    public static final Pattern GRAILS_RESOURCE_PATTERN_EIGHTH_MATCH = org.codehaus.groovy.grails.io.support.GrailsResourceUtils.GRAILS_RESOURCE_PATTERN_EIGHTH_MATCH;

    /**
     * Checks whether the file referenced by the given url is a domain class
     *
     * @param url The URL instance
     * @return True if it is a domain class
     */
    public static boolean isDomainClass(URL url) {
        return org.codehaus.groovy.grails.io.support.GrailsResourceUtils.isDomainClass(url);
    }

    /**
     * Gets the class name of the specified Grails resource
     *
     * @param resource The Spring Resource
     * @return The class name or null if the resource is not a Grails class
     */
    public static String getClassName(Resource resource) {
        return org.codehaus.groovy.grails.io.support.GrailsResourceUtils.getClassName(resource);
    }

    /**
     * Returns the class name for a Grails resource.
     *
     * @param path The path to check
     * @return The class name or null if it doesn't exist
     */
    public static String getClassName(String path) {
        return org.codehaus.groovy.grails.io.support.GrailsResourceUtils.getClassName(path);
    }

    /**
     * Checks whether the specified path is a Grails path.
     *
     * @param path The path to check
     * @return True if it is a Grails path
     */
    public static boolean isGrailsPath(String path) {
        return org.codehaus.groovy.grails.io.support.GrailsResourceUtils.isGrailsPath(path);
    }

    public static boolean isGrailsResource(Resource r) {
        return org.codehaus.groovy.grails.io.support.GrailsResourceUtils.isGrailsResource(r);
    }

    public static Resource getViewsDir(Resource resource) {
        return org.codehaus.groovy.grails.io.support.GrailsResourceUtils.getViewsDir(resource);
    }

    public static Resource getAppDir(Resource resource) {
        return org.codehaus.groovy.grails.io.support.GrailsResourceUtils.getAppDir(resource);
    }


    /**
     * Takes a Grails resource (one located inside the grails-app dir) and gets its relative path inside the WEB-INF directory
     * when deployed.
     *
     * @param resource The Grails resource, which is a file inside the grails-app dir
     * @return The relative URL of the file inside the WEB-INF dir at deployment time or null if it cannot be established
     */
    public static String getRelativeInsideWebInf(Resource resource) {
        return org.codehaus.groovy.grails.io.support.GrailsResourceUtils.getRelativeInsideWebInf(resource);
    }


    /**
     * Retrieves the static resource path for the given Grails resource artifact (controller/taglib etc.)
     *
     * @param resource The Resource
     * @param contextPath The additonal context path to prefix
     * @return The resource path
     */
    public static String getStaticResourcePathForResource(Resource resource, String contextPath) {
        return org.codehaus.groovy.grails.io.support.GrailsResourceUtils.getStaticResourcePathForResource(resource, contextPath);
    }

    /**
     * Get the path relative to an artefact folder under grails-app i.e:
     *
     * Input: /usr/joe/project/grails-app/conf/BootStrap.groovy
     * Output: BootStrap.groovy
     *
     * Input: /usr/joe/project/grails-app/domain/com/mystartup/Book.groovy
     * Output: com/mystartup/Book.groovy
     *
     * @param path The path to evaluate
     * @return The path relative to the root folder grails-app
     */
    public static String getPathFromRoot(String path) {
        return org.codehaus.groovy.grails.io.support.GrailsResourceUtils.getPathFromRoot(path);
    }

    /**
     * Takes a file path and returns the name of the folder under grails-app i.e:
     *
     * Input: /usr/joe/project/grails-app/domain/com/mystartup/Book.groovy
     * Output: domain
     *
     * @param path The path
     * @return The domain or null if not known
     */
    public static String getArtefactDirectory(String path) {
        return org.codehaus.groovy.grails.io.support.GrailsResourceUtils.getArtefactDirectory(path);
    }

    /**
     * Takes any number of Strings and appends them into a uri, making
     * sure that a forward slash is inserted between each piece and
     * making sure that no duplicate slashes are in the uri
     *
     * <pre>
     * Input: ""
     * Output: ""
     *
     * Input: "/alpha", "/beta", "/gamma"
     * Output: "/alpha/beta/gamma
     *
     * Input: "/alpha/, "/beta/", "/gamma"
     * Output: "/alpha/beta/gamma
     *
     * Input: "/alpha/", "/beta/", "/gamma/"
     * Output "/alpha/beta/gamma/
     *
     * Input: "alpha", "beta", "gamma"
     * Output: "alpha/beta/gamma
     * </pre>
     *
     * @param pieces Strings to concatenate together into a uri
     * @return a uri
     */
    public static String appendPiecesForUri(String... pieces) {
        return org.codehaus.groovy.grails.io.support.GrailsResourceUtils.appendPiecesForUri(pieces);
    }
}
