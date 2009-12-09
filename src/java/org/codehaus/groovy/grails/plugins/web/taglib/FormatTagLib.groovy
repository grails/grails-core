

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

import java.math.RoundingMode;
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang.time.FastDateFormat
import org.springframework.context.NoSuchMessageException
import org.springframework.util.StringUtils
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
        		if(defaultMessage instanceof Closure) {
        			message = defaultMessage()
        		} else {
        			message = defaultMessage as String
        		}
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

         def locale = resolveLocale(attrs.get('locale'))
         if (b) {
             return attrs["true"] ?: messageHelper('boolean.true', { messageHelper('default.boolean.true', 'True', null, locale) }, null, locale)
         }
         else {
        	 return attrs["false"] ?: messageHelper('boolean.false', { messageHelper('default.boolean.false', 'False', null, locale) }, null, locale)
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

    	def locale = resolveLocale(attrs.get('locale'))
    	def timeStyle = null
    	def dateStyle = null
    	if(attrs.get('style') != null) {
    		def style=attrs.get('style').toString().toUpperCase()
    		timeStyle = style
    		dateStyle = style
    	}
    	if(attrs.get('dateStyle') != null) {
    		dateStyle=attrs.get('dateStyle').toString().toUpperCase()
    	}
    	if(attrs.get('timeStyle') != null) {
    		timeStyle=attrs.get('timeStyle').toString().toUpperCase()
    	}
    	def type = attrs.get('type')?.toString()?.toUpperCase()
        def formatName = attrs.get('formatName')
        def format = attrs.get('format')
        def timeZone = attrs.get('timeZone')
        if(timeZone!=null) {
        	if(!(timeZone instanceof TimeZone)) {
        		timeZone = TimeZone.getTimeZone(timeZone as String)
        	} 
        } else {
        	timeZone = TimeZone.getDefault()
        }
        
        def dateFormat
        if(!type) {
	        if(!format && formatName) {
	            format = messageHelper(formatName,null,null,locale)
	            if(!format) throwTagError("Attribute [formatName] of Tag [formatDate] specifies a format key [$formatName] that does not exist within a message bundle!")
	        }
	        else if (!format) {
	            format = messageHelper('date.format', { messageHelper('default.date.format', 'yyyy-MM-dd HH:mm:ss z', null, locale) }, null, locale)
	        }
	        dateFormat = FastDateFormat.getInstance(format, timeZone, locale)
        } else {
        	if(type=='DATE') {
    	        dateFormat = FastDateFormat.getDateInstance(parseStyle(dateStyle), timeZone, locale)
        	} else if (type=='TIME') {
        		dateFormat = FastDateFormat.getTimeInstance(parseStyle(timeStyle), timeZone, locale)
        	} else { // 'both' or 'datetime'
        		dateFormat = FastDateFormat.getDateTimeInstance(parseStyle(dateStyle), parseStyle(timeStyle), timeZone, locale)
        	}
        }
        
        return dateFormat.format(date)
    }

     def parseStyle(styleStr) {
      	def style=FastDateFormat.SHORT
    	if(styleStr=='FULL') {
    		style=FastDateFormat.FULL
    	} else if (styleStr=='LONG') {
    		style=FastDateFormat.LONG
    	} else if (styleStr=='MEDIUM') {
    		style=FastDateFormat.MEDIUM
    	}
      	return style
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

        def formatName = attrs.get('formatName')
        def format = attrs.get('format')
        def type = attrs.get('type')
        def locale = resolveLocale(attrs.get('locale'))
        
        if(type==null) {
	        if(!format && formatName) {
	            format = messageHelper(formatName,null,null,locale)
	            if(!format) throwTagError("Attribute [formatName] of Tag [formatNumber] specifies a format key [$formatName] that does not exist within a message bundle!")
	        }
	        else if (!format) {
	            format = messageHelper( "number.format", { messageHelper( "default.number.format", "0", null, locale) } ,null ,locale)
	        }
        }

        DecimalFormatSymbols dcfs = locale ? new DecimalFormatSymbols( locale ) : new DecimalFormatSymbols()

        DecimalFormat decimalFormat
        if(!type) {
        	decimalFormat = new java.text.DecimalFormat( format, dcfs )
        } else {
        	if(type=='currency') {
        		decimalFormat = NumberFormat.getCurrencyInstance(locale)
            } else if (type=='number') {
        		decimalFormat = NumberFormat.getNumberInstance(locale)
        	} else if (type=='percent') {
        		decimalFormat = NumberFormat.getPercentInstance(locale)
        	} else {
        		throwTagError("Attribute [type] of Tag [formatNumber] specifies an unknown type. Known types are currency, number and percent.")
        	}
        }

        // ensure formatting accuracy
        decimalFormat.setParseBigDecimal(true)

        if(attrs.get('currencyCode') != null) {
        	Currency currency=Currency.getInstance(attrs.get('currencyCode') as String)
        	decimalFormat.setCurrency(currency)
        }
        if(attrs.get('currencySymbol') != null) {
        	dcfs = decimalFormat.getDecimalFormatSymbols()
        	dcfs.setCurrencySymbol(attrs.get('currencySymbol') as String)
        	decimalFormat.setDecimalFormatSymbols(dcfs)
        }
        if(attrs.get('groupingUsed') != null) {
        	decimalFormat.setGroupingUsed(attrs.get('groupingUsed').toBoolean())
        }
        if(attrs.get('maxIntegerDigits') != null) {
        	decimalFormat.setMaximumIntegerDigits(attrs.get('maxIntegerDigits') as Integer)
        }
        if(attrs.get('minIntegerDigits') != null) {
        	decimalFormat.setMinimumIntegerDigits(attrs.get('minIntegerDigits') as Integer)
        }
        if(attrs.get('maxFractionDigits') != null) {
        	decimalFormat.setMaximumFractionDigits(attrs.get('maxFractionDigits') as Integer)
        }
        if(attrs.get('minFractionDigits') != null) {
        	decimalFormat.setMinimumFractionDigits(attrs.get('minFractionDigits') as Integer)
        }
        if(attrs.get('roundingMode') != null) {
        	def roundingMode=attrs.get('roundingMode')
        	if(!(roundingMode instanceof RoundingMode)) {
        		roundingMode = RoundingMode.valueOf(roundingMode)
        	}
        	decimalFormat.setRoundingMode(roundingMode)
        }

        if(!(number instanceof Number)) {
        	number = decimalFormat.parse(number as String)
        }
        
        def formatted
        try {
        	formatted=decimalFormat.format(number)
        } catch(ArithmeticException e) {
        	// if roundingMode is UNNECESSARY and ArithemeticException raises, just return original number formatted with default number formatting
        	formatted=NumberFormat.getNumberInstance(locale).format(number)
        }
        return formatted
    }

    def resolveLocale(localeAttr) {
         def locale = localeAttr
         if(locale != null && !(locale instanceof Locale)) {
         	locale=StringUtils.parseLocaleString(locale as String)
         }
         if(locale==null) {
         	locale=RCU.getLocale(request)
         	if(locale==null) {
         		locale=Locale.getDefault()
         	}
         }    
         return locale
    }

    def encodeAs = { attrs, body ->
        if (!attrs.codec)
            throwTagError("Tag [encodeAs] requires a codec name in the [codec] attribute")

        return body()?."encodeAs${attrs.codec}"()
    }
}
