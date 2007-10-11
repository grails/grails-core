/**
 * Tests for the FormatTagLib
 
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

    void testFormatNumber() {
        def template = '<g:formatNumber number="${myNumber}" format="\\$###,##0" />'

        assertOutputEquals('$10', template, [myNumber:10])

    }

    void testEncodeAs() {
        def template = '<g:encodeAs codec="HTML">Coheed & Cambria</g:encodeAs>'

        assertOutputEquals('Coheed &amp; Cambria', template, [:])

    }

}