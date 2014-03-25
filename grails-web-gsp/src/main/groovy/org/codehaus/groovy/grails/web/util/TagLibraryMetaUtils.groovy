package org.codehaus.groovy.grails.web.util

import grails.artefact.Enhanced
import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsTagLibClass
import org.codehaus.groovy.grails.web.pages.GroovyPage
import org.codehaus.groovy.grails.web.pages.TagLibraryLookup
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.context.ApplicationContext

class TagLibraryMetaUtils {
    // used for testing (GroovyPageUnitTestMixin.mockTagLib) and "nonEnhancedTagLibClasses" in GroovyPagesGrailsPlugin
    static void enhanceTagLibMetaClass(final GrailsTagLibClass taglib, TagLibraryLookup gspTagLibraryLookup) {
        if (!taglib.clazz.getAnnotation(Enhanced)) {
            final MetaClass mc = taglib.getMetaClass()
            final String namespace = taglib.namespace ?: GroovyPage.DEFAULT_NAMESPACE

            for (tag in taglib.tagNames) {
                registerMethodMissingForTags(mc, gspTagLibraryLookup, namespace, tag)
            }
            // propertyMissing and methodMissing are now added in MetaClassEnhancer / TagLibraryApi
        }
    }

    @CompileStatic
    static registerMethodMissingForTags(MetaClass metaClass, TagLibraryLookup gspTagLibraryLookup, String namespace, String name) {
        GroovyObject mc = (GroovyObject)metaClass;
        mc.setProperty(name) {Map attrs, Closure body ->
            GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, attrs, body, GrailsWebRequest.lookup())
        }
        mc.setProperty(name) {Map attrs, CharSequence body ->
            GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, attrs, new GroovyPage.ConstantClosure(body), GrailsWebRequest.lookup())
        }
        mc.setProperty(name) {Map attrs ->
            GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, attrs, null, GrailsWebRequest.lookup())
        }
        mc.setProperty(name) {Closure body ->
            GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, [:], body, GrailsWebRequest.lookup())
        }
        mc.setProperty(name) {->
            GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, [:], null, GrailsWebRequest.lookup())
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
}
