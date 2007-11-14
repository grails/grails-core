/**
 * 
 */
package org.codehaus.groovy.grails.web.context;

import groovy.lang.ExpandoMetaClass;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsConfigUtils;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;

/**
 * @author graemerocher
 *
 */
public class GrailsContextLoader extends ContextLoader {

	public static final Log LOG = LogFactory.getLog(GrailsContextLoader.class);
	
	protected WebApplicationContext createWebApplicationContext(ServletContext servletContext, ApplicationContext parent) throws BeansException {

        ExpandoMetaClass.enableGlobally();

        if(LOG.isDebugEnabled()) {
			LOG.debug("[GrailsContextLoader] Loading context. Creating parent application context");
		}
		WebApplicationContext  ctx =  super.createWebApplicationContext(servletContext, parent);
		
        GrailsApplication application = (GrailsApplication) ctx.getBean(GrailsApplication.APPLICATION_ID, GrailsApplication.class);		
		ctx =  GrailsConfigUtils.configureWebApplicationContext(servletContext, ctx);
        GrailsConfigUtils.executeGrailsBootstraps(application, ctx, servletContext);
        return ctx;
    }

}
