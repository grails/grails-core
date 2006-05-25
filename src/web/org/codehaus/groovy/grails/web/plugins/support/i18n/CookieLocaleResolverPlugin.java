package org.codehaus.groovy.grails.web.plugins.support.i18n;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.plugins.support.OrderedAdapter;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

/**
 * <p>Plugin that registers {@link CookieLocaleResolver}.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class CookieLocaleResolverPlugin extends OrderedAdapter implements GrailsPlugin {
    public void doWithGenericApplicationContext(GenericApplicationContext applicationContext, GrailsApplication application) {
        RootBeanDefinition bd = new RootBeanDefinition(CookieLocaleResolver.class);

        applicationContext.registerBeanDefinition("localeResolver", bd);
    }
}
