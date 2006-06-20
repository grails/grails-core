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

import java.net.URL;
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
	
    public static Pattern DOMAIN_PATH_PATTERN = Pattern.compile(".+/grails-app/domain/(.+)\\.groovy");

	/**
	 * Checks whether the file referenced by the given url is a domain class
	 * 
	 * @param url The URL instance
	 * @return True if it is a domain class
	 */
	public static boolean isDomainClass(URL url) {
		if(url == null)return false;
		
		if(DOMAIN_PATH_PATTERN.matcher(url.getPath()).find()) return true;
				
		return false;
	}

}
