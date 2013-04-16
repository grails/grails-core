package org.grails.databinding.converters

import groovy.transform.CompileStatic

import org.apache.commons.lang.StringUtils
import org.grails.databinding.StructuredBindingEditor
import org.junit.Assert

@CompileStatic
abstract class AbstractStructuredDateBindingEditor<T> implements StructuredBindingEditor<T> {
    public T assemble(String propertyName, Map fieldValues) throws IllegalArgumentException {
        final prefix = propertyName + '_'
        Assert.assertTrue("Can't populate a date without a year", fieldValues.containsKey(prefix + "year"))

        def yearString = (String)fieldValues.get(prefix + "year")
        def monthString = (String) fieldValues.get(prefix + "month")
        def dayString = (String) fieldValues.get(prefix + "day")
        def hourString = (String) fieldValues.get(prefix + "hour")
        def minuteString = (String) fieldValues.get(prefix + "minute")
        if (StringUtils.isBlank(yearString)
        && StringUtils.isBlank(monthString)
        && StringUtils.isBlank(dayString)
        && StringUtils.isBlank(hourString)
        && StringUtils.isBlank(minuteString)) {
            return null
        }
        def year
        try {
            Assert.assertTrue("Can't populate a date without a year", !StringUtils.isBlank(yearString))

            year = Integer.parseInt(yearString)

            int month = getIntegerValue(fieldValues, prefix + "month", 1)
            int day = getIntegerValue(fieldValues, prefix + "day", 1)
            int hour = getIntegerValue(fieldValues, prefix + "hour", 0)
            int minute = getIntegerValue(fieldValues, prefix + "minute", 0)

            def c = new GregorianCalendar(year,month - 1,day,hour,minute)
            return getDate(c)
        }
        catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Unable to parse structured date from request for date [\"+propertyName+\"]\"")
        }
    }
    
    public List<String> getRequiredFields() {
        ['year']
    }

    public List<String> getOptionalFields() {
        ['month', 'day', 'hour', 'minute']
    }

    public T getPropertyValue(Object obj, String propertyName,
            Map<String, Object> source) {
        assemble(propertyName, source)
    }

    private int getIntegerValue(Map values, String name, int defaultValue) throws NumberFormatException {
        if (values.get(name) != null) {
            return Integer.parseInt((String) values.get(name))
        }
        defaultValue
    }
    
    abstract T getDate(Calendar c)
}
