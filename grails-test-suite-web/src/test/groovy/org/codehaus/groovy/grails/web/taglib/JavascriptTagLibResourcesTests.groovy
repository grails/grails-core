package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.plugins.web.taglib.JavascriptTagLib

class JavascriptTagLibResourcesTests extends AbstractGrailsTagTests {

    def replaceMetaClass(o) {
        def old = o.metaClass

        // Create a new EMC for the class and attach it.
        def emc = new ExpandoMetaClass(o.getClass(), true, true)
        emc.initialize()
        o.metaClass = emc

        return old
    }

    void onInitMockBeans() {
        grailsApplication.parentContext.registerMockBean('grailsResourceProcessor', [something:'value'])
    }

    /**
     * Tests that the INCLUDED_JS_LIBRARIES attribute is set correctly without resources plugin
     */
    void testLibraryAttributeSetWhenResourcesPluginInstalled() {
        def template = '<g:javascript library="testing"/>'

        def taglib = appCtx.getBean(JavascriptTagLib.name)
        taglib.hasResourceProcessor = true
        def oldMC = replaceMetaClass(taglib)

        def requiredModule

        // Dummy r.resource impl
        def mockRes = [
            require: { attrs -> requiredModule = attrs.module; return '' }
        ]
        taglib.metaClass.getR = { -> mockRes }
        try {

            def result = applyTemplate(template, [:])

            println "Result: $result"
            assertEquals(['testing'], request.getAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES"))
            assertEquals 0, result.trim().size()
            assertEquals 'testing', requiredModule

        } finally {
            taglib.metaClass = oldMC
        }
    }

    /**
     * Tests that inline script blocks delegate to r:script
     */
    void testScriptTagIsUsedWhenResourcesPluginInstalled() {
        def template = '<g:javascript>var i = 999;</g:javascript>'

        def taglib = appCtx.getBean(JavascriptTagLib.name)
        taglib.hasResourceProcessor = true
        def oldMC = replaceMetaClass(taglib)

        def scriptFrag

        // Dummy r.script impl
        def mockRes = [
            script: { attrs, body -> scriptFrag = body(); return '' }
        ]
        taglib.metaClass.getR = { -> mockRes }
        try {

            def result = applyTemplate(template, [:])

            assertEquals 'var i = 999;', scriptFrag

        } finally {
            taglib.metaClass = oldMC
        }
    }
}
