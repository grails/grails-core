package org.codehaus.groovy.grails.plugins;

import groovy.util.ConfigObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Implements mechanism for figuring out what <code>PluginFilter</code>
 * implementation to use based on a set of provided configuration properties.
 *
 * @author Phil Zoio
 * @author Graeme Rocher
 */
public class PluginFilterRetriever {

    @SuppressWarnings("rawtypes")
    public PluginFilter getPluginFilter(Map properties) {

        Assert.notNull(properties);
        if (properties instanceof ConfigObject) {
            properties = ((ConfigObject)properties).flatten();
        }
        Object includes = properties.get("grails.plugin.includes");
        if (includes == null) properties.get("plugin.includes");
        Object excludes = properties.get("grails.plugin.excludes");
        if (excludes == null) properties.get("plugin.excludes");

        return getPluginFilter(includes, excludes);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    PluginFilter getPluginFilter(Object includes, Object excludes) {
        PluginFilter pluginFilter = null;

        if (includes != null) {
            if (includes instanceof Collection) {
                pluginFilter = new IncludingPluginFilter(new HashSet((Collection)includes));
            }
            else {
                String[] includesArray = StringUtils.commaDelimitedListToStringArray(includes.toString());
                pluginFilter = new IncludingPluginFilter(includesArray);
            }
        }
        else if (excludes != null) {
            if (excludes instanceof Collection) {
                pluginFilter = new ExcludingPluginFilter(new HashSet((Collection)includes));
            }
            else {
                String[] excludesArray = StringUtils.commaDelimitedListToStringArray(excludes.toString());
                pluginFilter = new ExcludingPluginFilter(excludesArray);
            }
        }
        else {
            pluginFilter = new IdentityPluginFilter();
        }
        return pluginFilter;
    }
}
