package org.codehaus.groovy.grails.web.taglib;

import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.grails.commons.*

class LinkRenderingTagLibTests extends AbstractGrailsTagTests {

    protected void onInit() {
        def mappingClass = gcl.parseClass('''
class TestUrlMappings {
    static mappings = {
      "/$controller/$action?/$id?"{
          constraints {
             // apply constraints here
          }
      }

      "/products/$id" {
          controller = "test"
          action = "index"
      }
      "/surveys/$action?" {
          controller = "survey"
       }
        "/searchable" {
            controller = "searchable"
            action = "index"
        }
        "/searchable/$action?" {
            controller = "searchable"
        }

        "/dummy/$action/$name/$id"(controller: "test2")
    }
}
        ''')

        grailsApplication.addArtefact(UrlMappingsArtefactHandler.TYPE, mappingClass)
    }

    void testLinkTagWithAttributeValueContainingEqualSignFollowedByQuote() {
        //  Some of these tests look peculiar but they relate to
        //  scenarios that were broken before GRAILS-7229 was addressed

        def template = '''<g:link controller="demo" class="${(y == '5' && x == '4') ? 'A' : 'B'}" >demo</g:link>'''
        assertOutputEquals '<a href="/demo" class="B">demo</a>', template, [x: '7', x: '4']
        template = '''<g:link controller="demo" class="${(y == '5' && x == '4') ? 'A' : 'B'}" >demo</g:link>'''
        assertOutputEquals '<a href="/demo" class="A">demo</a>', template, [x: '4', y: '5']

        template = '''<g:link controller="demo" class='${(y == "5" && x == "5") ? "A" : "B"}' >demo</g:link>'''
        assertOutputEquals '<a href="/demo" class="B">demo</a>', template, [y: '0', x: '5']
        template = '''<g:link controller="demo" class='${(y == "5" && x == "5") ? "A" : "B"}' >demo</g:link>'''
        assertOutputEquals '<a href="/demo" class="A">demo</a>', template, [y: '5', x: '5']

        template = '''<g:link controller="demo" class="${(someVar == 'abcd')}" >demos</g:link>'''
        assertOutputEquals '<a href="/demo" class="false">demos</a>', template, [someVar: 'some value']
        template = '''<g:link controller="demo" class="${(someVar == 'abcd')}" >demos</g:link>'''
        assertOutputEquals '<a href="/demo" class="true">demos</a>', template, [someVar: 'abcd']

        template = '''<g:link controller="demo" class="${(someVar == 'abcd' )}" >demos</g:link>'''
        assertOutputEquals '<a href="/demo" class="false">demos</a>', template, [someVar: 'some value']
        template = '''<g:link controller="demo" class="${(someVar == 'abcd' )}" >demos</g:link>'''
        assertOutputEquals '<a href="/demo" class="true">demos</a>', template, [someVar: 'abcd']
    }

    void testOverlappingReverseMappings() {
        def template = '<g:link controller="searchable" action="index" >Search</g:link>'
        assertOutputEquals('<a href="/searchable">Search</a>', template)

        template = '<g:link controller="searchable" >Search</g:link>'
        assertOutputEquals('<a href="/searchable">Search</a>', template)

        template = '<g:link controller="searchable" action="other" >Search</g:link>'
        assertOutputEquals('<a href="/searchable/other">Search</a>', template)

        template = '<g:form controller="searchable" action="index" >Search</g:form>'
        assertOutputEquals('<form action="/searchable" method="post" >Search</form>', template)

        template = '<g:form controller="searchable" >Search</g:form>'
        assertOutputEquals('<form action="/searchable" method="post" >Search</form>', template)
    }

    void testLinkWithControllerAndId() {
        def template = '<g:link controller="book" id="10">${name}</g:link>'
        assertOutputEquals('<a href="/book?id=10">Groovy in Action</a>', template, [name:"Groovy in Action"])
    }

    void testRenderLinkWithReverseMapping() {
        def template = '<g:link controller="survey">${name}</g:link>'
        assertOutputEquals('<a href="/surveys">Food I Like</a>', template, [name:"Food I Like"])

        template = '<g:link controller="test" action="index" id="MacBook">${name}</g:link>'
        assertOutputEquals('<a href="/products/MacBook">MacBook</a>', template, [name:"MacBook"])
    }

    void testUrlMapper() {
        assert appCtx.grailsUrlMappingsHolder
        assert appCtx.grailsUrlMappingsHolder.urlMappings.length > 0
    }

    void testRenderLink() {
        def template = '<g:link controller="foo" action="list">${name}</g:link>'
        assertOutputEquals('<a href="/foo/list">bar</a>', template, [name:"bar"])
    }

    void testRenderForm() {
        def template = '<g:form controller="foo" action="list">${name}</g:form>'
        assertOutputEquals('<form action="/foo/list" method="post" >bar</form>', template, [name:"bar"])

        template = '<g:form controller="foo">${name}</g:form>'
        assertOutputEquals('<form action="/foo" method="post" >bar</form>', template, [name:"bar"])
    }

    void testRenderFormWithUrlAttribute() {
        def template = '<g:form url="[controller:\'stuff\',action:\'list\']">${name}</g:form>'
        assertOutputEquals('<form action="/stuff/list" method="post" >bar</form>', template, [name:"bar"])

        template = '<g:form url="[controller:\'stuff\',action:\'show\', id:11]" id="myForm">${name}</g:form>'
        assertOutputEquals('<form action="/stuff/show/11" method="post" id="myForm" >bar</form>', template, [name:"bar"])
    }

    void testRenderFormWithUrlAttributeAndReverseMapping() {
        def template = '<g:form url="[controller:\'test\',action:\'index\', id:\'MacBook\']">${name}</g:form>'
        assertOutputEquals('<form action="/products/MacBook" method="post" >MacBook</form>', template, [name:"MacBook"])
    }

    void testCreateLinkWithCollectionParamsGRAILS7096() {
        def template = '''<g:createLink controller="controller" action="action" params="[test:['1','2']]"/>'''

        assertOutputEquals(
            "/controller/action?test=1&test=2",
            template,
            [:])

        template = '''<g:createLink controller="controller" action="action" params="[test:['2','3']]"/>'''

        assertOutputEquals(
            "/controller/action?test=2&test=3",
            template,
            [:])
    }

    void testCreateLinkWithObjectArrayParams() {
        def template = '''<g:createLink controller="controller" action="action" params="[test:['1','2'] as Object[]]"/>'''

        assertOutputEquals(
            "/controller/action?test=1&test=2",
            template,
            [:])

        template = '''<g:createLink controller="controller" action="action" params="[test:['2','3'] as Object[]]"/>'''

        assertOutputEquals(
            "/controller/action?test=2&test=3",
            template,
            [:])
    }

    void testCreateLinkWithExtraParamsGRAILS8249() {
        def template = '''<g:createLink controller="test2" action="show" id="jim" params="[name: 'Jim Doe', age: 31]" />'''
        assertOutputEquals("/dummy/show/Jim+Doe/jim?age=31", template, [:])

        // Ensure that without the required name param that it falls back to the conventional mapping
        template = '''<g:createLink controller="test2" action="show" id="jim" params="[age: 31]" />'''
        assertOutputEquals("/test2/show/jim?age=31", template, [:])
    }

}
