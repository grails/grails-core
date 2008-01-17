package org.codehaus.groovy.grails.plugins.web.taglib

import org.springframework.web.servlet.support.RequestContextUtils as RCU;
import java.text.*

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

import org.springframework.web.servlet.support.RequestContextUtils as RCU;

 /**
 * The base application tag library for Grails many of which take inspiration from Rails helpers (thanks guys! :)
 * This tag library tends to get extended by others as tags within here can be re-used in said libraries
 *
 * @author Jason Rudolph
 * @since 0.6
 *
 * Created:17-Jan-2006
 */
class FormatTagLib {
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
        	if(date == null) return
    	}
    	else {
            date = new Date()
    	}

        def formatName = attrs.get('formatName')
        def format = attrs.get('format')
        
        if(!format && formatName) {
            format = message(code:formatName)
            if(!format) throwTagError("Attribute [formatName] of Tag [formatDate] specifies a format key [$formatName] that does not exist within a message bundle!")
        }
        else if (!format) {
            format = "yyyy-MM-dd HH:mm:ss z"
        }

        def locale = RCU.getLocale(request)
        def simpleDateFormat = locale ?
        new SimpleDateFormat(format, locale) :
        new SimpleDateFormat(format)
        
        out << simpleDateFormat.format(date)
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
    	if (number == null) return
    	
        def formatName = attrs.get('formatName')
        def format = attrs.get('format')
        
        if(!format && formatName) {
            format = message(code:formatName)
            if(!format) throwTagError("Attribute [formatName] of Tag [formatNumber] specifies a format key [$formatName] that does not exist within a message bundle!")
        }
        else if (!format) {
                format = "0"
        }

        final def locale = RCU.getLocale(request)
        def dcfs = locale ?
        new DecimalFormatSymbols( locale ) :
        new DecimalFormatSymbols()

        def decimalFormat = new java.text.DecimalFormat( format, dcfs )

        out << decimalFormat.format((Double)number)
    }

    def encodeAs = { attrs, body ->
        if (!attrs.codec)
            throwTagError("Tag [encodeAs] requires a codec name in the [codec] attribute")

        out << body()."encodeAs${attrs.codec}"()
    }
}
