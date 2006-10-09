package org.codehaus.groovy.grails.web.binding;

import java.beans.PropertyEditorSupport;
import java.net.URI;
import java.net.URISyntaxException;

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
