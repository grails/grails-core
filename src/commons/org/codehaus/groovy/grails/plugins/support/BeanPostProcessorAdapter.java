package org.codehaus.groovy.grails.plugins.support;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.BeansException;

/**
 * <p>Adapter implementation of {@link BeanPostProcessor}.
 *
 * @author Steven Devijver
 * @since 0.2
 * @see BeanPostProcessor
 */
public class BeanPostProcessorAdapter implements BeanPostProcessor {
    /**
     *
     * @param bean
     * @param beanName
     * @return The specified bean
     * @throws BeansException
     * @see BeanPostProcessor#postProcessBeforeInitialization(Object, String)
     */
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     *
     * @param bean
     * @param beanName
     * @return The specified bean
     * @throws BeansException
     * @see BeanPostProcessor#postProcessAfterInitialization(Object, String)
     */
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
