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

import groovy.util.slurpersupport.GPathResult

/**
 * A Class that gather information about the behavior a plugin adds at runtime.
 *
 * @author Graeme Rocher
 * @since 1.2
 */

@Singleton
public class DocumentationContext {
    
    List<DocumentedMethod> methods = []
    List<DocumentedMethod> staticMethods = []
    List<DocumentedProperty> properties = []
    boolean active = false

    DocumentationContext documentMethod(String artefact, Class type, String name, Class[] arguments) {
        methods << new DocumentedMethod(name:name, arguments:arguments,type:type,artefact:artefact)
        return this
    }

    DocumentationContext documentStaticMethod(String artefact, Class type, String name, Class[] arguments) {
        staticMethods << new DocumentedMethod(name:name, arguments:arguments,type:type,artefact:artefact)
        return this
    }
    
    DocumentationContext documentProperty(String artefact, Class type, String name) {
        properties << new DocumentedProperty(artefact:artefact, type:type, name:name)
        return this
    }

}
class DocumentedMethod {
    String name
    Class[] arguments
    Class type
    String artefact

    public String toString() {
        return "${type.name}.${name}(${arguments*.name.join(',')})"
    }


}
class DocumentedProperty {
    String name
    Class type = Object
    String artefact

    public String toString() {
        return "${type.name}.${name}"
    }
}