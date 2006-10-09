/**
 * 
 */
package org.codehaus.groovy.grails.web.context;

import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;

/**
 * @author graemerocher
 *
 */
public class GrailsContextLoaderListener extends ContextLoaderListener {

	protected ContextLoader createContextLoader() {
		return new GrailsContextLoader();
	}

}
