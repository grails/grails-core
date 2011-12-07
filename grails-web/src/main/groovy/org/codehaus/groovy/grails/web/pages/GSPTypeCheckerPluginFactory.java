/*
 * Copyright 2003-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.web.pages;

import groovyjarjarasm.asm.Opcodes;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.grails.commons.*;
import org.codehaus.groovy.grails.commons.spring.GrailsApplicationContext;
import org.codehaus.groovy.transform.stc.TypeCheckerPluginFactory;
import org.codehaus.groovy.transform.stc.TypeCheckerPlugin;

import java.util.*;


public class GSPTypeCheckerPluginFactory implements TypeCheckerPluginFactory {
    public TypeCheckerPlugin getTypeCheckerPlugin(ClassNode node) {
        return new GSPPlugin();
    }
    
    private static class GSPPlugin extends TypeCheckerPlugin {
        private final static ClassNode EXCEPTION_TYPE = ClassHelper.make(Exception.class);
        private final static ClassNode GRAILSAPP_CN = ClassHelper.make(GrailsApplication.class);
        private final static ClassNode GRAILS_APPLICATION_CONTEXT = ClassHelper.make(GrailsApplicationContext.class);
        private static final Map<String, Class> GRAILS_APPL_LIST_TYPES = new HashMap<String, Class>() {{
            put("domainClasses", GrailsDomainClass.class);
            put("controllerClasses", GrailsControllerClass.class);
            put("serviceClasses", GrailsServiceClass.class);
            put("tagLibClasses", GrailsTagLibClass.class);
        }};

        public ClassNode resolveDynamicVariableType(DynamicVariable variable) {
            String name = variable.getName();
            if ("grailsApplication".equals(name)) return GRAILSAPP_CN;
            if ("applicationContext".equals(name)) return GRAILS_APPLICATION_CONTEXT;
            if ("JSP_TAGS".equals(name)) {
                // workaround for STC not finding the variable in Grails, though it works with a regular class...
                return ClassHelper.make(HashMap.class);
            }
            if ("exception".equals(name)) return EXCEPTION_TYPE;
            return super.resolveDynamicVariableType(variable);
        }
        
        public PropertyNode resolveProperty(ClassNode receiver, String propertyName) {
            if (receiver.isDerivedFrom(GRAILSAPP_CN)) {
                for (Map.Entry<String, Class> entry : GRAILS_APPL_LIST_TYPES.entrySet()) {
                    if (entry.getKey().equals(propertyName)) {
                        ClassNode listType = ClassHelper.LIST_TYPE.getPlainNodeReference();
                        listType.setGenericsTypes(new GenericsType[]{
                                new GenericsType(ClassHelper.make(entry.getValue()))
                        });
                        return new PropertyNode(propertyName, Opcodes.ACC_PUBLIC, listType, receiver, null, null, null);
                    }
                }
            }
            if (true) {
                // this is just to make an example work, but this should NEVER be done because returning a non-null
                // value on an unknown property changes the inferred type instead of throwing an error
                // FOR TEST PURPOSES ONLY
                return  new PropertyNode(propertyName, Opcodes.ACC_PUBLIC, ClassHelper.OBJECT_TYPE, receiver, null, null, null);
            }
            return super.resolveProperty(receiver, propertyName);
        }

        @Override
        public List<MethodNode> findMethod(final ClassNode receiver, final String name, final ClassNode... args) {
            if ("resource".equals(name)) {
                // FOR TEST PURPOSES ONLY
                MethodNode methodNode = new MethodNode(name, Opcodes.ACC_PUBLIC, ClassHelper.STRING_TYPE, toParameterArray(args), ClassNode.EMPTY_ARRAY, null);
                methodNode.setDeclaringClass(receiver);
                return Collections.singletonList(methodNode);
            }
            return super.findMethod(receiver, name, args);
        }
    }
}
