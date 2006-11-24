package org.codehaus.groovy.grails.plugins;

import java.math.BigDecimal;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;

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
 * @author Graeme Rocher
 * 
 * @since 0.2
 * @see BeanDefinitionRegistry
 */
public interface GrailsPlugin {

    String TRAILING_NAME = "GrailsPlugin";
	String VERSION = "version";
	String DO_WITH_SPRING = "doWithSpring";
	String DO_WITH_APPLICATION_CONTEXT = "doWithApplicationContext";
	String DEPENDS_ON = "dependsOn";


	/**
     * <p>This method is called to allow the plugin to add {@link org.springframework.beans.factory.config.BeanDefinition}s
     * to the {@link BeanDefinitionRegistry}.</p>
     *
     * @param applicationContext
     */
    void doWithApplicationContext(ApplicationContext applicationContext);
    
    
    /**
     * Executes the plugin code that performs runtime configuration as defined in the doWithSpring closure
     * 
     * @param springConfig The RuntimeSpringConfiguration instance
     */
    void doWithRuntimeConfiguration(RuntimeSpringConfiguration springConfig);

    
    /**
     * 
     * @return The name of the plug-in
     */
	String getName();


	/**
	 * 
	 * @return The version of the plug-in
	 */
	BigDecimal getVersion();


	/**
	 * 
	 * @return The names of the plugins this plugin is dependant on
	 */
	String[] getDependencyNames();


	/**
	 * The version of the specified dependency
	 * 
	 * @param name the name of the dependency
	 * @return The version
	 */
	BigDecimal getDependentVersion(String name);
}
