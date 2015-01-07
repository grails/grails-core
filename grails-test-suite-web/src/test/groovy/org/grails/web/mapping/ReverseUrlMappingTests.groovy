package org.grails.web.mapping

import org.grails.web.taglib.AbstractGrailsTagTests

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class ReverseUrlMappingTests extends AbstractGrailsTagTests {

    protected void onSetUp() {
        gcl.parseClass '''
public class CustomUrlMappings {
static mappings = {
"/$mslug/$controller/$action/$id?" {}

"/controller_name/$mslug/action_name/$nslug" {
    controller = "controller_name"
    action = "action_name"
}

"/controller_name/$mslug/action_name/$nslug/$oslug" {
    controller = "controller_name"
    action = "action_name"
}

name myNamedMapping: '/people/list' {
  controller = 'person'
  action = 'list'
}

name myOtherNamedMapping: "/showPeople/$lastName" {
  controller = 'person'
  action = 'byLastName'
}

name showBooks: '/showSomeBooks' {
    controller = 'book'
    action = 'list'
}

name showBooks2: '/showSomeOtherBooks' {
    controller = 'book'
    action = 'list'
}

name showBooksWithAction: "/showSomeOtherBooks/$action" {
    controller = 'book'
}

"/$namespace/$controller/$action?"()

"/grails/$controller/$action?" {
    namespace = "grails"
}

"/invokePrimaryController" {
    controller = 'namespaced'
    namespace = 'primary'
}

"/invokeSecondaryController" {
    controller = 'namespaced'
    namespace = 'secondary'
}

"/nonNamespacedController/$action?" {
    controller = 'namespaced'
}

}}'''

        gcl.parseClass '''
class ProductController {
    def create = {}
    def save = {}
}
'''
    }

    void testLinkTagRendering() {
        def template1 = '<g:link controller="product" action="create" params="[mslug:mslug]">New Product</g:link>'
        assertOutputEquals '<a href="/acme/product/create">New Product</a>', template1, [mslug:"acme"]

        def template2 = '<g:link controller="controller_name" action="action_name" params="[mslug:mslug,nslug:nslug]">New Product</g:link>'
        assertOutputEquals '<a href="/controller_name/acme/action_name/Coyote">New Product</a>', template2, [mslug:"acme",nslug:"Coyote"]

        def template3 = '<g:link controller="controller_name" action="action_name" params="[mslug:mslug,nslug:nslug,extra:extra]">New Product</g:link>'
        assertOutputEquals '<a href="/controller_name/acme/action_name/Coyote?extra=RoadRunner">New Product</a>', template3, [mslug:"acme",nslug:"Coyote",extra:"RoadRunner"]

        def template4 = '<g:link controller="controller_name" action="action_name" params="[mslug:mslug,extra:extra,nslug:nslug]">New Product</g:link>'
        assertOutputEquals '<a href="/controller_name/acme/action_name/Coyote?extra=RoadRunner">New Product</a>', template4, [mslug:"acme",nslug:"Coyote",extra:"RoadRunner"]

        def template5 = '<g:link controller="controller_name" action="action_name" params="[mslug:mslug,nslug:nslug,oslug:oslug]">New Product</g:link>'
        assertOutputEquals '<a href="/controller_name/acme/action_name/Coyote/RoadRunner">New Product</a>', template5, [mslug:"acme",nslug:"Coyote",oslug:"RoadRunner"]

        def template6 = '<g:link mapping="myNamedMapping">List People</g:link>'
        assertOutputEquals '<a href="/people/list">List People</a>', template6, [:]

        def template7 = '<g:link mapping="myOtherNamedMapping" params="[lastName:\'Keenan\']">List People</g:link>'
        assertOutputEquals '<a href="/showPeople/Keenan">List People</a>', template7, [:]

        def template8 = '<g:link controller="namespaced" namespace="primary">Link To Primary</g:link>'
        assertOutputEquals '<a href="/invokePrimaryController">Link To Primary</a>', template8, [:]

        def template9 = '<g:link controller="namespaced" namespace="secondary">Link To Secondary</g:link>'
        assertOutputEquals '<a href="/invokeSecondaryController">Link To Secondary</a>', template9, [:]

        def template10 = '<g:link controller="namespaced">Link To Non Namespaced</g:link>'
        assertOutputEquals '<a href="/nonNamespacedController">Link To Non Namespaced</a>', template10, [:]
    }

    void testPaginateWithNamedUrlMapping() {
        def template = '<g:paginate mapping="showBooks" total="15" max="5" />'
        assertOutputEquals '<span class="currentStep">1</span><a href="/showSomeBooks?offset=5&amp;max=5" class="step">2</a><a href="/showSomeBooks?offset=10&amp;max=5" class="step">3</a><a href="/showSomeBooks?offset=5&amp;max=5" class="nextLink">Next</a>', template
    }

    void testSortableColumnWithNamedUrlMapping() {
        webRequest.controllerName = 'book'

        def template1 = '<g:sortableColumn property="releaseDate" title="Release Date" mapping="showBooks2" />'
        assertOutputEquals '<th class="sortable" ><a href="/showSomeOtherBooks?sort=releaseDate&amp;order=asc">Release Date</a></th>', template1

        def template2 = '<g:sortableColumn property="releaseDate" title="Release Date" mapping="showBooksWithAction" action="action_name"/>'
        assertOutputEquals '<th class="sortable" ><a href="/showSomeOtherBooks/action_name?sort=releaseDate&amp;order=asc">Release Date</a></th>', template2
    }

    void testSortableColumnWithNamespaceAttribute() {
        webRequest.controllerName = 'book'

        def template = '<g:sortableColumn property="id" title="ID" action="index" namespace="grails" />'
        assertOutputEquals '<th class="sortable" ><a href="/grails/book/index?sort=id&amp;order=asc">ID</a></th>', template
    }
}
