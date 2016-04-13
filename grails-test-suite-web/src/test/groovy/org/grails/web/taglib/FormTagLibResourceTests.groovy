package org.grails.web.taglib

import org.grails.core.artefact.UrlMappingsArtefactHandler

class FormTagLibResourceTests extends AbstractGrailsTagTests {

    protected void onInit() {
        def mappingClass = gcl.parseClass('''
class TestUrlMappings {
    static mappings = {
        "/books"(resources:"book") {
            "/authors"(resources:"author")
        }
    }
}
        ''')

        grailsApplication.addArtefact(UrlMappingsArtefactHandler.TYPE, mappingClass)
    }

    void testResourceSave() {
        def template = '<g:form resource="book" action="save"/>'
        assertOutputEquals('<form action="/books" method="post" ></form>', template)
    }

    void testResourceUpdate() {
        def template = '<g:form resource="book" action="update" id="1"/>'
        assertOutputEquals('<form action="/books/1" method="post" ><input type="hidden" name="_method" value="PUT" id="_method" /></form>', template)
    }

    void testResourceUpdateIdInParams() {
        def template = '<g:form resource="book" action="update" params="[id:1]"/>'
        assertOutputEquals('<form action="/books/1" method="post" ><input type="hidden" name="_method" value="PUT" id="_method" /></form>', template)
    }

    void testResourcePatch() {
        def template = '<g:form resource="book" action="patch" id="1"/>'
        assertOutputEquals('<form action="/books/1" method="post" ><input type="hidden" name="_method" value="PATCH" id="_method" /></form>', template)
    }

    void testResourcePatchIdInParams() {
        def template = '<g:form resource="book" action="patch" params="[id:1]"/>'
        assertOutputEquals('<form action="/books/1" method="post" ><input type="hidden" name="_method" value="PATCH" id="_method" /></form>', template)
    }

    void testResourceNestedSave() {
        def template = '<g:form resource="book/author" action="save" params="[bookId:1]"/>'
        assertOutputEquals('<form action="/books/1/authors" method="post" ></form>', template)
    }

    void testResourceNestedUpdate() {
        // We'd really like to suppoer this format <g:form resource="book/author" action="update" id="2" bookId="1"/>
        // but the form tag limits the set of attributes it hands to the linkGenerator and the dynamic parameters like 'bookId' get filtered out
        // instead we make do with putting bookId in the params attribute
        def template = '<g:form resource="book/author" action="update" id="2" params="[bookId:1]"/>'
        assertOutputEquals('<form action="/books/1/authors/2" method="post" ><input type="hidden" name="_method" value="PUT" id="_method" /></form>', template)
    }

    void testResourceNestedUpdateIdInParams() {
        def template = '<g:form resource="book/author" action="update" params="[bookId:1, id:2]"/>'
        assertOutputEquals('<form action="/books/1/authors/2" method="post" ><input type="hidden" name="_method" value="PUT" id="_method" /></form>', template)
    }

    void testResourceNestedPatch() {
        def template = '<g:form resource="book/author" action="patch" id="2" params="[bookId:1]"/>'
        assertOutputEquals('<form action="/books/1/authors/2" method="post" ><input type="hidden" name="_method" value="PATCH" id="_method" /></form>', template)
    }

    void testResourceNestedPatchIdInParams() {
        def template = '<g:form resource="book/author" action="patch" params="[bookId:1, id:2]"/>'
        assertOutputEquals('<form action="/books/1/authors/2" method="post" ><input type="hidden" name="_method" value="PATCH" id="_method" /></form>', template)
    }

    void assertOutputEquals(expected, template, params = [:]) {
        def engine = appCtx.groovyPagesTemplateEngine

        assert engine
        def t = engine.createTemplate(template, "test_"+ System.currentTimeMillis())

        def w = t.make(params)

        def sw = new StringWriter()
        def out = new PrintWriter(sw)
        webRequest.out = out
        w.writeTo(out)

        assertEquals expected, sw.toString()
    }
}
