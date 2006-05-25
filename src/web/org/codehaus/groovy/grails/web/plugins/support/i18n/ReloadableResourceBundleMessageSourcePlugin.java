package org.codehaus.groovy.grails.web.plugins.support.i18n;

import org.codehaus.groovy.grails.plugins.support.OrderedAdapter;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.MutablePropertyValues;

/**
 * <p>Plugin that registers {@link ReloadableResourceBundleMessageSource}.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class ReloadableResourceBundleMessageSourcePlugin extends OrderedAdapter implements GrailsPlugin {
    public void doWithGenericApplicationContext(GenericApplicationContext applicationContext, GrailsApplication application) {
        RootBeanDefinition bd = new RootBeanDefinition(ReloadableResourceBundleMessageSource.class);
        MutablePropertyValues mvp = new MutablePropertyValues();
        mvp.addPropertyValue("basename", "WEB-INF/grails-app/i18n/messages");
        bd.setPropertyValues(mvp);

        applicationContext.registerBeanDefinition("messageSource", bd);
    }
}
