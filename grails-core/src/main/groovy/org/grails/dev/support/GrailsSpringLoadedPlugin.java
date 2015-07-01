package org.grails.dev.support;

import grails.plugins.GrailsPluginManager;
import org.grails.core.util.ClassPropertyFetcher;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.util.Assert;
import org.springsource.loaded.Plugin;
import org.springsource.loaded.Plugins;
import org.springsource.loaded.ReloadEventProcessorPlugin;
import org.springsource.loaded.agent.*;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.List;

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
        org.grails.beans.support.CachedIntrospectionResults.clearClassLoader(clazz.getClassLoader());
        ClassPropertyFetcher.clearClassPropertyFetcherCache();
        Introspector.flushFromCaches(clazz);

        pluginManager.informOfClassChange(clazz);
    }

    private static boolean unregistered = false;
    public static void unregister() {
        List<Plugin> globalPlugins = new ArrayList<Plugin>(SpringLoadedPreProcessor.getGlobalPlugins());
        for (Plugin globalPlugin : globalPlugins) {
            Plugins.unregisterGlobalPlugin(globalPlugin);
        }
        unregistered = true;
    }

    public static GrailsSpringLoadedPlugin register(GrailsPluginManager pluginManager) {
        Assert.notNull(pluginManager, "Argument pluginManager cannot be null");
        GrailsSpringLoadedPlugin plugin = new GrailsSpringLoadedPlugin(pluginManager);
        Plugins.registerGlobalPlugin(plugin);
        if(unregistered) {
            Plugins.registerGlobalPlugin( new JVMPlugin() );
            Plugins.registerGlobalPlugin( new SpringPlugin() );
            Plugins.registerGlobalPlugin( new GroovyPlugin() );
            Plugins.registerGlobalPlugin( new CglibPlugin() );
        }
        return plugin;
    }
}
