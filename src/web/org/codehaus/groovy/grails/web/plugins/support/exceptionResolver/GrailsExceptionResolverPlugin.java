package org.codehaus.groovy.grails.web.plugins.support.exceptionResolver;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.AbstractGrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.web.errors.GrailsExceptionResolver;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

/**
 * <p>Plugin that registers {@link org.codehaus.groovy.grails.web.errors.GrailsExceptionResolver}.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class GrailsExceptionResolverPlugin extends AbstractGrailsPlugin implements GrailsPlugin {
    public GrailsExceptionResolverPlugin(Class pluginClass, GrailsApplication application) {
		super(pluginClass, application);
	}

	public void doWithApplicationContext(ApplicationContext applicationContext) {
    	if(applicationContext instanceof GenericApplicationContext) {
    		GenericApplicationContext ctx = (GenericApplicationContext)applicationContext;
		
	        RootBeanDefinition bd = new RootBeanDefinition(GrailsExceptionResolver.class);
	        MutablePropertyValues mpv = new MutablePropertyValues();
	        mpv.addPropertyValue("exceptionMappings", "java.lang.Exception=error");
	        bd.setPropertyValues(mpv);
	
	        ctx.registerBeanDefinition("exceptionHandler", bd);
    	}
    }

	public void doWithRuntimeConfiguration(RuntimeSpringConfiguration springConfig) {
		// do nothing
		
	}
}
