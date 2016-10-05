package org.grails.plugins.web

/**
 * Created by jameskleeh on 10/5/16.
 */
interface GrailsTagDateHelper {

    Object getTimeZone(Object timeZone)

    Object getFormatFromPattern(String format, Object timeZone, Locale locale)

    Object getDateFormat(String style, Object timeZone, Locale locale)

    Object getTimeFormat(String style, Object timeZone, Locale locale)

    Object getDateTimeFormat(String dateStyle, String timeStyle, Object timeZone, Locale locale)

    String format(Object formatter, Object date)

    Boolean supportsDatePicker(Class clazz)

    GregorianCalendar buildCalendar(Object date)
}