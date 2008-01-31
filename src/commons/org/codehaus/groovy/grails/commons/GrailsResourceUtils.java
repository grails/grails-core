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

//import java.io.File;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for working with Grails resources and URLs that represent artifacts
 * within a Grails application
 * 
 * @author Graeme Rocher
 *
 * @since 0.2
 * 
 * Created: 20th June 2006
 */
public class GrailsResourceUtils {

    //private static final String FS = File.separator;

    /**
     * The relative path to the WEB-INF directory
     */
    public static final String WEB_INF = "/WEB-INF";

    /**
     * The name of the Grails application directory
     */
    public static final String GRAILS_APP_DIR = "grails-app";

    /**
     * The name of the Web app dir within Grails
     */
    public static final String WEB_APP_DIR = "web-app";

    /**
     * The path to the views directory
     */
    public static final String VIEWS_DIR_PATH = GRAILS_APP_DIR + "/views/";

    /*
    Domain path is always matched against the normalized File representation of an URL and
    can therefore work with slashes as separators.
    */
    public static Pattern DOMAIN_PATH_PATTERN = Pattern.compile(".+/"+GRAILS_APP_DIR+"/domain/(.+)\\.groovy");

    /*
    This pattern will match any resource within a given directory inside grails-app
    */
    public static Pattern RESOURCE_PATH_PATTERN = Pattern.compile(".+?/"+GRAILS_APP_DIR+"/(.+?)/(.+?\\.groovy)");

    public static Pattern SPRING_SCRIPTS_PATH_PATTERN = Pattern.compile(".+?/"+GRAILS_APP_DIR+"/conf/spring/(.+?\\.groovy)");

    public static Pattern[] COMPILER_ROOT_PATTERNS = {
        SPRING_SCRIPTS_PATH_PATTERN,
        RESOURCE_PATH_PATTERN
    };

    /*
    Resources are resolved against the platform specific path and must therefore obey the
    specific File.separator.
    */
    public static final Pattern GRAILS_RESOURCE_PATTERN_FIRST_MATCH;
    public static final Pattern GRAILS_RESOURCE_PATTERN_SECOND_MATCH;
    public static final Pattern GRAILS_RESOURCE_PATTERN_THIRD_MATCH;
    public static final Pattern GRAILS_RESOURCE_PATTERN_FOURTH_MATCH;
    public static final Pattern GRAILS_RESOURCE_PATTERN_FIFTH_MATCH;
    public static final Pattern GRAILS_RESOURCE_PATTERN_SIXTH_MATCH;

    static {
        String fs = File.separator;
        if (fs.equals("\\")) fs = "\\\\"; // backslashes need escaping in regexes

        GRAILS_RESOURCE_PATTERN_FIRST_MATCH = Pattern.compile(createGrailsResourcePattern(fs, GRAILS_APP_DIR +fs+ "conf" +fs + "spring"));
        GRAILS_RESOURCE_PATTERN_THIRD_MATCH = Pattern.compile(createGrailsResourcePattern(fs, GRAILS_APP_DIR +fs +"\\w+"));
        GRAILS_RESOURCE_PATTERN_FIFTH_MATCH = Pattern.compile(createGrailsResourcePattern(fs, "grails-tests"));
        fs = "/";
        GRAILS_RESOURCE_PATTERN_SECOND_MATCH = Pattern.compile(createGrailsResourcePattern(fs, GRAILS_APP_DIR +fs+ "conf" +fs + "spring"));
        GRAILS_RESOURCE_PATTERN_FOURTH_MATCH = Pattern.compile(createGrailsResourcePattern(fs, GRAILS_APP_DIR +fs +"\\w+"));
        GRAILS_RESOURCE_PATTERN_SIXTH_MATCH = Pattern.compile(createGrailsResourcePattern(fs, "grails-tests"));
    }

    public static final Pattern[] patterns = new Pattern[]{
            GRAILS_RESOURCE_PATTERN_FIRST_MATCH,
            GRAILS_RESOURCE_PATTERN_SECOND_MATCH,
            GRAILS_RESOURCE_PATTERN_THIRD_MATCH,
            GRAILS_RESOURCE_PATTERN_FOURTH_MATCH,
            GRAILS_RESOURCE_PATTERN_FIFTH_MATCH,
            GRAILS_RESOURCE_PATTERN_SIXTH_MATCH
    };
    private static final Log LOG = LogFactory.getLog(GrailsResourceUtils.class);


    private static String createGrailsResourcePattern(String separator, String base) {
		return ".+"+separator +base+separator +"(.+)\\.groovy";
	}


    /**
	 * Checks whether the file referenced by the given url is a domain class
	 * 
	 * @param url The URL instance
	 * @return True if it is a domain class
	 */
	public static boolean isDomainClass(URL url) {
		if (url == null) return false;

        return DOMAIN_PATH_PATTERN.matcher(url.getFile()).find();
    }

