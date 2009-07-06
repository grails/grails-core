/*
 * Copyright 2004-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.groovy.grails.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Implementation of <code>PluginFilter</code> which removes that all of the supplied
 * plugins (identified by name) as well as their dependencies are omitted from the 
 * filtered plugin list
 * @author Phil Zoio
 */
public class ExcludingPluginFilter extends BasePluginFilter {

	public ExcludingPluginFilter(Set excluded) {
		super(excluded);
	}

	public ExcludingPluginFilter(String[] excluded) {
		super(excluded);
	}

	protected List getPluginList(List original, List pluginList) {

		// go through and remove ones that don't apply
		List newList = new ArrayList(original);
		for (Iterator iter = newList.iterator(); iter.hasNext();) {
			GrailsPlugin element = (GrailsPlugin) iter.next();

			// remove the excluded dependencies
			if (pluginList.contains(element)) {
				iter.remove();
			}
		}

		return newList;
	}

	protected void addPluginDependencies(List additionalList,
			GrailsPlugin plugin) {
		// find the plugins which depend on the one we've excluded

		String pluginName = plugin.getName();

		Collection values = getAllPlugins();
		Iterator others = values.iterator();
		while (others.hasNext()) {
			GrailsPlugin p = (GrailsPlugin) others.next();

			String pName = p.getName();

			// ignore the current plugin
			if (pluginName.equals(pName)) {
				continue;
			}

			boolean depends = isDependentOn(p, pluginName);

			if (depends) {
				registerDependency(additionalList, p);
			}
		}
	}

}
