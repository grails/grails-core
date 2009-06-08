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
package org.codehaus.groovy.grails.documentation;

import groovy.lang.*;
import org.codehaus.groovy.reflection.ClassInfo;

/**
 * Used to enable the Metadata generating EMC creation handle
 *
 * @author Graeme Rocher
 * @since 1.2
 */
public class MetadataGeneratingMetaClassCreationHandle extends ExpandoMetaClassCreationHandle {

    private static final MetadataGeneratingMetaClassCreationHandle instance = new MetadataGeneratingMetaClassCreationHandle();

    /* (non-Javadoc)
	 * @see groovy.lang.MetaClassRegistry.MetaClassCreationHandle#create(java.lang.Class, groovy.lang.MetaClassRegistry)
	 */
	protected MetaClass createNormalMetaClass(Class theClass, MetaClassRegistry registry) {
		if(theClass != MetadataGeneratingExpandoMetaClass.class
           && theClass != ExpandoMetaClass.class
           && theClass != DocumentationContext.class
           && theClass != DocumentedMethod.class
           && theClass != DocumentedProperty.class
           && theClass != DocumentedElement.class
           && theClass != DocumentationContextThreadLocal.class
           && !Closure.class.isAssignableFrom(theClass)) {
			return new MetadataGeneratingExpandoMetaClass(theClass);
		}
		else {
			return super.createNormalMetaClass(theClass, registry);
		}
	}

    /**
     * Registers a modified ExpandoMetaClass with the creation handle
     *
     * @param emc The EMC
     */
    public void registerModifiedMetaClass(ExpandoMetaClass emc) {
        final Class klazz = emc.getJavaClass();
        GroovySystem.getMetaClassRegistry().setMetaClass(klazz,emc);
    }

	public boolean hasModifiedMetaClass(ExpandoMetaClass emc) {
		return emc.getClassInfo().getModifiedExpando() != null;
	}

    /**
     * <p>Enables the ExpandoMetaClassCreationHandle with the registry
     *
     * <code>ExpandoMetaClassCreationHandle.enable();</code>
     *
     */
    public static void enable() {
        final MetaClassRegistry metaClassRegistry = GroovySystem.getMetaClassRegistry();
        if (metaClassRegistry.getMetaClassCreationHandler() != instance) {
            ClassInfo.clearModifiedExpandos();
            metaClassRegistry.setMetaClassCreationHandle(instance);
        }
    }

    public static void disable() {
        final MetaClassRegistry metaClassRegistry = GroovySystem.getMetaClassRegistry();
        if (metaClassRegistry.getMetaClassCreationHandler() == instance) {
            ClassInfo.clearModifiedExpandos();
            metaClassRegistry.setMetaClassCreationHandle(new MetaClassRegistry.MetaClassCreationHandle());
        }
    }
}
