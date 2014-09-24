package org.grails.dev.support;

import grails.plugins.GrailsPluginManager;
import org.grails.core.util.ClassPropertyFetcher;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.util.Assert;
import org.springsource.loaded.Plugins;
import org.springsource.loaded.ReloadEventProcessorPlugin;

import java.beans.Introspector;

/**
 * A Spring loaded plugin
 *
 * @author Graeme Rocher
 * @since 3.0
 */
public class GrailsSpringLoadedPlugin implements ReloadEventProcessorPlugin {

    GrailsPluginManager pluginManager;

    private GrailsSpringLoadedPlugin(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @Override
    public boolean shouldRerunStaticInitializer(String typename, Class<?> clazz, String encodedTimestamp) {
        return true;
    }

    @Override
    public void reloadEvent(String typename, Class<?> clazz, String encodedTimestamp) {
        CachedIntrospectionResults.clearClassLoader(clazz.getClassLoader());
        ClassPropertyFetcher.clearClassPropertyFetcherCache();
        Introspector.flushFromCaches(clazz);

        pluginManager.informOfClassChange(clazz);
    }

    public static void register(GrailsPluginManager pluginManager) {
        Assert.notNull(pluginManager, "Argument pluginManager cannot be null");
        Plugins.registerGlobalPlugin(new GrailsSpringLoadedPlugin(pluginManager));
    }
}
