package org.codehaus.groovy.grails.web.plugins.support.i18n;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.AbstractGrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * <p>Plugin that registers {@link LocaleChangeInterceptor}.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class LocaleChangeInterceptorPlugin extends AbstractGrailsPlugin implements GrailsPlugin {
    public LocaleChangeInterceptorPlugin(GrailsApplication application) {
		super(LocaleChangeInterceptorPlugin.class, application);
	}

	public void doWithApplicationContext(ApplicationContext applicationContext) {
    	if(applicationContext instanceof GenericApplicationContext) {
    		GenericApplicationContext ctx = (GenericApplicationContext)applicationContext;
    	
	        RootBeanDefinition bd = new RootBeanDefinition(LocaleChangeInterceptor.class);
	        MutablePropertyValues mvp = new MutablePropertyValues();
	        mvp.addPropertyValue("paramName", "lang");
	        bd.setPropertyValues(mvp);
	
	        ctx.registerBeanDefinition("localeChangeInterceptor", bd);
    	}
    }

	public void doWithRuntimeConfiguration(RuntimeSpringConfiguration springConfig) {
		// do nothing		
	}
}
