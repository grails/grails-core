package org.grails.web.util

import groovy.transform.CompileStatic

import grails.util.GrailsClassUtils
import grails.core.GrailsTagLibClass
import org.grails.web.pages.GroovyPage
import org.grails.web.pages.TagLibraryLookup
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.reflection.CachedMethod
import org.codehaus.groovy.runtime.metaclass.MethodSelectionException
import org.springframework.context.ApplicationContext

class TagLibraryMetaUtils {
    // used for testing (GroovyPageUnitTestMixin.mockTagLib) and "nonEnhancedTagLibClasses" in GroovyPagesGrailsPlugin
    @CompileStatic
    static void enhanceTagLibMetaClass(final GrailsTagLibClass taglib, TagLibraryLookup gspTagLibraryLookup) {
        final MetaClass mc = taglib.getMetaClass()
        final String namespace = taglib.namespace ?: GroovyPage.DEFAULT_NAMESPACE
        registerTagMetaMethods(mc, gspTagLibraryLookup, namespace)
        registerNamespaceMetaProperties(mc, gspTagLibraryLookup)
    }

    @CompileStatic
    static void registerNamespaceMetaProperties(MetaClass mc, TagLibraryLookup gspTagLibraryLookup) {
        for(String ns : gspTagLibraryLookup.getAvailableNamespaces()) {
            registerNamespaceMetaProperty(mc, gspTagLibraryLookup, ns)
        }
    }
    
    @CompileStatic
    static void registerNamespaceMetaProperty(MetaClass metaClass, TagLibraryLookup gspTagLibraryLookup, String namespace) {
        if(!metaClass.hasProperty(namespace) && !doesMethodExist(metaClass, GrailsClassUtils.getGetterName(namespace), [] as Class[])) {
            registerPropertyMissingForTag(metaClass, namespace, gspTagLibraryLookup.lookupNamespaceDispatcher(namespace))
        }
    }

    @CompileStatic
    static registerMethodMissingForTags(MetaClass metaClass, TagLibraryLookup gspTagLibraryLookup, String namespace, String name, boolean addAll=true, boolean overrideMethods=true) {
        GroovyObject mc = (GroovyObject)metaClass;
        
        if(overrideMethods || !doesMethodExist(metaClass, name, [Map, Closure] as Class[])) {
            mc.setProperty(name) {Map attrs, Closure body ->
                GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, attrs, body, GrailsWebRequest.lookup())
            }
        }
        if(overrideMethods || !doesMethodExist(metaClass, name, [Map, CharSequence] as Class[])) {
            mc.setProperty(name) {Map attrs, CharSequence body ->
                GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, attrs, new GroovyPage.ConstantClosure(body), GrailsWebRequest.lookup())
            }
        }
        if(overrideMethods || !doesMethodExist(metaClass, name, [Map] as Class[])) {
            mc.setProperty(name) {Map attrs ->
                GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, attrs, null, GrailsWebRequest.lookup())
            }
        }
        if (addAll) {
            if(overrideMethods || !doesMethodExist(metaClass, name, [Closure] as Class[])) {
                mc.setProperty(name) {Closure body ->
                    GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, [:], body, GrailsWebRequest.lookup())
                }
            }
            if(overrideMethods || !doesMethodExist(metaClass, name, [] as Class[])) {
                mc.setProperty(name) {->
                    GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, [:], null, GrailsWebRequest.lookup())
                }
            }
        }
    }

    static registerMethodMissingForTags(MetaClass mc, ApplicationContext ctx,
                                        GrailsTagLibClass tagLibraryClass, String name) {
        TagLibraryLookup gspTagLibraryLookup = ctx.getBean("gspTagLibraryLookup")
        String namespace = tagLibraryClass.namespace ?: GroovyPage.DEFAULT_NAMESPACE
        registerMethodMissingForTags(mc, gspTagLibraryLookup, namespace, name)
    }

    @CompileStatic
    static void registerPropertyMissingForTag(MetaClass metaClass, String name, Object result) {
        GroovyObject mc = (GroovyObject)metaClass;
        mc.setProperty(GrailsClassUtils.getGetterName(name)) {-> result }
    }
    
    @CompileStatic
    static void registerTagMetaMethods(MetaClass emc, TagLibraryLookup lookup, String namespace) {
        for(String tagName : lookup.getAvailableTags(namespace)) {
            boolean addAll = !(namespace == GroovyPage.DEFAULT_NAMESPACE && tagName == 'hasErrors')
            registerMethodMissingForTags(emc, lookup, namespace, tagName, addAll, false)
        }
        if (namespace != GroovyPage.DEFAULT_NAMESPACE) {
            registerTagMetaMethods(emc, lookup, GroovyPage.DEFAULT_NAMESPACE)
        }
    }
    
    @CompileStatic
    protected static boolean doesMethodExist(final MetaClass mc, final String methodName, final Class[] parameterTypes, boolean staticScope=false, boolean onlyReal=false) {
        boolean methodExists = false
        try {
            MetaMethod existingMethod = mc.pickMethod(methodName, parameterTypes)
            if(existingMethod && existingMethod.isStatic()==staticScope && (!onlyReal || isRealMethod(existingMethod)) && parameterTypes.length==existingMethod.parameterTypes.length)  {
                methodExists = true
            }
        } catch (MethodSelectionException mse) {
            // the metamethod already exists with multiple signatures, must check if the exact method exists
            methodExists = mc.methods.contains { MetaMethod existingMethod ->
                existingMethod.name == methodName && existingMethod.isStatic()==staticScope && (!onlyReal || isRealMethod(existingMethod)) && ((!parameterTypes && !existingMethod.parameterTypes) || parameterTypes==existingMethod.parameterTypes)
            }
        }
    }
        
    @CompileStatic
    private static boolean isRealMethod(MetaMethod existingMethod) {
        existingMethod instanceof CachedMethod
    }
}
