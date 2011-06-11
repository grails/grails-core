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
import groovy.lang.MetaClassRegistry;
import groovy.lang.MetaClassRegistryChangeEvent;
import groovy.lang.MetaClassRegistryChangeEventListener;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Allows clean-up of changes made to the MetaClassRegistry.
 *
 * @author Graeme Rocher
 * @since 1.4
 */
@SuppressWarnings("rawtypes")
public class MetaClassRegistryCleaner implements MetaClassRegistryChangeEventListener {

    private Collection<Class> alteredClasses = new ConcurrentLinkedQueue<Class>();

    public void updateConstantMetaClass(MetaClassRegistryChangeEvent cmcu) {
        alteredClasses.add(cmcu.getClassToUpdate());
    }

    public void clean() {
        MetaClassRegistry registry = GroovySystem.getMetaClassRegistry();
        for (Class cls : alteredClasses) {
            registry.removeMetaClass(cls);
        }
        alteredClasses.clear();
    }
}
