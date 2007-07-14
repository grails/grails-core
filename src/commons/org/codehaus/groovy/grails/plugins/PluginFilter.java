package org.codehaus.groovy.grails.plugins;

import java.util.List;

/**
 * Defines interface for obtaining a sublist of <code>GrailsPlugin</code> instances
 * based on an original supplied list of <code>GrailsPlugin</code> instances
 * @author Phil Zoio
 */
public interface PluginFilter {

	/**
	 * Returns a filtered list of plugins
	 * @param original the original supplied set of <code>GrailsPlugin</code> instances
	 * @return a sublist of these items
	 */
	List filterPluginList(List original);
}
