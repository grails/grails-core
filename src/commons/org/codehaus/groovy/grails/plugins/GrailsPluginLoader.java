package org.codehaus.groovy.grails.plugins;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.OrderComparator;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>This class loads all <code>META-INF/GrailsPlugins.xml</code> files located on
 * the classpath in a Spring {@link ApplicationContext}.
 *
 * @author Steven Devijver
 * @since 0.2
 * @see GrailsPlugin
 */
public class GrailsPluginLoader {
    private static Log log = LogFactory.getLog(GrailsPluginLoader.class);

    /**
     * <p>This methods loads all resources using the <code>resourcePath</code> arguments into a Spring
     * {@link ApplicationContext}. Next all beans that implement in this {@link ApplicationContext} instance
     * that implement the {@link GrailsPlugin} interface are retrieved and the
     * {@link GrailsPlugin#doWithApplicationContext(org.springframework.context.support.GenericApplicationContext,org.codehaus.groovy.grails.commons.GrailsApplication)}
     * method is called once per bean.</b>
     *
     * <p>{@link GrailsPlugin} instances may implement the {@link org.springframework.core.Ordered}
     * interface in which case the objects are sorted before being called.
     *
     * @param applicationContext the {@link BeanDefinitionRegistry} that receives the {@link org.springframework.beans.factory.config.BeanDefinition}s
     * @param grailsApplication the {@link GrailsApplication} object that is passed to the {@link GrailsPlugin} objects
     * @param resourcePath the resource location path that will be loaded, e.g. <ocde>classpath*:META-INF/GrailsPlugin.xml</code>
     * @throws IOException in case the loading of the XML files fail
     */
    public static final void loadPlugins(GenericApplicationContext applicationContext, GrailsApplication grailsApplication, String resourcePath) throws IOException {
        Resource[] pluginResources = new PathMatchingResourcePatternResolver().getResources(resourcePath);

        if (log.isWarnEnabled()) {
            if (pluginResources.length == 0) {
                log.warn("No Grails plugin resources found!");
            }
        }

        ApplicationContext pluginContext = createApplicationContext(pluginResources);

        List plugins = getPlugins(pluginContext);

        Collections.sort(plugins, new OrderComparator());

        for (int i = 0; i < plugins.size(); i++) {
            GrailsPlugin plugin = (GrailsPlugin) plugins.get(i);
            plugin.doWithApplicationContext(applicationContext);
        }
    }

    protected static final List getPlugins(ApplicationContext applicationContext) {
         return new ArrayList(applicationContext.getBeansOfType(GrailsPlugin.class).values());
    }

    protected static final ApplicationContext createApplicationContext(Resource[] pluginResources) {
        GenericApplicationContext applicationContext = new GenericApplicationContext();
        BeanDefinitionReader bdr = new XmlBeanDefinitionReader(applicationContext);
        bdr.loadBeanDefinitions(pluginResources);
        applicationContext.refresh();
        return applicationContext;
    }
}
