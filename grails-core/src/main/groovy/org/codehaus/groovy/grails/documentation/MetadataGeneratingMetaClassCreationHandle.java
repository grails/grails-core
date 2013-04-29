/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.documentation;

import groovy.lang.Closure;
import groovy.lang.ExpandoMetaClass;
import groovy.lang.ExpandoMetaClassCreationHandle;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import groovy.lang.MetaClassRegistry;

/**
 * Used to enable the Metadata generating EMC creation handle.
 *
 * @author Graeme Rocher
 * @since 1.2
 */
public class MetadataGeneratingMetaClassCreationHandle extends ExpandoMetaClassCreationHandle {

    /* (non-Javadoc)
     * @see groovy.lang.MetaClassRegistry.MetaClassCreationHandle#create(java.lang.Class, groovy.lang.MetaClassRegistry)
     */
    @SuppressWarnings("rawtypes")
    @Override
    protected MetaClass createNormalMetaClass(Class theClass, MetaClassRegistry registry) {
        if (!isExcludedClass(theClass)) {
            return new MetadataGeneratingExpandoMetaClass(theClass);
        }

        return super.createNormalMetaClass(theClass, registry);
    }

    public static boolean isExcludedClass(Class<?> theClass) {
        return theClass == MetadataGeneratingExpandoMetaClass.class ||
               theClass == ExpandoMetaClass.class ||
               theClass == DocumentationContext.class ||
               theClass == DocumentedMethod.class ||
               theClass == DocumentedProperty.class ||
               theClass == DocumentedElement.class ||
               theClass == Boolean.class || Closure.class.isAssignableFrom(theClass);
    }

    /**
     * Registers a modified ExpandoMetaClass with the creation handle
     *
     * @param emc The EMC
     */
    @Override
    public void registerModifiedMetaClass(ExpandoMetaClass emc) {
        GroovySystem.getMetaClassRegistry().setMetaClass(emc.getJavaClass(), emc);
    }

    @Override
    public boolean hasModifiedMetaClass(ExpandoMetaClass emc) {
        return emc.getClassInfo().getModifiedExpando() != null;
    }

    /**
     * Enables the ExpandoMetaClassCreationHandle with the registry.
     *
     * <code>ExpandoMetaClassCreationHandle.enable();</code>
     *
     * @deprecated Dynamic document generation no longer supported
     */
    @Deprecated
    public static void enable() {
    }

    public static void disable() {
    }
}
