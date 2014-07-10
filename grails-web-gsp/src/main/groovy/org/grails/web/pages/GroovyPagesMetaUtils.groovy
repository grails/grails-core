/*
 * Copyright 2011 the original author or authors.
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
package org.grails.web.pages

import grails.util.Environment
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import grails.util.GrailsMetaClassUtils
import org.grails.web.util.TagLibraryMetaUtils

@CompileStatic
class GroovyPagesMetaUtils {
    private final static Object[] EMPTY_OBJECT_ARRAY = new Object[0]

    static void registerMethodMissingForGSP(Class gspClass, TagLibraryLookup gspTagLibraryLookup) {
        registerMethodMissingForGSP(GrailsMetaClassUtils.getExpandoMetaClass(gspClass), gspTagLibraryLookup)
    }

    static void registerMethodMissingForGSP(final MetaClass emc, final TagLibraryLookup gspTagLibraryLookup) {
        if(gspTagLibraryLookup==null) return
        final boolean addMethodsToMetaClass = !Environment.isDevelopmentMode()

        GroovyObject mc = (GroovyObject)emc
        synchronized(emc) {
            mc.setProperty("methodMissing", { String name, Object args ->
                methodMissingForTagLib(emc, emc.getTheClass(), gspTagLibraryLookup, GroovyPage.DEFAULT_NAMESPACE, name, args, addMethodsToMetaClass)
            })
        }
        TagLibraryMetaUtils.registerTagMetaMethods(emc, gspTagLibraryLookup, GroovyPage.DEFAULT_NAMESPACE)
        TagLibraryMetaUtils.registerNamespaceMetaProperties(emc, gspTagLibraryLookup)
    }

    private static Object[] makeObjectArray(Object args) {
        args instanceof Object[] ? (Object[])args : [args] as Object[]
    }

    @CompileStatic(TypeCheckingMode.SKIP) // workaround for GROOVY-6147 bug
    static Object methodMissingForTagLib(MetaClass mc, Class type, TagLibraryLookup gspTagLibraryLookup, String namespace, String name, Object argsParam, boolean addMethodsToMetaClass) {
        Object[] args = makeObjectArray(argsParam)
        final GroovyObject tagBean = gspTagLibraryLookup.lookupTagLibrary(namespace, name)
        if (tagBean != null) {
            MetaClass tagBeanMc = tagBean.getMetaClass()
            final MetaMethod method=tagBeanMc.respondsTo(tagBean, name, args).find{ it }
            if (method != null) {
                if (addMethodsToMetaClass) {
                    // add all methods with the same name to metaclass at once to prevent "wrong number of arguments" exception
                    for (MetaMethod m in tagBeanMc.respondsTo(tagBean, name)) {
                        addTagLibMethodToMetaClass(tagBean, m, mc)
                    }
                }
                return method.invoke(tagBean, args)
            }
        }
        throw new MissingMethodException(name, type, args)
    }

    static addTagLibMethodToMetaClass(final GroovyObject tagBean, final MetaMethod method, final MetaClass mc) {
        Class[] paramTypes = method.nativeParameterTypes
        Closure methodMissingClosure = null
        switch(paramTypes.length) {
            case 0:
                methodMissingClosure = {->
                    method.invoke(tagBean, EMPTY_OBJECT_ARRAY)
                }
                break
            case 1:
                if (paramTypes[0] == Closure) {
                    methodMissingClosure = { Closure body ->
                        method.invoke(tagBean, body)
                    }
                } else if (paramTypes[0]==Map) {
                    methodMissingClosure = { Map attrs->
                        method.invoke(tagBean, attrs)
                    }
                } else {
                    methodMissingClosure = { Object attrs->
                        method.invoke(tagBean, attrs)
                    }
                }
                break
            case 2:
                if (paramTypes[0] == Map) {
                    if (paramTypes[1] == Closure) {
                        methodMissingClosure = { Map attrs, Closure body ->
                            method.invoke(tagBean, attrs, body)
                        }
                    } else if (paramTypes[1] == CharSequence) {
                        methodMissingClosure = { Map attrs, CharSequence body ->
                            method.invoke(tagBean, attrs, body)
                        }
                    } else if (paramTypes[1] == String) {
                        methodMissingClosure = { Map attrs, String body ->
                            method.invoke(tagBean, attrs, body)
                        }
                    } else {
                        methodMissingClosure = { Map attrs, Object body ->
                            method.invoke(tagBean, attrs, body)
                        }
                    }
                }
                break
        }
        if (methodMissingClosure != null) {
            synchronized(mc) {
                ((GroovyObject)mc).setProperty(method.name, methodMissingClosure)
            }
        }
    }
}
