/*
 * Copyright 2004-2005 the original author or authors.
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
package org.grails.plugins.web

import groovy.transform.CompileStatic
import org.apache.commons.lang.time.FastDateFormat

/**
 * The default implementation of {@link GrailsTagDateHelper}
 *
 * @author James Kleeh
 * @since 3.2.1
 * @see GrailsTagDateHelper
 */
@CompileStatic
class DefaultGrailsTagDateHelper implements GrailsTagDateHelper {

    @Override
    Object getTimeZone(Object timeZone) {
        if (timeZone != null) {
            if (!(timeZone instanceof TimeZone)) {
                TimeZone.getTimeZone(timeZone as String)
            } else {
                timeZone
            }
        } else {
            TimeZone.getDefault()
        }
    }

    @Override
    Object getFormatFromPattern(String format, Object timeZone, Locale locale) {
        FastDateFormat.getInstance(format, (TimeZone)timeZone, locale)
    }

    @Override
    Object getDateFormat(String style, Object timeZone, Locale locale) {
        FastDateFormat.getDateInstance(parseStyle(style), (TimeZone)timeZone, locale)
    }

    @Override
    Object getTimeFormat(String style, Object timeZone, Locale locale) {
        FastDateFormat.getTimeInstance(parseStyle(style), (TimeZone)timeZone, locale)
    }

    @Override
    Object getDateTimeFormat(String dateStyle, String timeStyle, Object timeZone, Locale locale) {
        FastDateFormat.getDateTimeInstance(parseStyle(dateStyle), parseStyle(timeStyle), (TimeZone)timeZone, locale)
    }

    @Override
    String format(Object formatter, Object date) {
        ((FastDateFormat)formatter).format(date)
    }

    private static int parseStyle(String styleStr) {
        switch (styleStr) {
            case 'FULL':   return FastDateFormat.FULL
            case 'LONG':   return FastDateFormat.LONG
            case 'MEDIUM': return FastDateFormat.MEDIUM
            default:       return FastDateFormat.SHORT
        }
    }

    @Override
    Boolean supportsDatePicker(Class clazz) {
        clazz == Date
    }

    @Override
    GregorianCalendar buildCalendar(Object date) {
        GregorianCalendar c = new GregorianCalendar()
        c.setTime((Date)date)
        c
    }
}
