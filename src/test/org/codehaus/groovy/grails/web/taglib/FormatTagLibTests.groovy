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


    void testFormatBoolean() {
        messageSource.addMessage("default.boolean.true",request.locale, "Yeah!")
        messageSource.addMessage("default.boolean.false",request.locale, "Noooo!")

        def template = '<g:formatBoolean boolean="${theBoolean}" />'

        assertOutputEquals "Yeah!", template, [theBoolean:true]
        assertOutputEquals "Noooo!", template, [theBoolean:false]

        messageSource.addMessage("boolean.true",request.locale, "Yippeee!")
        messageSource.addMessage("boolean.false",request.locale, "Urghh!")

        assertOutputEquals "Yippeee!", template, [theBoolean:true]
        assertOutputEquals "Urghh!", template, [theBoolean:false]

    }


    void testFormatDate() {
        def calender = new GregorianCalendar(1980,1,3)
        def template = '<g:formatDate format="yyyy-MM-dd" date="${date}"/>'
        assertOutputEquals("1980-02-03", template, [date:calender.getTime()])
    }

    void testFormatDateWithStyle() {
        def calender = new GregorianCalendar(1980,1,3)
        def template = '<g:formatDate date="${date}" type="date" style="LONG" locale="en_US"/>'
        assertOutputEquals("February 3, 1980", template, [date:calender.getTime()])
    }

    void testFormatDateTimeWithStyle() {
        def calender = new GregorianCalendar(1980,1,3)
        def template = '<g:formatDate date="${date}" type="datetime" style="LONG" timeStyle="SHORT" locale="en_US"/>'
        assertOutputEquals("February 3, 1980 12:00 AM", template, [date:calender.getTime()])
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

    void testFormatBigDecimal() {
        def number = "3.12325678" as BigDecimal
        def template = '<g:formatNumber format="#.####" number="${number}"/>'
        assertOutputEquals("3.1233", template, [number: number])
    }

    void testFormatCurrency() {
        def number = "3.12325678" as BigDecimal
        def template = '<g:formatNumber type="currency" number="${number}" locale="fi_FI" />'
        assertOutputEquals("3,12 €", template, [number: number])
    }    

    void testFormatCurrencyWithCodeAndLocale() {
        def number = "3.12325678" as BigDecimal
        def template = '<g:formatNumber type="currency" currencyCode="USD" number="${number}" locale="fi_FI" />'
        assertOutputEquals("3,12 USD", template, [number: number])
    }    
    
    void testFormatCurrencyWithCode() {
        def number = "3.12325678" as BigDecimal
        def template = '<g:formatNumber type="currency" currencyCode="USD" number="${number}" locale="en_US" />'
        assertOutputEquals("\$3.12", template, [number: number])
    }    

    void testFormatNumberDecimals() {
        def number = "3.12325678" as BigDecimal
        def template = '<g:formatNumber type="number" number="${number}" locale="fi_FI" minFractionDigits="3" maxFractionDigits="3" />'
        assertOutputEquals("3,123", template, [number: number])
    }    

    void testFormatNumberRoundingModeHalfDown() {
        def number = "3.125" as BigDecimal
        def template = '<g:formatNumber type="number" number="${number}" locale="fi_FI" maxFractionDigits="2" roundingMode="HALF_DOWN" />'
        assertOutputEquals("3,12", template, [number: number])
    }

    void testFormatNumberRoundingModeHalfUp() {
        def number = "3.125" as BigDecimal
        def template = '<g:formatNumber type="number" number="${number}" locale="fi_FI" maxFractionDigits="2" roundingMode="HALF_UP" />'
        assertOutputEquals("3,13", template, [number: number])
    }

    void testFormatNumberRoundingModeUnnecessary() {
        def number = "3.125" as BigDecimal
        def template = '<g:formatNumber type="number" number="${number}" locale="fi_FI" maxFractionDigits="2" roundingMode="UNNECESSARY" />'
        assertOutputEquals("3,125", template, [number: number])
    }

    void testFormatNumberRoundingModeUnnecessary2() {
        def number = "3.125" as BigDecimal
        def template = '<g:formatNumber type="number" number="${number}" locale="fi_FI" maxFractionDigits="3" roundingMode="UNNECESSARY" />'
        assertOutputEquals("3,125", template, [number: number])
    }

    void testFormatNumberInteger() {
        def number = "3.12325678" as BigDecimal
        def template = '<g:formatNumber type="number" number="${number}" locale="fi_FI" minIntegerDigits="3" maxIntegerDigits="3" minFractionDigits="0" maxFractionDigits="0"/>'
        assertOutputEquals("003", template, [number: number])
    }    

	void testFormatNumberInteger2() {
		def number = 1
		def template = '<g:formatNumber type="number" number="${number}" minIntegerDigits="3"/>'
		assertOutputEquals("001", template, [number: number])
	} 
	
	void testFormatNumberInteger3() {
		def number = 1234
		def template = '<g:formatNumber type="number" number="${number}" minIntegerDigits="3" groupingUsed="false" locale="en_US"/>'
		assertOutputEquals("1234", template, [number: number])
	} 
	
	void testFormatNumberInteger4() {
		def number = 1234
		def template = '<g:formatNumber type="number" number="${number}" minIntegerDigits="3" groupingUsed="true" locale="en_US"/>'
		assertOutputEquals("1,234", template, [number: number])
	} 	
	
	void testFormatNumberParsingString() {
        def number = "3,12325678" as String
        def template = '<g:formatNumber type="number" number="${number}" locale="fi_FI" minFractionDigits="3" maxFractionDigits="3" />'
        assertOutputEquals("3,123", template, [number: number])
    }    
}
