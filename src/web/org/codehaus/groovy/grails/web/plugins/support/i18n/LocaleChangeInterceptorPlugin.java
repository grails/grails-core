package org.codehaus.groovy.grails.web.plugins.support.i18n;

import org.codehaus.groovy.grails.plugins.support.OrderedAdapter;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * <p>Plugin that registers {@link LocaleChangeInterceptor}.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class LocaleChangeInterceptorPlugin extends OrderedAdapter implements GrailsPlugin {
    public void doWithGenericApplicationContext(GenericApplicationContext applicationContext, GrailsApplication application) {
        RootBeanDefinition bd = new RootBeanDefinition(LocaleChangeInterceptor.class);
        MutablePropertyValues mvp = new MutablePropertyValues();
        mvp.addPropertyValue("paramName", "lang");
        bd.setPropertyValues(mvp);

        applicationContext.registerBeanDefinition("localeChangeInterceptor", bd);
    }
}
