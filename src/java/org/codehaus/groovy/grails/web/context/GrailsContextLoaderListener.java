/**
 * 
 */
package org.codehaus.groovy.grails.web.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;

/**
 * Extends the Spring default ContextLoader to load GrailsApplicationContext
 * 
 * @author Graeme Rocher
 *
 */
public class GrailsContextLoaderListener extends ContextLoaderListener {

    private static final Log LOG = LogFactory.getLog(GrailsContextLoaderListener.class);

	protected ContextLoader createContextLoader() {
		return new GrailsContextLoader();
	}
}
