package org.codehaus.groovy.grails.web.plugins.support.exceptionResolver;

import org.codehaus.groovy.grails.plugins.support.OrderedAdapter;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.web.errors.GrailsExceptionResolver;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.MutablePropertyValues;

/**
 * <p>Plugin that registers {@link org.codehaus.groovy.grails.web.errors.GrailsExceptionResolver}.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class GrailsExceptionResolverPlugin extends OrderedAdapter implements GrailsPlugin {
    public void doWithGenericApplicationContext(GenericApplicationContext applicationContext, GrailsApplication application) {
        RootBeanDefinition bd = new RootBeanDefinition(GrailsExceptionResolver.class);
        MutablePropertyValues mpv = new MutablePropertyValues();
        mpv.addPropertyValue("exceptionMappings", "java.lang.Exception=error");
        bd.setPropertyValues(mpv);

        applicationContext.registerBeanDefinition("exceptionHandler", bd);
    }
}
