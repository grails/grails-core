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
assertOutputEquals('''<select name="foo" id="foo" >
<option value="1" >1</option>
<option value="2" selected="selected" >2</option>
<option value="3" >3</option>
</select>''', template)        
    }

    void testSimpleSelect() {
        def template = '<g:select name="foo" from="[1,2,3]" value="1" />'

assertOutputEquals('''<select name="foo" id="foo" >
<option value="1" selected="selected" >1</option>
<option value="2" >2</option>
<option value="3" >3</option>
</select>''', template)

    }


    void testMultiSelect() {
        def template = '<g:select name="foo" from="[1,2,3]" value="[2,3]" />'

assertOutputEquals('''<select name="foo" id="foo" multiple="true" >
<option value="1" >1</option>
<option value="2" selected="selected" >2</option>
<option value="3" selected="selected" >3</option>
</select>''', template)

    }
}