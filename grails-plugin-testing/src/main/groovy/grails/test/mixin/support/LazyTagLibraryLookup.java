/*
 * Copyright 2011 SpringSource
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
package grails.test.mixin.support;

import groovy.lang.GroovyObject;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsTagLibClass;
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler;
import org.codehaus.groovy.grails.plugins.web.GroovyPagesGrailsPlugin;
import org.codehaus.groovy.grails.web.pages.GroovyPage;
import org.codehaus.groovy.grails.web.pages.TagLibraryLookup;
import org.codehaus.groovy.grails.web.taglib.NamespacedTagDispatcher;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lazy implementation of the tag library lookup class designed for testing purposes
 *
 * @author Graeme Rocher
 * @since 1.4
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class LazyTagLibraryLookup extends TagLibraryLookup{

    private Map<String, String> resolveTagLibraries = new HashMap<String, String>();
    private Map<String, List<Class>> tagClassesByNamespace = new HashMap<String, List<Class>>();
    private Map<Class, GrailsTagLibClass> tagClassToTagLibMap= new HashMap<Class, GrailsTagLibClass>();

    @Override
    protected void registerTagLibraries() {
        List<Class> providedArtefacts = (List<Class>) new GroovyPagesGrailsPlugin().getProvidedArtefacts();

        tagClassesByNamespace.put(GrailsTagLibClass.DEFAULT_NAMESPACE, new ArrayList<Class>());
        namespaceDispatchers.put(GrailsTagLibClass.DEFAULT_NAMESPACE, new NamespacedTagDispatcher(GrailsTagLibClass.DEFAULT_NAMESPACE, GroovyPage.class, grailsApplication, this));
        for (Class providedArtefact : providedArtefacts) {
            if (!grailsApplication.isArtefactOfType(TagLibArtefactHandler.TYPE, providedArtefact)) continue;
            Object value = GrailsClassUtils.getStaticPropertyValue(providedArtefact, GrailsTagLibClass.NAMESPACE_FIELD_NAME);
            if (value != null) {

                String namespace = value.toString();
                List<Class> classes = tagClassesByNamespace.get(namespace);
                if (classes == null) {
                    classes = new ArrayList<Class>();
                    tagClassesByNamespace.put(namespace, classes);
                }
                classes.add(providedArtefact);

                namespaceDispatchers.put(namespace, new NamespacedTagDispatcher(namespace, GroovyPage.class, grailsApplication, this));
            }
            else {
                tagClassesByNamespace.get(GrailsTagLibClass.DEFAULT_NAMESPACE).add(providedArtefact);
            }

        }

    }

    @Override
    public GroovyObject lookupTagLibrary(String namespace, String tagName) {

        String tagKey = tagNameKey(namespace, tagName);
        if (resolveTagLibraries.containsKey(tagKey)) {
            return applicationContext.getBean(resolveTagLibraries.get(tagKey), GroovyObject.class);
        }

        List<Class> tagLibraryClasses = tagClassesByNamespace.get(namespace);
        if (tagLibraryClasses != null) {
            for (Class tagLibraryClass : tagLibraryClasses) {

                GrailsTagLibClass tagLib = tagClassToTagLibMap.get(tagLibraryClass);
                if (tagLib == null) {
                    tagLib = (GrailsTagLibClass) grailsApplication.addArtefact(TagLibArtefactHandler.TYPE, tagLibraryClass);
                    tagClassToTagLibMap.put(tagLibraryClass, tagLib);
                }
                String tagLibraryClassName = tagLibraryClass.getName();
                if (tagLib == null || !tagLib.hasTag(tagName)) {
                    continue;
                }

                if (!applicationContext.containsBean(tagLibraryClassName)) {
                    registerTagLib(tagLib);
                    if (tagLib.hasTag(tagName)) {
                        GenericBeanDefinition bd = new GenericBeanDefinition();
                        bd.setBeanClass(tagLibraryClass);
                        bd.setAutowireCandidate(true);
                        bd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_NAME);
                        ((GenericApplicationContext)applicationContext).getDefaultListableBeanFactory().registerBeanDefinition(tagLibraryClassName, bd);
                        resolveTagLibraries.put(tagKey, tagLib.getFullName());
                        return (GroovyObject) applicationContext.getBean(tagLibraryClassName);
                    }
                }
            }
        }

        return null;
    }

    @Override
    protected void putTagLib(String nameKey, GrailsTagLibClass taglib){
        tagLibraries.put(nameKey, taglib.getFullName());
    }

    @Override
    public void registerTagLib(GrailsTagLibClass taglib) {

        super.registerTagLib(taglib);

        String ns = taglib.getNamespace();

        for (String tagName : taglib.getTagNames()) {
            String key = tagNameKey(ns, tagName);
            resolveTagLibraries.put(key, taglib.getFullName());
        }
    }
}
