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
 * WITHOUT c;pWARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.plugins.web.taglib

import java.text.DecimalFormatSymbols;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.context.NoSuchMessageException;
import org.springframework.web.servlet.support.RequestContextUtils as RCU

 /**
 * The base application tag library for Grails many of which take inspiration from Rails helpers (thanks guys! :)
 * This tag library tends to get extended by others as tags within here can be re-used in said libraries
 *
 * @author Jason Rudolph
 * @author Lari Hotari
 * @since 0.6
 *
 * Created:17-Jan-2006
 */
class FormatTagLib {
	static returnObjectForTags = ['formatBoolean','formatDate','formatNumber','encodeAs']

    String messageHelper(code, defaultMessage = null, args = null, locale = null) {
    	if(locale==null) {
    		locale=RCU.getLocale(request)
    	}
        def messageSource = grailsAttributes.getApplicationContext().getBean("messageSource")
        def message
        try {
        	message = messageSource.getMessage( code, args == null ? null : args.toArray(), locale )
        } catch (NoSuchMessageException e) {
        	if(defaultMessage != null) {
        		message = defaultMessage()
        	}
        }
        return message
    }
	
    /**
      * Outputs the given boolean as the specified text label. If the
      * <code>true</code> and <code>false</code> option are not given,
      * then the boolean is output using the default label.
      *
      * Attributes:
      *
      * boolean - the boolean to output
      * true (optional) - text label for boolean true value
      * false (optional) - text label for boolean false value
      *
      * Examples:
      *
      * <g:formatBoolean boolean="${myBoolean}" />
      * <g:formatBoolean boolean="${myBoolean}" true="True!" false="False!" />
      */
     def formatBoolean = { attrs ->
         if (!attrs.containsKey("boolean"))
             throwTagError("Tag [formatBoolean] is missing required attribute [boolean]")

         def b = attrs.get("boolean")
         if (b == null) {
             return null
         }
         else if (!(b instanceof Boolean)) {
             b = Boolean.valueOf(b)
         }

         if (b) {
             return attrs["true"] ?: messageHelper('boolean.true', { messageHelper('default.boolean.true', 'True') })
         }
         else {
        	 return attrs["false"] ?: messageHelper('boolean.false', { messageHelper('default.boolean.false', 'False') })
         }
     }

    /**
     * Outputs the given <code>Date</code> object in the specified format.  If
     * the <code>date</code> is not given, then the current date/time is used.
     * If the <code>format</code> option is not given, then the date is output
     * using the default format.
     *
     * e.g., <g:formatDate date="${myDate}" format="yyyy-MM-dd HH:mm" />
     *
     * @see java.text.SimpleDateFormat
     */
    def formatDate = { attrs ->
    
    	def date
    	if (attrs.containsKey('date')) {
        	date = attrs.get('date')
        	if(date == null) return null
    	}
    	else {
            date = new Date()
    	}

        def formatName = attrs.get('formatName')
        def format = attrs.get('format')
        
        if(!format && formatName) {
            format = messageHelper(formatName)
            if(!format) throwTagError("Attribute [formatName] of Tag [formatDate] specifies a format key [$formatName] that does not exist within a message bundle!")
        }
        else if (!format) {
            format = messageHelper('date.format', { messageHelper('default.date.format', 'yyyy-MM-dd HH:mm:ss z') })
        }

        def locale = RCU.getLocale(request)
        def dateFormat = locale ? FastDateFormat.getInstance(format, locale) : FastDateFormat.getInstance(format)
        
        return dateFormat.format(date)
    }

    /**
     * Outputs the given number in the specified format.  If the
     * <code>format</code> option is not given, then the number is output
     * using the default format.
     *
     * e.g., <g:formatNumber number="${myNumber}" format="###,##0" />
     *
     * @see java.text.DecimalFormat
     */
    def formatNumber = { attrs ->

		if (!attrs.containsKey('number'))
			throwTagError("Tag [formatNumber] is missing required attribute [number]")
		
    	def number = attrs.get('number')
    	if (number == null) return null
        else if(!(number instanceof Number)) {
            number = number.toString().toInteger()
        }
    	
        def formatName = attrs.get('formatName')
        def format = attrs.get('format')
        
        if(!format && formatName) {
            format = messageHelper(formatName)
            if(!format) throwTagError("Attribute [formatName] of Tag [formatNumber] specifies a format key [$formatName] that does not exist within a message bundle!")
        }
        else if (!format) {
            format = messageHelper( "number.format", { messageHelper( "default.number.format", "0") })
        }

        final def locale = RCU.getLocale(request)
        def dcfs = locale ? new DecimalFormatSymbols( locale ) : new DecimalFormatSymbols()

        def decimalFormat = new java.text.DecimalFormat( format, dcfs )

        return decimalFormat.format((Double)number)
    }

    def encodeAs = { attrs, body ->
        if (!attrs.codec)
            throwTagError("Tag [encodeAs] requires a codec name in the [codec] attribute")

        return body()?."encodeAs${attrs.codec}"()
    }
}
