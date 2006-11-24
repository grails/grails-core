package org.codehaus.groovy.grails.web.plugins.support.multipartresolver;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.AbstractGrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

/**
 * <p>Plugin that registers {@link CommonsMultipartResolver}.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class CommonsMultipartResolverPlugin extends AbstractGrailsPlugin implements GrailsPlugin {
    public CommonsMultipartResolverPlugin(Class pluginClass, GrailsApplication application) {
		super(pluginClass, application);
	}

	public void doWithApplicationContext(ApplicationContext applicationContext) {
        RootBeanDefinition bd = new RootBeanDefinition(CommonsMultipartResolver.class);
    	if(applicationContext instanceof GenericApplicationContext) {
    		GenericApplicationContext ctx = (GenericApplicationContext)applicationContext;

    		ctx.registerBeanDefinition("multipartResolver", bd);
    	}
    }

	public void doWithRuntimeConfiguration(RuntimeSpringConfiguration springConfig) {
		// do nothing
	}
}
