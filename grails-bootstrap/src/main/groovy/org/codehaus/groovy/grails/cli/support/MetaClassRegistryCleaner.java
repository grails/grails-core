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

import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import groovy.lang.MetaClassRegistryChangeEvent;
import groovy.lang.MetaClassRegistryChangeEventListener;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.codehaus.groovy.runtime.metaclass.MetaClassRegistryImpl;

/**
 * Allows clean-up of changes made to the MetaClassRegistry.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@SuppressWarnings("rawtypes")
public class MetaClassRegistryCleaner implements MetaClassRegistryChangeEventListener {

    private Map<Class, Object> alteredClasses = new ConcurrentHashMap<Class, Object> ();
    private Map<IdentityWeakReference, Object> alteredInstances = new ConcurrentHashMap<IdentityWeakReference, Object>();
    private static final Object NO_CUSTOM_METACLASS = new Object();
    private static boolean cleaning;

    public void updateConstantMetaClass(MetaClassRegistryChangeEvent cmcu) {
        if(!cleaning) {
            MetaClass oldMetaClass = cmcu.getOldMetaClass();
            Class classToUpdate = cmcu.getClassToUpdate();
            Object instanceToUpdate = cmcu.getInstance();
            if (instanceToUpdate == null) {
                updateMetaClassOfClass(oldMetaClass, classToUpdate);
            } else {
                updateMetaClassOfInstance(oldMetaClass, instanceToUpdate);
            }
        }
    }

    private void updateMetaClassOfInstance(MetaClass oldMetaClass, Object instanceToUpdate) {
        IdentityWeakReference key = new IdentityWeakReference(instanceToUpdate);
        if (oldMetaClass != null) {
            Object current = alteredInstances.get(key);
            if (current == null || current == NO_CUSTOM_METACLASS) {
                alteredInstances.put(key, oldMetaClass);
            }
        } else {
            alteredInstances.put(key, NO_CUSTOM_METACLASS);
        }
    }

    private void updateMetaClassOfClass(MetaClass oldMetaClass, Class classToUpdate) {
        if (oldMetaClass != null) {
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
        try {
            cleaning = true;
            MetaClassRegistryImpl registry = (MetaClassRegistryImpl) GroovySystem.getMetaClassRegistry();
            cleanMetaClassOfClass(registry);
            cleanMetaClassOfInstance(registry);
        } finally {
            cleaning = false;
        }
    }

    private void cleanMetaClassOfInstance(MetaClassRegistryImpl registry) {
        List<IdentityWeakReference> keys = new ArrayList<IdentityWeakReference>(alteredInstances.keySet());
        for (IdentityWeakReference key : keys) {
            Object instance = key.get();
            if (instance != null) {
                Object alteredMetaClass = alteredInstances.get(key);
                if (alteredMetaClass == NO_CUSTOM_METACLASS) {
                    alteredMetaClass = null;
                }
                registry.setMetaClass(instance, (MetaClass) alteredMetaClass);
            }
        }
        alteredInstances.clear();
    }

    private void cleanMetaClassOfClass(MetaClassRegistryImpl registry) {
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

    private static final class IdentityWeakReference extends WeakReference<Object> {

        private int hash;

        public IdentityWeakReference(Object referent) {
            super(referent);
            hash = System.identityHashCode(referent);
        }

        public int hashCode() {
            return hash;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            IdentityWeakReference ref = (IdentityWeakReference) obj;
            return this.get() == ref.get();
        }

    }

}
