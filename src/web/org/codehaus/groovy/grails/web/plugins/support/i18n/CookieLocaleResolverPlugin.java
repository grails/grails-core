package org.codehaus.groovy.grails.web.plugins.support.i18n;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.AbstractGrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

/**
 * <p>Plugin that registers {@link CookieLocaleResolver}.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class CookieLocaleResolverPlugin extends AbstractGrailsPlugin implements GrailsPlugin {
    public CookieLocaleResolverPlugin(Class pluginClass, GrailsApplication application) {
		super(pluginClass, application);
	}

	public void doWithApplicationContext(ApplicationContext applicationContext) {
    	if(applicationContext instanceof GenericApplicationContext) {
    		GenericApplicationContext ctx = (GenericApplicationContext)applicationContext;
		
    		RootBeanDefinition bd = new RootBeanDefinition(CookieLocaleResolver.class);

    		ctx.registerBeanDefinition("localeResolver", bd);
    	}
    }

	public void doWithRuntimeConfiguration(RuntimeSpringConfiguration springConfig) {
		// do nothing
		
	}
}
