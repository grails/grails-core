package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.commons.UrlMappingsArtefactHandler

class NamespacedNamedUrlMappingTests extends AbstractGrailsTagTests {

    protected void onInit() {
        def mappingClass = gcl.parseClass('''
class TestUrlMappings {
    static mappings = {
      "/$controller/$action?/$id?"{
          constraints {
             // apply constraints here
          }
      }

      name productListing: "/products/list" {
          controller = "product"
          action = "list"
      }

      name productDetail: "/showProduct/$productName/$flavor?" {
          controller = "product"
          action = "show"
      }
    }
}
        ''')

        grailsApplication.addArtefact(UrlMappingsArtefactHandler.TYPE, mappingClass)
    }

    void testLinkAttributes() {
        def template = '<link:productDetail attrs="[class: \'fancy\']" productName="licorice" flavor="strawberry">Strawberry Licorice</link:productDetail>'
        assertOutputEquals '<a href="/showProduct/licorice/strawberry" class="fancy">Strawberry Licorice</a>', template
    }

    void testLinkAttributesPlusAdditionalRequestParameters() {
        def template = '<link:productDetail attrs="[class: \'fancy\']" packaging="boxed" size="large" productName="licorice" flavor="strawberry">Strawberry Licorice</link:productDetail>'
            assertOutputEquals '<a href="/showProduct/licorice/strawberry?packaging=boxed&amp;size=large" class="fancy">Strawberry Licorice</a>', template
    }

    void testNoParameters() {
        def template = '<link:productListing>Product Listing</link:productListing>'
        assertOutputEquals '<a href="/products/list">Product Listing</a>', template
    }

    void testAttributeForParameter() {
        def template = '<link:productDetail productName="Scotch">Scotch Details</link:productDetail>'
        assertOutputEquals '<a href="/showProduct/Scotch">Scotch Details</a>', template
    }

    void testMultipleAttributesForParameters() {
        def template = '<link:productDetail productName="licorice" flavor="strawberry">Strawberry Licorice</link:productDetail>'
        assertOutputEquals '<a href="/showProduct/licorice/strawberry">Strawberry Licorice</a>', template
    }
}
