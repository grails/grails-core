package org.codehaus.groovy.grails.web.plugins.support.i18n;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.AbstractGrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * <p>Plugin that registers {@link ReloadableResourceBundleMessageSource}.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class ReloadableResourceBundleMessageSourcePlugin extends AbstractGrailsPlugin implements GrailsPlugin {
    public ReloadableResourceBundleMessageSourcePlugin(GrailsApplication application) {
		super(ReloadableResourceBundleMessageSourcePlugin.class, application);
	}

	public void doWithApplicationContext(ApplicationContext applicationContext) {
    	if(applicationContext instanceof GenericApplicationContext) {
    		GenericApplicationContext ctx = (GenericApplicationContext)applicationContext;
    	
	        RootBeanDefinition bd = new RootBeanDefinition(ReloadableResourceBundleMessageSource.class);
	        MutablePropertyValues mvp = new MutablePropertyValues();
	        mvp.addPropertyValue("basename", "WEB-INF/grails-app/i18n/messages");
	        bd.setPropertyValues(mvp);
	
	        
	        ctx.registerBeanDefinition("messageSource", bd);
    	}
    }

	public void doWithRuntimeConfiguration(RuntimeSpringConfiguration springConfig) {
		// do nothing		
	}
}
