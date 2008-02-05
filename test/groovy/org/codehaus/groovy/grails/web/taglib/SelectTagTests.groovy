package org.codehaus.groovy.grails.web.taglib
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jan 25, 2008
 */
class SelectTagTests extends AbstractGrailsTagTests {

    void testSelectWithBigDecimal() {
        def template = '<g:set var="value" value="${2.4}"/><g:select name="foo" from="[1,2,3]" value="${value}" />'
        assertOutputContains('<option value="2" selected="selected" >2</option>', template)
    }

    void testSimpleSelect() {
        def template = '<g:select name="foo" from="[1,2,3]" value="1" />'
        assertOutputContains('<option value="1" selected="selected" >1</option>', template)
        assertOutputContains('<option value="2" >2</option>', template)
        assertOutputContains('<option value="3" >3</option>', template)
    }


    void testMultiSelect() {
        def template = '<g:select name="foo" from="[1,2,3]" value="[2,3]" />'

        assertOutputContains('<select name="foo" id="foo" multiple="true" >', template)
        assertOutputContains('<option value="1" >1</option>', template)
        assertOutputContains('<option value="2" selected="selected" >2</option>', template)
        assertOutputContains('<option value="3" selected="selected" >3</option>', template)
    }
}