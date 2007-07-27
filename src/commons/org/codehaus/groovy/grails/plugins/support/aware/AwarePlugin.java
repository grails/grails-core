package org.codehaus.groovy.grails.plugins.support.aware;

import groovy.lang.GroovyObject;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.AbstractGrailsPlugin;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Collections;
import java.util.Map;

/**
 * <p>Grails plugin that registers <code>*Aware</code> {@link org.springframework.beans.factory.config.BeanPostProcessor}s.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class AwarePlugin extends AbstractGrailsPlugin {

    public AwarePlugin(Class pluginClass, GrailsApplication application) {
		super(pluginClass, application);
	}

	public void doWithApplicationContext(ApplicationContext applicationContext) {
        registerGrailsApplicationAwareBeanPostProcessor(applicationContext, application);
        registerClassLoaderAwareBeanPostProcessor(applicationContext, application.getClassLoader());
    }

    protected void registerGrailsApplicationAwareBeanPostProcessor(ApplicationContext applicationContext, GrailsApplication grailsApplication) {
    	if(applicationContext instanceof GenericApplicationContext) {
    		GenericApplicationContext ctx = (GenericApplicationContext)applicationContext;
            ctx.getBeanFactory().addBeanPostProcessor(new GrailsApplicationAwareBeanPostProcessor(grailsApplication));
            ctx.getBeanFactory().ignoreDependencyInterface(GrailsApplicationAware.class);
	
    	}
    }

    protected void registerClassLoaderAwareBeanPostProcessor(ApplicationContext applicationContext, ClassLoader classLoader) {
    	if(applicationContext instanceof GenericApplicationContext) {
    		GenericApplicationContext ctx = (GenericApplicationContext)applicationContext;    	
    		ctx.getBeanFactory().addBeanPostProcessor(new ClassLoaderAwareBeanPostProcessor(classLoader));
    		ctx.getBeanFactory().ignoreDependencyInterface(ClassLoaderAware.class);
    	}
    }


	public void doWithRuntimeConfiguration(RuntimeSpringConfiguration springConfig) {
		// nothing
		
	}

    public GroovyObject getInstance() {
        throw new UnsupportedOperationException("Method [getInstance] not supported on AwarePlugin");
    }

    public void doWithDynamicMethods(ApplicationContext applicationContext) {
		// do nothing
	}

    public boolean isEnabled() {
        return true;
    }

    public String[] getObservedPluginNames() {
        return new String[0];  
    }

    public void notifyOfEvent(Map event) {
        // do nothing
    }

    public Map notifyOfEvent(int eventKind, Object source) {
        return Collections.EMPTY_MAP;
    }

    public void doArtefactConfiguration() {
		// do nothing		
	}

    public Class[] getProvidedArtefacts() {
        return new Class[0];
    }

}
