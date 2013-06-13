/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.databinding.converters

import groovy.transform.CompileStatic

import org.apache.commons.lang.StringUtils
import org.grails.databinding.StructuredBindingEditor

/**
 * @author Jeff Brown
 * @since 2.3
 */
@CompileStatic
abstract class AbstractStructuredDateBindingEditor<T> implements StructuredBindingEditor<T> {

    public T assemble(String propertyName, Map fieldValues) throws IllegalArgumentException {
        final prefix = propertyName + '_'
        assert fieldValues.containsKey(prefix + "year"), "Can't populate a date without a year"

        def yearString = (String)fieldValues.get(prefix + "year")
        def monthString = (String) fieldValues.get(prefix + "month")
        def dayString = (String) fieldValues.get(prefix + "day")
        def hourString = (String) fieldValues.get(prefix + "hour")
        def minuteString = (String) fieldValues.get(prefix + "minute")
        if (StringUtils.isBlank(yearString) &&
            StringUtils.isBlank(monthString) &&
            StringUtils.isBlank(dayString) &&
            StringUtils.isBlank(hourString) &&
            StringUtils.isBlank(minuteString)) {
            return null
        }
        def year
        try {
            assert !StringUtils.isBlank(yearString), "Can't populate a date without a year"

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

    List<String> getRequiredFields() {
        ['year']
    }

    List<String> getOptionalFields() {
        ['month', 'day', 'hour', 'minute']
    }

    public T getPropertyValue(obj, String propertyName, Map<String, Object> source) {
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
