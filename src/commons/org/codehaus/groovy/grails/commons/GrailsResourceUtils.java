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
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

    /*
    Domain path is always matched against the normalized File representation of an URL and
    can therefore work with slashes as separators.
    */
    public static Pattern DOMAIN_PATH_PATTERN = Pattern.compile(".+/grails-app/domain/(.+)\\.groovy");

    /*
    Resources are resolved against the platform specific path and must therefore obey the
    specific File.separator.
    */ 
    public static final Pattern GRAILS_RESOURCE_PATTERN_FIRST_MATCH;
    public static final Pattern GRAILS_RESOURCE_PATTERN_SECOND_MATCH;
    public static final Pattern GRAILS_RESOURCE_PATTERN_THIRD_MATCH;
    public static final Pattern GRAILS_RESOURCE_PATTERN_FOURTH_MATCH;
    static {
        String fs = File.separator;
        if (fs.equals("\\")) fs = "\\\\"; // backslashes need escaping in regexes
            
        GRAILS_RESOURCE_PATTERN_FIRST_MATCH = Pattern.compile(createGrailsResourcePattern(fs, "grails-app"+fs +"\\w+"));
        GRAILS_RESOURCE_PATTERN_THIRD_MATCH = Pattern.compile(createGrailsResourcePattern(fs, "grails-tests"));
        fs = "/";
        GRAILS_RESOURCE_PATTERN_SECOND_MATCH = Pattern.compile(createGrailsResourcePattern(fs, "grails-app"+fs +"\\w+"));
        GRAILS_RESOURCE_PATTERN_FOURTH_MATCH = Pattern.compile(createGrailsResourcePattern(fs, "grails-tests"));
    }

    public static final Pattern[] patterns = new Pattern[]{ GRAILS_RESOURCE_PATTERN_FIRST_MATCH, GRAILS_RESOURCE_PATTERN_SECOND_MATCH, GRAILS_RESOURCE_PATTERN_THIRD_MATCH, GRAILS_RESOURCE_PATTERN_FOURTH_MATCH};
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
	            return m.group(1);
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
            StringBuffer buf = new StringBuffer(appDir.getURL().toString());

            String className = getClassName(resource);
            buf.append("/views");
            if(GrailsClassUtils.isControllerClass(className)) {
               buf.append("/").append(GrailsClassUtils.getLogicalPropertyName(className, "Controller"));
            }
            else if(GrailsClassUtils.isTagLibClass(className)) {
                buf.append("/").append(GrailsClassUtils.getLogicalPropertyName(className, "TagLib"));
            }
            else if(isDomainClass(resource.getURL())) {
                buf.append("/").append(GrailsClassUtils.getPropertyName(className));
            }

            return new UrlResource(buf.toString());

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

            int i = url.lastIndexOf("grails-app");
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
}
