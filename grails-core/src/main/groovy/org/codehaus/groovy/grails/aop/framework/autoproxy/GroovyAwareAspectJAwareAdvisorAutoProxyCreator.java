package org.codehaus.groovy.grails.aop.framework.autoproxy;

import groovy.lang.GroovyObject;
import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;

/**
 * Enables AspectJ weaving from the application context.
 *
 * @author Graeme Rocher
 * @since 1.3.4
 */
public class GroovyAwareAspectJAwareAdvisorAutoProxyCreator extends AnnotationAwareAspectJAutoProxyCreator {

    private static final long serialVersionUID = 1;

    @Override
    protected boolean shouldProxyTargetClass(Class<?> beanClass, String beanName) {
        return GroovyObject.class.isAssignableFrom(beanClass) || super.shouldProxyTargetClass(beanClass, beanName);
    }
}
