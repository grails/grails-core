package org.codehaus.groovy.grails.web.plugins.support.multipartresolver;

import org.codehaus.groovy.grails.plugins.support.OrderedAdapter;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

/**
 * <p>Plugin that registers {@link CommonsMultipartResolver}.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class CommonsMultipartResolverPlugin extends OrderedAdapter implements GrailsPlugin {
    public void doWithGenericApplicationContext(GenericApplicationContext applicationContext, GrailsApplication application) {
        RootBeanDefinition bd = new RootBeanDefinition(CommonsMultipartResolver.class);

        applicationContext.registerBeanDefinition("multipartResolver", bd);
    }
}
