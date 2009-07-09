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
package org.codehaus.groovy.grails.web.binding;

import java.beans.PropertyEditorSupport;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Provides type conversion from Strings to java.net.URI instances
 * 
 * @author Graeme Rocher
 * @since 0.3 
 *
 */
public class UriEditor extends PropertyEditorSupport {

	public String getAsText() {
		URI uri = (URI)getValue();
		if(uri==null) {
			return "";
		}
		else {
			return uri.toString();
		}
	}

	public void setAsText(String uriString) throws IllegalArgumentException {

		URI uri;
		try {
			uri = new URI(uriString);
			setValue(uri);
		} catch (URISyntaxException e) {
			setValue(null);
		}	
	}

}
