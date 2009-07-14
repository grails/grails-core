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

package org.codehaus.groovy.grails.documentation

import org.codehaus.groovy.grails.commons.GrailsClassUtils
import grails.util.Metadata

/**
 * A Class that gather information about the behavior a plugin adds at runtime.
 *
 * @author Graeme Rocher
 * @since 1.2
 */

public class DocumentationContext {

    private static DocumentationContextThreadLocal threadLocalDocumentContext;
    static {
        if(!Metadata.current.warDeployed) {
            threadLocalDocumentContext = new DocumentationContextThreadLocal()
        }
    }
    public static DocumentationContext getInstance() {        
        threadLocalDocumentContext?.get() 
    }

    String artefactType = "Unknown"
    String currentDocumentation
    List<DocumentedMethod> methods = []
    List<DocumentedMethod> staticMethods = []
    List<DocumentedProperty> properties = []
    private boolean active = false

    boolean hasMetadata() {
        !methods?.isEmpty() || !staticMethods?.isEmpty() || !properties?.isEmpty()
    }

    boolean isActive() {
        this.active
    }
    void setActive(boolean b) { this.active = b }

    void reset() {
        active = false
        artefactType = "Unknown"
    }

    /**
     * Stores documentation for the next method or property to be added
     */
    DocumentationContext document(String doc) {
        currentDocumentation = doc
        return this
    }

    /**
     * Documents an instance method
     */
    DocumentationContext documentMethod(String artefact, Class type, String name, Class[] arguments) {
        if(!currentDocumentation) {
            if(GrailsClassUtils.isGetter(name, arguments)) {
                currentDocumentation = properties.find { it.name == GrailsClassUtils.getPropertyForGetter(name) }?.text
            }
            if(GrailsClassUtils.isSetter(name, arguments)) {
                currentDocumentation = properties.find { it.name == GrailsClassUtils.getPropertyForSetter(name) }?.text
            }


        }
        methods << new DocumentedMethod(name:name, arguments:arguments,type:type,artefact:artefact, text:currentDocumentation)
        currentDocumentation = null
        return this
    }

    /**
     * Documents a static method
     */
    DocumentationContext documentStaticMethod(String artefact, Class type, String name, Class[] arguments) {
        staticMethods << new DocumentedMethod(name:name, arguments:arguments,type:type,artefact:artefact,text:currentDocumentation)
        currentDocumentation = null
        return this
    }



    /**
     * Documents a property
     */
    DocumentationContext documentProperty(String artefact, Class type, String name) {

        if(!currentDocumentation) {
            def getterOrSetter = methods.find {it.name == GrailsClassUtils.getGetterName(name) || it.name == GrailsClassUtils.getSetterName(name)}
            if(getterOrSetter && getterOrSetter.text) {
                currentDocumentation = getterOrSetter.text
            }
        }
        properties << new DocumentedProperty(artefact:artefact, type:type, name:name, text:currentDocumentation)
        currentDocumentation = null
        return this
    }

}
class DocumentationContextThreadLocal extends InheritableThreadLocal{

    protected Object initialValue() {
        return new DocumentationContext()
    }

}
class DocumentedElement {
    String name
    Class type
    String artefact
    String text
}
class DocumentedMethod extends DocumentedElement{
    Class[] arguments
    public String toString() {
        return "${type.name}.${name}(${arguments*.name.join(',')})"
    }


}
class DocumentedProperty extends DocumentedElement {
    Class type = Object
    public String toString() {
        return "${type.name}.${name}"
    }
}