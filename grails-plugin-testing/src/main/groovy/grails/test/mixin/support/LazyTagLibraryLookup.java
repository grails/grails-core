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

import grails.core.GrailsTagLibClass;
import groovy.lang.GroovyObject;
import org.grails.core.gsp.DefaultGrailsTagLibClass;
import org.grails.plugins.web.GroovyPagesGrailsPlugin;
import org.grails.taglib.TagLibraryLookup;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lazy implementation of the tag library lookup class designed for testing purposes.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class LazyTagLibraryLookup extends TagLibraryLookup {
    List<Class> tagLibClasses = (List<Class>) new GroovyPagesGrailsPlugin().getProvidedArtefacts();
    private Map<String, GrailsTagLibClass> lazyLoadableTagLibs = new HashMap<>();

    @Override
    protected void registerTagLibraries() {
        super.registerTagLibraries();
        for (Class providedArtefact : tagLibClasses) {
            registerLazyLoadableTagLibClass(providedArtefact);
        }
    }

    public void registerLazyLoadableTagLibClass(Class tagLibClass) {
        GrailsTagLibClass grailsTagLibClass = new DefaultGrailsTagLibClass(tagLibClass);
        if (!hasNamespace(grailsTagLibClass.getNamespace())) {
            registerNamespaceDispatcher(grailsTagLibClass.getNamespace());
        }
        for (String tagName : grailsTagLibClass.getTagNames()) {
            String tagKey = tagNameKey(grailsTagLibClass.getNamespace(), tagName);
            lazyLoadableTagLibs.put(tagKey, grailsTagLibClass);
        }
    }

    @Override
    public GroovyObject lookupTagLibrary(String namespace, String tagName) {
        GroovyObject tagLibrary = super.lookupTagLibrary(namespace, tagName);
        if (tagLibrary == null) {
            String tagKey = tagNameKey(namespace, tagName);
            GrailsTagLibClass taglibClass = lazyLoadableTagLibs.get(tagKey);
            if (taglibClass != null) {
                if (!applicationContext.containsBean(taglibClass.getFullName())) {
                    GenericBeanDefinition bd = new GenericBeanDefinition();
                    bd.setBeanClass(taglibClass.getClazz());
                    bd.setAutowireCandidate(true);
                    bd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_NAME);
                    ((GenericApplicationContext) applicationContext).getDefaultListableBeanFactory().registerBeanDefinition(taglibClass.getFullName(), bd);
                }
                registerTagLib(taglibClass);
                tagLibrary = super.lookupTagLibrary(namespace, tagName);
            }
        }
        return tagLibrary;
    }

    protected String tagNameKey(String namespace, String tagName) {
        return namespace + ':' + tagName;
    }

    @Override
    protected void putTagLib(Map<String, Object> tags, String name, GrailsTagLibClass taglib) {
        if (applicationContext.containsBean(taglib.getFullName())) {
            super.putTagLib(tags, name, taglib);
        }
    }
}