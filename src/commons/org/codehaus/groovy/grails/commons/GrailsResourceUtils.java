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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;
import org.springframework.core.io.Resource;
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
	
    public static Pattern DOMAIN_PATH_PATTERN = Pattern.compile(".+\\\\grails-app\\\\domain\\\\(.+)\\.groovy");
	public static Pattern GRAILS_RESOURCE_PATTERN = Pattern.compile(".+\\\\grails-app\\\\\\w+\\\\(.+)\\.groovy");
    static{
        if(File.separator.equals("/")){
			DOMAIN_PATH_PATTERN = Pattern.compile(".+/grails-app/domain/(.+)\\.groovy");   
            GRAILS_RESOURCE_PATTERN =    
                Pattern.compile(".+/grails-app/\\w+/(.+)\\.groovy");
        }
    }    

	/**
	 * Checks whether the file referenced by the given url is a domain class
	 * 
	 * @param url The URL instance
	 * @return True if it is a domain class
	 */
	public static boolean isDomainClass(URL url) {
		if(url == null)return false;
		
		if(DOMAIN_PATH_PATTERN.matcher(url.getFile()).find()) return true;
				
		return false;
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
		Matcher m = GrailsResourceUtils.GRAILS_RESOURCE_PATTERN.matcher(path);
        if(m.find()) {
            return m.group(1);
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
		Matcher m = GrailsResourceUtils.GRAILS_RESOURCE_PATTERN.matcher(path);
        if(m.find()) {
            return true;
        }
        return false;		
	}
}
