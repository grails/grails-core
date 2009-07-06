package org.codehaus.groovy.grails.plugins.support.aware;

import org.springframework.beans.BeansException;
import org.codehaus.groovy.grails.plugins.support.aware.ClassLoaderAware;
import org.codehaus.groovy.grails.plugins.support.BeanPostProcessorAdapter;

public class ClassLoaderAwareBeanPostProcessor extends BeanPostProcessorAdapter {
    private ClassLoader classLoader;

    public ClassLoaderAwareBeanPostProcessor(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ClassLoaderAware) {
            ((ClassLoaderAware)bean).setClassLoader(classLoader);
        }
        return bean;
    }
}
