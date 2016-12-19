package org.grails.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.beans.PropertyDescriptor;

/**
 * Adds timings of bean creation times logged to the "org.grails.startup" group
 *
 * @author Graeme Rocher
 * @since 3.0.12
 */
public class BeanCreationProfilingPostProcessor implements InstantiationAwareBeanPostProcessor, ApplicationListener<ContextRefreshedEvent> {

    private final StopWatch stopWatch = new StopWatch("Bean Creation StopWatch");
    private static final Logger LOG = LoggerFactory.getLogger("org.grails.startup");

    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        stopWatch.start("Create Bean: " + beanName);
        return null;
    }

    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
        return true;
    }

    @Override
    public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {
        return pvs;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        stopWatch.stop();
        return bean;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        stopWatch.complete();
        if(LOG.isDebugEnabled()) {
            LOG.debug(stopWatch.prettyPrint());
        }
    }
}
