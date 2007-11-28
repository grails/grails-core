/**
 * Tests for the FormatTagLib.
 *
 * @author Graeme Rocher
 * @since 0.6
 *
 * Created: Jul 25, 2007
 * Time: 7:57:59 AM
 * 
 */

package org.codehaus.groovy.grails.web.taglib

class FormatTagLibTests extends AbstractGrailsTagTests {

    void testFormatDate() {
        def calender = new GregorianCalendar(1980,1,3)
        def template = '<g:formatDate format="yyyy-MM-dd" date="${date}"/>'
        assertOutputEquals("1980-02-03", template, [date:calender.getTime()])
    }

    void testFormatDateNullDate() {
        def template = '<g:formatDate format="yyyy-MM-dd" date="${date}"/>'
        assertOutputEquals("", template, [date:null])
    }

    void testFormatDateCurrentDate() {
        def template = '<g:formatDate format="yyyy-MM-dd"/>'
        def output = applyTemplate(template)
        assertTrue(output ==~ /\d{4}-\d{2}-\d{2}/)
    }

    void testFormatNumber() {
        def template = '<g:formatNumber number="${myNumber}" format="\\$###,##0"/>'
        assertOutputEquals('$10', template, [myNumber:10])
    }

    void testFormatNumberNullNumber() {
        def template = '<g:formatNumber number="${myNumber}"/>'
        assertOutputEquals("", template, [myNumber:null])
    }

    void testFormatNumberNoNumber() {
        try
        {
        	applyTemplate('<g:formatNumber/>')
        	fail('Expecting a GrailsTagException')
        }
        catch(org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException e)
        {
        	// expected
        }
    }

    void testFormatDateFromBundle() {
        def calender = new GregorianCalendar(1980,1,3)
        def template = '<g:formatDate formatName="format.date" date="${date}"/>'
        messageSource.addMessage("format.date", request.locale, "yyyy-MM-dd")

        assertOutputEquals("1980-02-03", template, [date:calender.getTime()])
    }

    void testFormatNumberFromBundle() {
       def template = '<g:formatNumber number="${myNumber}" formatName="format.number" />'
       messageSource.addMessage("format.number", request.locale, '\$###,##0')
        assertOutputEquals('$10', template, [myNumber:10])
    }

    void testEncodeAs() {
        def template = '<g:encodeAs codec="HTML">Coheed & Cambria</g:encodeAs>'

        assertOutputEquals('Coheed &amp; Cambria', template, [:])

    }
}
