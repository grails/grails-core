/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.commons;

import groovy.lang.Closure;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Default implementation of a tag lib class.
 *
 * @author Graeme Rocher
 */
public class DefaultGrailsTagLibClass extends AbstractInjectableGrailsClass implements GrailsTagLibClass {

    protected static final String TAG_LIB = "TagLib";

    private Set<String> tags = new HashSet<String>();
    private String namespace = GrailsTagLibClass.DEFAULT_NAMESPACE;
    private Set<String> returnObjectForTagsSet = new HashSet<String>();

    /**
     * Default contructor.
     *
     * @param clazz        the tag library class
     */
    @SuppressWarnings("rawtypes")
    public DefaultGrailsTagLibClass(Class<?> clazz) {
        super(clazz, TAG_LIB);

        for (PropertyDescriptor prop : getPropertyDescriptors()) {
            Method readMethod = prop.getReadMethod();
            if (readMethod == null || Modifier.isStatic(readMethod.getModifiers())) {
                continue;
            }

            if (Closure.class.isAssignableFrom(prop.getPropertyType())) {
                tags.add(prop.getName());
            }
        }

        String ns = getStaticPropertyValue(NAMESPACE_FIELD_NAME, String.class);
        if (ns != null && !"".equals(ns.trim())) {
            namespace = ns.trim();
        }

        List returnObjectForTagsList = getStaticPropertyValue(RETURN_OBJECT_FOR_TAGS_FIELD_NAME, List.class);
        if (returnObjectForTagsList != null) {
            for (Object tagName : returnObjectForTagsList) {
                returnObjectForTagsSet.add(String.valueOf(tagName));
            }
        }
    }

    public boolean hasTag(String tagName) {
        return tags.contains(tagName);
    }

    public Set<String> getTagNames() {
        return tags;
    }

    public String getNamespace() {
        return namespace;
    }

    public Set<String> getTagNamesThatReturnObject() {
        return returnObjectForTagsSet;
    }
}