	/**
	 * Gets the class name of the specified Grails resource
	 * 
	 * @param resource The Spring Resource
	 * @return The class name or null if the resource is not a Grails class
	 */
	public static String getClassName(Resource resource) {
        try {
        	return getClassName(resource.getFile().getAbsolutePath());
        } catch (IOException e) {
            throw new GrailsConfigurationException("I/O error reading class name from resource ["+resource+"]: " + e.getMessage(),e );
        }        	
	}

	/**
	 * Returns the class name for a Grails resource
	 * 
	 * @param path The path to check
	 * @return The class name or null if it doesn't exist
	 */
	public static String getClassName(String path) {
		for (int i = 0; i < patterns.length; i++) {
			Matcher m = patterns[i].matcher(path);
	        if(m.find()) {
	            return m.group(1).replaceAll("[/\\\\]", ".");
	        }			
		}
        return null;
	}	

	/**
	 * Checks whether the specified path is a Grails path
	 * 
	 * @param path The path to check
	 * @return True if it is a Grails path
	 */
	public static boolean isGrailsPath(String path) {
		for (int i = 0; i < patterns.length; i++) {
			Matcher m = patterns[i].matcher(path);
	        if(m.find()) {
	            return true;
	        }			
		}
		return false;
    }


	public static boolean isGrailsResource(Resource r) {
		try {
			return isGrailsPath(r.getURL().getFile());
		} catch (IOException e) {
			return false;
		}
	}

    public static Resource getViewsDir(Resource resource) {
        if(resource == null)return null;

        try {
            Resource appDir = getAppDir(resource);
            return new UrlResource(appDir.getURL().toString()+"/views");

        } catch (IOException e) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Error reading URL whilst resolving views dir from ["+resource+"]: " + e.getMessage(),e);
            }
            return null;
        }

    }

    public static Resource getAppDir(Resource resource) {
        if(resource == null)return null;


        try {
            String url = resource.getURL().toString();

            int i = url.lastIndexOf(GRAILS_APP_DIR);
            if(i > -1) {
                url = url.substring(0, i+10);
                return new UrlResource(url);
            }
            else {
                return null;
            }
        } catch (MalformedURLException e) {
            return null;
        } catch (IOException e) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Error reading URL whilst resolving app dir from ["+resource+"]: " + e.getMessage(),e);
            }
            return null;
        }
    }


    private static final Pattern PLUGIN_PATTERN = Pattern.compile(".+?(/plugins/.+?/"+GRAILS_APP_DIR+"/.+)");

    /**
     * This method will take a Grails resource (one located inside the grails-app dir) and get its relative path inside the WEB-INF directory
     * when deployed
     *
     * @param resource The Grails resource, which is a file inside the grails-app dir
     * @return The relative URL of the file inside the WEB-INF dir at deployment time or null if it cannot be established
     */
    public static String getRelativeInsideWebInf(Resource resource) {
        if(resource == null) return null;

        try {
            String url = resource.getURL().toString();
            int i = url.indexOf(WEB_INF);
            if(i > -1) {
                return url.substring(i);
            }
            else {
                Matcher m = PLUGIN_PATTERN.matcher(url);
                if(m.find()) {
                    return WEB_INF +m.group(1);
                }
                else {
                    i = url.lastIndexOf(GRAILS_APP_DIR);
                    if(i > -1) {
                        return WEB_INF+"/" + url.substring(i);
                    }
                }
            }
            
        } catch (IOException e) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Error reading URL whilst resolving relative path within WEB-INF from ["+resource+"]: " + e.getMessage(),e);
            }
            return null;

        }
        return null;
        
    }

    private static final Pattern PLUGIN_RESOURCE_PATTERN = Pattern.compile(".+?/(plugins/.+?)/"+GRAILS_APP_DIR+"/.+");
    
    /**
     * Retrieves the static resource path for the given Grails resource artifact (controller/taglib etc.)
     * 
     * @param resource The Resource
     * @param contextPath The additonal context path to prefix
     * @return The resource path
     */
    public static String getStaticResourcePathForResource(Resource resource, String contextPath) {

        if(contextPath == null)contextPath = "";
        if(resource == null)return contextPath;

        String url;
        try {
            url = resource.getURL().toString();
        } catch (IOException e) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Error reading URL whilst resolving static resource path from ["+resource+"]: " + e.getMessage(),e);
            }
            return contextPath;
        }

        Matcher m = PLUGIN_RESOURCE_PATTERN.matcher(url);
        if(m.find()) {            
             return (contextPath.length() > 0 ? contextPath + "/" : "") + m.group(1);
        }

        return contextPath; 
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
        for (int i = 0; i < COMPILER_ROOT_PATTERNS.length; i++) {
            Matcher m = COMPILER_ROOT_PATTERNS[i].matcher(path);
            if(m.find()) {
                return m.group(m.groupCount());
            }
        }
        return null;
    }
}
