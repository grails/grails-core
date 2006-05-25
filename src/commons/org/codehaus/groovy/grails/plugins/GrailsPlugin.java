package org.codehaus.groovy.grails.plugins;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.support.GenericApplicationContext;
import org.codehaus.groovy.grails.commons.GrailsApplication;

/**
 * <p>Plugin interface that adds Spring {@link org.springframework.beans.factory.config.BeanDefinition}s
 * to a registry based on a {@link GrailsApplication} object. After all <code>GrailsPlugin</code> classes
 * have been processed the {@link org.springframework.beans.factory.config.BeanDefinition}s in the registry are
 * loaded in a Spring {@link org.springframework.context.ApplicationContext} that's the singular
 * configuration unit of Grails applications.</p>
 *
 * <p>It's up to implementation classes to determine where <code>GrailsPlugin</code> instances are loaded
 * from.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 * @see BeanDefinitionRegistry
 */
public interface GrailsPlugin {

    /**
     * <p>This method is called to allow the plugin to add {@link org.springframework.beans.factory.config.BeanDefinition}s
     * to the {@link BeanDefinitionRegistry}.</p>
     *
     * @param applicationContext
     * @param application the {@link org.codehaus.groovy.grails.commons.GrailsApplication} the loaded the Groovy source files
     */
    void doWithGenericApplicationContext(GenericApplicationContext applicationContext, GrailsApplication application);
}
