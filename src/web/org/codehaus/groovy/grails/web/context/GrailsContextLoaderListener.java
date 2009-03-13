/**
 * 
 */
package org.codehaus.groovy.grails.web.context;

import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;

/**
 * Extends the Spring default ContextLoader to load GrailsApplicationContext
 * 
 * @author Graeme Rocher
 *
 */
public class GrailsContextLoaderListener extends ContextLoaderListener {

	protected ContextLoader createContextLoader() {
		return new GrailsContextLoader();
	}

}
