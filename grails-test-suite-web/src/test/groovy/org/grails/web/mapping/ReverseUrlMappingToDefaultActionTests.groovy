package org.grails.web.mapping

import org.grails.web.taglib.AbstractGrailsTagTests

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class ReverseUrlMappingToDefaultActionTests extends AbstractGrailsTagTests {

    protected void onSetUp() {
        gcl.parseClass '''
class UrlMappings {
    static mappings = {
            "/$id?"{
                controller = "reverseUrlMappingContent"
                action = "view"
            }

            "/$dir/$id?"{
                controller = "reverseUrlMappingContent"
                action = "view"
            }
    }
}'''

        gcl.parseClass '''
@grails.artefact.Artefact("Controller")
class ReverseUrlMappingContentController {
    def view = {}
}
@grails.artefact.Artefact("Controller")
class ReverseUrlMappingTestController {
    def foo = {}
    def index = {}
}
'''
    }

    void testLinkTagRendering() {

        def template = '<g:link url="[controller:\'reverseUrlMappingContent\', params:[dir:\'about\'], id:\'index\']">click</g:link>'
        assertOutputEquals '<a href="/about/index">click</a>', template
    }
}
