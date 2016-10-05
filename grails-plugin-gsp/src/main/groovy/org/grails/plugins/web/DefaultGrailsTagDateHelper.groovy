package org.grails.plugins.web

import org.apache.commons.lang.time.FastDateFormat


class DefaultGrailsTagDateHelper implements GrailsTagDateHelper {

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

    Object getFormatFromPattern(String format, Object timeZone, Locale locale) {
        FastDateFormat.getInstance(format, (TimeZone)timeZone, locale)
    }

    Object getDateFormat(String style, Object timeZone, Locale locale) {
        FastDateFormat.getDateInstance(parseStyle(style), (TimeZone)timeZone, locale)
    }

    Object getTimeFormat(String style, Object timeZone, Locale locale) {
        FastDateFormat.getTimeInstance(parseStyle(style), (TimeZone)timeZone, locale)
    }

    Object getDateTimeFormat(String dateStyle, String timeStyle, Object timeZone, Locale locale) {
        FastDateFormat.getDateTimeInstance(parseStyle(dateStyle), parseStyle(timeStyle), (TimeZone)timeZone, locale)
    }

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

    Boolean supportsDatePicker(Class clazz) {
        clazz == Date
    }

    GregorianCalendar buildCalendar(Object date) {
        GregorianCalendar c = new GregorianCalendar()
        c.setTime((Date)date)
        c
    }
}
