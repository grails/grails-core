package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib
import org.springframework.mock.web.MockHttpServletResponse

class ApplicationTagLibResourcesTests extends AbstractGrailsTagTests {

    void onInitMockBeans() {
        grailsApplication.parentContext.registerMockBean('grailsResourceProcessor', [something:'value'])
    }

    def replaceMetaClass(Object o) {
        def old = o.metaClass

        // Create a new EMC for the class and attach it.
        def emc = new ExpandoMetaClass(o.class, true, true)
        emc.initialize()
        o.metaClass = emc

        return old
    }

    void testResourceTagDirOnlyWithResourcesHooks() {
        request.contextPath = '/test'
        def template = '${resource(dir:"jquery")}'

        def taglib = appCtx.getBean(ApplicationTagLib.name)
        taglib.hasResourceProcessor = true
        def oldMC = replaceMetaClass(taglib)

        // Dummy r.resource impl
        def mockRes = [
            resource: { attrs -> "WRONG"}
        ]
        taglib.metaClass.getR = { -> mockRes }
        try {
            assertOutputEquals '/test/jquery', template
        } finally {
            taglib.metaClass = oldMC
        }
    }

    void testResourceTagDirAndFileWithResourcesHooks() {
        request.contextPath = '/test'
        def template = '${resource(dir:"jquery", file:"jqtest.js")}'

        def taglib = appCtx.getBean(ApplicationTagLib.class.name)
        taglib.hasResourceProcessor = true
        def oldMC = replaceMetaClass(taglib)

        // Dummy r.resource impl
        def mockRes = [
            resource: { attrs -> "RESOURCES:${attrs.dir}/${attrs.file}" }
        ]
        taglib.metaClass.getR = { -> mockRes }
        try {
            assertOutputEquals 'RESOURCES:jquery/jqtest.js', template
        } finally {
            taglib.metaClass = oldMC
        }
    }
}
