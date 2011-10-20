package org.codehaus.groovy.grails.web.taglib

import grails.util.GrailsUtil

import org.codehaus.groovy.grails.commons.UrlMappingsArtefactHandler
import org.codehaus.groovy.grails.plugins.web.taglib.JavascriptProvider
import org.codehaus.groovy.grails.plugins.web.taglib.JavascriptTagLib
import org.codehaus.groovy.grails.support.MockStringResourceLoader
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import org.springframework.web.util.WebUtils
import org.codehaus.groovy.grails.web.pages.GroovyPageBinding
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler

class JavascriptTagLibResourcesTests extends AbstractGrailsTagTests {

    def replaceMetaClass(Object o) {
        def old = o.metaClass

        // Create a new EMC for the class and attach it.
        def emc = new ExpandoMetaClass(o.class, true, true)
        emc.initialize()
        o.metaClass = emc
        
        return old
    }

    void onInitMockBeans() {
        grailsApplication.mainContext.registerMockBean('grailsResourceProcessor', [something:'value'])
    }

    /**
     * Tests that the INCLUDED_JS_LIBRARIES attribute is set correctly without resources plugin
     */
    void testLibraryAttributeSetWhenResourcesPluginInstalled() {
        def template = '<g:javascript library="testing"/>'

        def taglib = appCtx.getBean(JavascriptTagLib.class.name)
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

        def taglib = appCtx.getBean(JavascriptTagLib.class.name)
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
