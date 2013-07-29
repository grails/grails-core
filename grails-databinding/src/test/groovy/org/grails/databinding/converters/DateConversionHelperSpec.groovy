package org.grails.databinding.converters

import java.text.ParseException

import spock.lang.Specification

class DateConversionHelperSpec extends Specification {

    void 'Test parsing dates'() {
        given:
        def helper = new DateConversionHelper()

        when:
        def date = helper.convert '2013-04-15 21:26:31.973'

        then:
        Calendar.APRIL == date.month
        15 == date.date
        113 == date.year
        21 == date.hours
        26 == date.minutes
        31 == date.seconds

        when:
        date = helper.convert '2011-03-12T09:24:22Z'

        then:
        Calendar.MARCH == date.month
        12 == date.date
        111 == date.year
        9 == date.hours
        24 == date.minutes
        22 == date.seconds
    }

    void 'Test custom formats'() {
        given:
        def helper = new DateConversionHelper()
        helper.formatStrings = ['MMddyyyy', "'Month: 'MM', Day: 'dd', Year: 'yyyy"]

        when:
        def date = helper.convert '11151969'

        then:
        Calendar.NOVEMBER == date.month
        15 == date.date
        69 == date.year
        0 == date.hours
        0 == date.minutes
        0 == date.seconds

        when:
        date = helper.convert 'Month: 04, Day: 07, Year: 1984'

        then:
        Calendar.APRIL == date.month
        7 == date.date
        84 == date.year
        0 == date.hours
        0 == date.minutes
        0 == date.seconds
    }

    void 'Test invalid format String'() {
        given:
        def helper = new DateConversionHelper()

        when:
        helper.convert 'some bogus value'

        then:
        thrown ParseException
    }

    void 'Test formatting an empty String'() {
        given:
        def helper = new DateConversionHelper()

        when:
        helper.convert ''

        then:
        thrown ParseException
    }
}
