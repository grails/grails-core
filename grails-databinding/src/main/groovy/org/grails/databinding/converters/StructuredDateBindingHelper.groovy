/* Copyright 2013 the original author or authors.
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

import org.apache.commons.lang.StringUtils
import org.grails.databinding.StructuredDataBindingHelper
import org.junit.Assert

class StructuredDateBindingHelper implements StructuredDataBindingHelper {

    private final Class dateType

    StructuredDateBindingHelper(Class dateType) {
        this.dateType = dateType
    }

    public List<String> getRequiredFields() {
        return Arrays.asList("year");
    }

    public List<String> getOptionalFields() {
        return Arrays.asList("month", "day", "hour", "minute");
    }
    public Object getPropertyValue(Object obj, String propertyName,
            Map<String, Object> source) {
        return assemble(propertyName, source);
    }

    public Object assemble(String propertyName, Map fieldValues) throws IllegalArgumentException {
        final String prefix = propertyName + "_";
        Assert.assertTrue("Can't populate a date without a year", fieldValues.containsKey(prefix + "year"));

        String yearString = (String) fieldValues.get(prefix + "year");
        String monthString = (String) fieldValues.get(prefix + "month");
        String dayString = (String) fieldValues.get(prefix + "day");
        String hourString = (String) fieldValues.get(prefix + "hour");
        String minuteString = (String) fieldValues.get(prefix + "minute");
        if (StringUtils.isBlank(yearString)
        && StringUtils.isBlank(monthString)
        && StringUtils.isBlank(dayString)
        && StringUtils.isBlank(hourString)
        && StringUtils.isBlank(minuteString)) {
            return null;
        }
        int year;
        try {
            Assert.assertTrue("Can't populate a date without a year", !StringUtils.isBlank(yearString));

            year = Integer.parseInt(yearString);

            int month = getIntegerValue(fieldValues, prefix + "month", 1);
            int day = getIntegerValue(fieldValues, prefix + "day", 1);
            int hour = getIntegerValue(fieldValues, prefix + "hour", 0);
            int minute = getIntegerValue(fieldValues, prefix + "minute", 0);

            Calendar c = new GregorianCalendar(year,month - 1,day,hour,minute);
            if (dateType == Date.class) {
                return c.getTime();
            }
            if (dateType == java.sql.Date.class) {
                return new java.sql.Date(c.getTime().getTime());
            }
            return c;
        }
        catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Unable to parse structured date from request for date [\"+propertyName+\"]\"");
        }
    }

    private int getIntegerValue(Map values, String name, int defaultValue) throws NumberFormatException {
        if (values.get(name) != null) {
            return Integer.parseInt((String) values.get(name));
        }
        return defaultValue;
    }
}
