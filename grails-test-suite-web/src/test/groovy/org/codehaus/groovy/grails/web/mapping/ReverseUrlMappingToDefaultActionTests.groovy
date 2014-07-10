package org.codehaus.groovy.grails.web.mapping

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
                controller = "content"
                action = "view"
            }

            "/$dir/$id?"{
                controller = "content"
                action = "view"
            }
    }
}'''

        gcl.parseClass '''
@grails.artefact.Artefact("Controller")
class ContentController {
    def view = {}
}
@grails.artefact.Artefact("Controller")
class TestController {
    def foo = {}
    def index = {}
}
'''
    }

    void testLinkTagRendering() {

        def template = '<g:link url="[controller:\'content\', params:[dir:\'about\'], id:\'index\']">click</g:link>'
        assertOutputEquals '<a href="/about/index">click</a>', template
    }
}
