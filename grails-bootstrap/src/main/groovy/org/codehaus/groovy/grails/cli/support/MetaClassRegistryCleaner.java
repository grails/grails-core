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
package org.codehaus.groovy.grails.cli.support;

import groovy.lang.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Allows clean-up of changes made to the MetaClassRegistry.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@SuppressWarnings("rawtypes")
public class MetaClassRegistryCleaner implements MetaClassRegistryChangeEventListener {

    private Map<Class, Object> alteredClasses = new ConcurrentHashMap<Class, Object> ();
    private static final Object NO_CUSTOM_METACLASS = new Object();

    public void updateConstantMetaClass(MetaClassRegistryChangeEvent cmcu) {
        MetaClass oldMetaClass = cmcu.getOldMetaClass();
        Class classToUpdate = cmcu.getClassToUpdate();
        if(oldMetaClass != null) {
            Object current = alteredClasses.get(classToUpdate);
            if(current == null || current == NO_CUSTOM_METACLASS) {
                alteredClasses.put(classToUpdate, oldMetaClass);
            }
        }
        else {
            alteredClasses.put(classToUpdate, NO_CUSTOM_METACLASS);
        }
    }

    public void clean() {
        MetaClassRegistry registry = GroovySystem.getMetaClassRegistry();
        Set<Class> classes = new HashSet<Class>(alteredClasses.keySet());
        for (Class aClass : classes) {
            Object alteredMetaClass = alteredClasses.get(aClass);
            if(alteredMetaClass == NO_CUSTOM_METACLASS) {
                registry.removeMetaClass(aClass);
            }
            else {
                registry.setMetaClass(aClass, (MetaClass) alteredMetaClass);
            }
        }
        alteredClasses.clear();
    }
}
