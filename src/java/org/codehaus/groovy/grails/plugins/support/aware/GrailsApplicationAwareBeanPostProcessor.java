package org.codehaus.groovy.grails.plugins.support.aware;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.support.BeanPostProcessorAdapter;
import org.springframework.beans.BeansException;

/**
 * <p>Implementation of {@link org.springframework.beans.factory.config.BeanPostProcessor}
 * that recognizes {@link org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware} and injects and instance of
 * {@link GrailsApplication}.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class GrailsApplicationAwareBeanPostProcessor extends BeanPostProcessorAdapter {
    private GrailsApplication grailsApplication;

    public GrailsApplicationAwareBeanPostProcessor(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        processAwareInterfaces(grailsApplication,bean);
        return bean;
    }

    public static void processAwareInterfaces(GrailsApplication grailsApplication, Object bean) {
        if (bean instanceof GrailsApplicationAware) {
            ((GrailsApplicationAware)bean).setGrailsApplication(grailsApplication);
        }
        if(bean instanceof GrailsConfigurationAware) {
            ((GrailsConfigurationAware)bean).setConfiguration(grailsApplication.getConfig());
        }
    }

}
