/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.web.pages;

import groovy.lang.GroovyObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.commons.GrailsTagLibClass;
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.codehaus.groovy.grails.web.taglib.NamespacedTagDispatcher;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * A class that is look-up tag library instances
 *
 * @author Graeme Rocher
 * @since 1.1
 *        <p/>
 *        Created: Jan 5, 2009
 */
public class TagLibraryLookup implements ApplicationContextAware, GrailsApplicationAware, InitializingBean{
    private ApplicationContext applicationContext;
    private GrailsApplication grailsApplication;
    private Map<String, String> tagLibraries = new HashMap<String, String>();
    private Map<String, NamespacedTagDispatcher> namespaceDispatchers = new HashMap<String, NamespacedTagDispatcher>();
    private Set<String> tagsThatReturnObject = new HashSet<String>();

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    public void afterPropertiesSet() throws Exception {
        if(grailsApplication != null && applicationContext!=null) {
            GrailsClass[] taglibs =  grailsApplication.getArtefacts(TagLibArtefactHandler.TYPE);
            for (GrailsClass grailsClass : taglibs) {
                registerTagLib((GrailsTagLibClass)grailsClass);
            }
            namespaceDispatchers.put(GroovyPage.TEMPLATE_NAMESPACE, new NamespacedTagDispatcher(GroovyPage.DEFAULT_NAMESPACE, GroovyPage.class, grailsApplication, this) {
                @Override
                public Object invokeMethod(final String name, Object args) {

                    Map attrs = new HashMap() {{
                       put("template", name);
                    }};
                    if(args != null && args instanceof Object[]) {
                       Object[] tagArgs = ((Object[])args);
                        if(tagArgs.length>0 && tagArgs[0] instanceof Map) {
                            attrs.put("model", tagArgs[0]);                            
                        }
                    }

                    return super.invokeMethod("render", new Object[]{attrs});
                }
            });

        }

    }

    /**
     * Registers a tag library for lookup. Each of the tags in the
     * library is mapped by namespace:name to the taglib bean. If the
     * taglib has already been registered, this method will override
     * the existing information and update the tags to use the new
     * version.
     * @param taglib The taglib descriptor class.
     */
    public void registerTagLib(GrailsTagLibClass taglib) {
        String namespace = taglib.getNamespace();
        namespaceDispatchers.put(namespace, new NamespacedTagDispatcher(namespace, GroovyPage.class, grailsApplication, this));
        for(String tagName : taglib.getTagNames()) {
        	String nameKey=tagNameKey(namespace, tagName);
            tagLibraries.put(nameKey, taglib.getFullName());
            tagsThatReturnObject.remove(nameKey);
        }
        for(String tagName : taglib.getTagNamesThatReturnObject()) {
        	String nameKey=tagNameKey(namespace, tagName);
        	tagsThatReturnObject.add(nameKey);
        }
    }

    /**
     * Looks up a tag library for the given namespace and tag name
     *
     * @param namespace The tag library namespace
     * @param tagName The tag name
     * @return The tag library or null if it wasn't found
     */
    public GroovyObject lookupTagLibrary(String namespace, String tagName) {
    	String fullName = tagLibraries.get(tagNameKey(namespace, tagName));
    	if(fullName != null) {
    		return (GroovyObject) applicationContext.getBean(fullName);
    	} else {
    		return null;
    	}
    }

	private String tagNameKey(String namespace, String tagName) {
		return namespace+':'+tagName;
	}
    
    public boolean doesTagReturnObject(String namespace, String tagName) {
    	return tagsThatReturnObject.contains(tagNameKey(namespace, tagName));
    }

    /**
     * Looks up a namespace dispatcher for the given namespace
     * @param namespace The namespace
     * @return The NamespacedTagDispatcher
     */
    public NamespacedTagDispatcher lookupNamespaceDispatcher(String namespace) {
        return namespaceDispatchers.get(namespace);
    }

    /**
     * Returns whether the given namespace is in use
     * @param namespace The namespace
     * @return True if it is in use
     */
    public boolean hasNamespace(String namespace) {
        return namespaceDispatchers.containsKey(namespace);
    }

    /**
     * @return The namespaces available
     */
    public Set<String> getAvailableNamespaces() {
        return namespaceDispatchers.keySet();
    }
}

