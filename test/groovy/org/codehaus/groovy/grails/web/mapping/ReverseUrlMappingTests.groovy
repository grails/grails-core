package org.codehaus.groovy.grails.web.mapping

import org.codehaus.groovy.grails.web.taglib.AbstractGrailsTagTests

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Mar 19, 2008
 */
class ReverseUrlMappingTests extends AbstractGrailsTagTests{
    public void onSetUp() {
        gcl.parseClass '''
public class CustomUrlMappings {
static mappings = {
"/$mslug/$controller/$action/$id?" {
}
}
}'''

        gcl.parseClass '''
class ProductController {
    def create = {}
    def save = {}
}
'''
    }


    void testLinkTagRendering() {
        def template = '<g:link controller="product" action="create" params="[mslug:mslug]">New Product</g:link>'

        assertOutputEquals '<a href="/acme/product/create">New Product</a>', template, [mslug:"acme"]
    }


}