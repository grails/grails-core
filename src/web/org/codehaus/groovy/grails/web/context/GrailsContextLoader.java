/**
 * 
 */
package org.codehaus.groovy.grails.web.context;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsConfigUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author graemerocher
 *
 */
public class GrailsContextLoader extends ContextLoader {

	public static final Log LOG = LogFactory.getLog(GrailsContextLoader.class);
	
	protected WebApplicationContext createWebApplicationContext(ServletContext servletContext, ApplicationContext parent) throws BeansException {
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("[GrailsContextLoader] Loading context. Creating parent application context");
		}
		WebApplicationContext  ctx =  super.createWebApplicationContext(servletContext, parent);
		
		return GrailsConfigUtils.configureWebApplicationContext(servletContext, ctx);
	}

}
