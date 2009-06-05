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

import org.springframework.core.io.Resource
import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import groovy.util.slurpersupport.Node

/**
 * A specialized version of ExpandoMetaClass that is capable of generating metadata about the dynamic methods and properties
 * that are added to artefact types at runtime by plugins
 *
 * A project with sufficient code coverage will have metadata about the methods and properties it provides generated at runtime.
 *
 * @author Graeme Rocher
 * @since 1.2
 */

public class MetadataGeneratingExpandoMetaClass extends ExpandoMetaClass{

    private String artefactType


    public MetadataGeneratingExpandoMetaClass(Class theClass, String artefactType) {
        super(theClass,true, true)
        this.artefactType = artefactType

    }


    public void addMetaBeanProperty(MetaBeanProperty mp) {
        super.addMetaBeanProperty(mp)

        DocumentationContext context = DocumentationContext.instance

        if(context.active) {
            context.documentProperty(artefactType,javaClass, mp.name)
        }

    }

    protected void registerStaticMethod(String name, Closure callable) {
        super.registerStaticMethod(name, callable);
        DocumentationContext context = DocumentationContext.instance
        if(context.active) {
               context.documentStaticMethod(artefactType,javaClass, name, callable.parameterTypes)
        }

    }



    public void registerInstanceMethod(MetaMethod method) {
        super.registerInstanceMethod(method);
        DocumentationContext context = DocumentationContext.instance
        if(context.active) {
            if(method.isStatic()) {
               context.documentStaticMethod(artefactType,javaClass, method.name, method.nativeParameterTypes)
            }
            else {
               context.documentMethod(artefactType,javaClass, method.name, method.nativeParameterTypes)
            }
        }

    }







}