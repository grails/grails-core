package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.commons.*

/**
 * Some more tests for the behaviour of reverse linking from mappings.
 *
 * @author Graeme Rocher
 * @since 0.6
 */
class OverlappingReverseMappedLinkTests extends AbstractGrailsTagTests {

    protected void onInit() {
        def mappingClass = gcl.parseClass('''
class UrlMappings {
    static mappings = {
      "/authors" {
              controller = "author"
              action = "list"
          }

      "/content/$controller/$action?/$id?"{
          constraints {
             // apply constraints here
          }
      }
    }
}
        ''')

        grailsApplication.addArtefact(UrlMappingsArtefactHandler.TYPE, mappingClass)
    }

    void testSimpleLink() {
        def expected = '<a href="/authors">link1</a>'
        assertOutputEquals(expected, '<g:link controller="author" action="list">link1</g:link>')
    }

    void testLinkWithPaginationParams() {
        def expected = '<a href="/authors?max=10&amp;offset=20">link1</a>'
        assertOutputEquals(expected, '<g:link controller="author" action="list" params="[max:10,offset:20]">link1</g:link>')
    }
}
