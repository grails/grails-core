package org.codehaus.groovy.grails.web.mapping

import org.codehaus.groovy.grails.web.taglib.AbstractGrailsTagTests

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: May 30, 2008
 */
class ReverseUrlMappingToDefaultActionTests extends AbstractGrailsTagTests{
    public void onSetUp() {
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
class ContentController {
    def view = {}
}
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