/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.web.binding;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.propertyeditors.CustomDateEditor;

import java.text.DateFormat;
import java.util.*;

/**
 * Structured editor for editing dates that takes 5 fields that represent the year, month, day, hour and minute
 * and constructs a Date instance
 *
 * @since 1.0.4
 * @author Graeme Rocher 
 */
public class StructuredDateEditor extends CustomDateEditor implements StructuredPropertyEditor {

    public StructuredDateEditor(DateFormat dateFormat, boolean b) {
        super(dateFormat, b);
    }

    public StructuredDateEditor(DateFormat dateFormat, boolean b, int i) {
        super(dateFormat, b, i);
    }

    public List getRequiredFields() {
        List requiredFields = new ArrayList();
        requiredFields.add("year");
        return requiredFields;
    }

    public List getOptionalFields() {
        List optionalFields = new ArrayList();
        optionalFields.add("month");
        optionalFields.add("day");
        optionalFields.add("hour");
        optionalFields.add("minute");
        return optionalFields;
    }

    public Object assemble(Class type, Map fieldValues) throws IllegalArgumentException {
        if (!fieldValues.containsKey("year")) {
            throw new IllegalArgumentException("Can't populate a date without a year");
        }

        String yearString = (String) fieldValues.get("year");
        int year;

        try {
            if(StringUtils.isBlank(yearString)) {
                throw new IllegalArgumentException("Can't populate a date without a year");
            }
            else {
                year = Integer.parseInt(yearString);
            }

            int month = getIntegerValue(fieldValues, "month", 1);
            int day = getIntegerValue(fieldValues, "day", 1);
            int hour = getIntegerValue(fieldValues, "hour", 0);
            int minute = getIntegerValue(fieldValues, "minute", 0);

            Calendar c = new GregorianCalendar(year,month - 1,day,hour,minute);
            if(type == Date.class) {
                return c.getTime();
            } else if(type == java.sql.Date.class) {
                return new java.sql.Date(c.getTime().getTime());
            }
            return c;
        }
        catch(NumberFormatException nfe) {
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
