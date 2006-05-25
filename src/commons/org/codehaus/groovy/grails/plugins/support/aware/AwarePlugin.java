package org.codehaus.groovy.grails.plugins.support.aware;

import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.plugins.support.OrderedAdapter;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.context.support.GenericApplicationContext;

/**
 * <p>Grails plugin that registers <code>*Aware</code> {@link org.springframework.beans.factory.config.BeanPostProcessor}s.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class AwarePlugin extends OrderedAdapter implements GrailsPlugin {

    public void doWithGenericApplicationContext(GenericApplicationContext applicationContext, GrailsApplication application) {
        registerGrailsApplicationAwareBeanPostProcessor(applicationContext, application);
        registerClassLoaderAwareBeanPostProcessor(applicationContext, application.getClassLoader());
    }

    protected void registerGrailsApplicationAwareBeanPostProcessor(GenericApplicationContext applicationContext, GrailsApplication grailsApplication) {
        applicationContext.getBeanFactory().addBeanPostProcessor(new GrailsApplicationAwareBeanPostProcessor(grailsApplication));
        applicationContext.getBeanFactory().ignoreDependencyInterface(GrailsApplicationAware.class);
    }

    protected void registerClassLoaderAwareBeanPostProcessor(GenericApplicationContext applicationContext, ClassLoader classLoader) {
        applicationContext.getBeanFactory().addBeanPostProcessor(new ClassLoaderAwareBeanPostProcessor(classLoader));
        applicationContext.getBeanFactory().ignoreDependencyInterface(ClassLoaderAware.class);
    }
}
