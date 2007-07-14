package org.codehaus.groovy.grails.plugins;

import java.util.Properties;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Implements mechanism for figuring out what <code>PluginFilter</code>
 * implementation to use based on a set of provided configuration properties
 * @author Phil Zoio
 */
public class PluginFilterRetriever {

	public PluginFilter getPluginFilter(Properties properties) {

		Assert.notNull(properties);

		String includes = properties.getProperty("plugin.includes");
		String excludes = properties.getProperty("plugin.excludes");

		return getPluginFilter(includes, excludes);
	}

	PluginFilter getPluginFilter(String includes, String excludes) {
		PluginFilter pluginFilter = null;

		if (includes != null) {
			String[] includesArray = StringUtils
					.commaDelimitedListToStringArray(includes);
			pluginFilter = new IncludingPluginFilter(includesArray);
		} else if (excludes != null) {
			String[] excludesArray = StringUtils
					.commaDelimitedListToStringArray(excludes);
			pluginFilter = new ExcludingPluginFilter(excludesArray);
		} else {
			pluginFilter = new IdentityPluginFilter();
		}
		return pluginFilter;
	}
}
