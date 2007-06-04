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
import org.springframework.validation.Errors;
import org.springframework.context.NoSuchMessageException;
import org.springframework.web.servlet.support.RequestContextUtils as RCU;
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;

 /**
 *  A  tag lib that provides tags for working with form controls
 *
 * @author Graeme Rocher
 * @since 17-Jan-2006
 */

class FormTagLib {
	def out // to facilitate testing

	/**
	 * Creates a new text field
	 */
	def textField = { attrs ->
		attrs.type = "text"  
		attrs.tagName = "textField" 
		def result = field(attrs)
		if(result) {     
			out << result
		}
	}
	/**
	 * Creates a hidden field
	 */
	def hiddenField = { attrs ->
		attrs.type = "hidden"
		attrs.tagName = "hiddenField"
		out << field(attrs)
	}
	/**
	 * Creates a submit button
	 */
	def submitButton = { attrs ->
		attrs.type = "submit"
		attrs.tagName = "submitButton"
		out << field(attrs)
	}
	/**
	 * A general tag for creating fields
	 */
	def field = { attrs ->  
        resolveAttributes( attrs)

		out << "<input type=\"${attrs.remove('type')}\" "
        outputAttributes(attrs)
		out << "/>"
	}

	/**
	 * A general tag for creating textareas
	 */
	def textArea = { attrs ->
	    resolveAttributes(attrs)

        // Pull out the value to use as content not attrib
        def value = attrs.remove( 'value')
        def escapeHtml = true
		if(attrs.escapeHtml) escapeHtml = Boolean.valueOf(attrs.remove('escapeHtml'))

		out << "<textarea "
        outputAttributes(attrs)
		out << ">" << (escapeHtml ? value.encodeAsHTML() : value) << "</textarea>"
	}

    /**
     * Check required attributes, set the id to name if no id supplied, extract bean values etc.
     */
    void resolveAttributes(attrs)
    {
        if(!attrs.name && !attrs.field) {
            throwTagError("Tag [${attrs.tagName}] is missing required attribute [name] or [field]")
        }
        attrs.remove('tagName')

        attrs.id = (!attrs.id ? attrs.name : attrs.id)

        def val = attrs.remove('bean')
        if(val) {
            if(attrs.name.indexOf('.'))
                attrs.name.split('\\.').each { val = val?."$it" }
            else {
                val = val[name]
            }
            attrs.value = val
        }
        attrs.value = (attrs.value ? attrs.value : "")
    }

    /**
     * Dump out attributes in HTML compliant fashion
     */
    void outputAttributes(attrs)
    {
        attrs.each { k,v ->
            out << k << "=\"" << v.encodeAsHTML() << "\" "
        }
    }

    /**
     *  General linking to controllers, actions etc. Examples:
     *
     *  <g:form action="myaction">...</gr:form>
     *  <g:form controller="myctrl" action="myaction">...</gr:form>
     */
    def form = { attrs, body ->
        out << "<form action=\""
        // create the link
        out << createLink(attrs)

        out << '\" '
        // default to post
        if(!attrs['method']) {
            out << 'method="post" '
        }
        // process remaining attributes
        outputAttributes(attrs)

        out << ">"
        // output the body
        def bodyContent = body()
		out << bodyContent

        // close tag
        out << "</form>"
    }
    /**
     * Creates a submit button that submits to an action in the controller specified by the form action
     * The name of the action attribute is translated into the action name, for example "Edit" becomes
     * "_action_edit" or "List People" becomes "_action_listPeople"
     * If the action attribute is not specified, the value attribute will be used as part of the action name
     *
     *  <g:actionSubmit value="Edit" />
     *  <g:actionSubmit action="Edit" value="Some label for editing" />
     *
     */
    def actionSubmit = { attrs ->
    	if(!attrs.value) {
            throwTagError("Tag [$tagName] is missing required attribute [value]")
        }

		// add action and value
		def value = attrs.remove('value')
		def action = attrs.action ? attrs.remove('action') : value
    	
    	out << "<input type=\"submit\" name=\"_action_${action}\" value=\"${value}\" "
    	
    	// process remaining attributes
        outputAttributes(attrs)

        // close tag
        out << '/>'

    }
    /**
     * Creates a an image submit button that submits to an action in the controller specified by the form action
     * The name of the action attribute is translated into the action name, for example "Edit" becomes
     * "_action_edit" or "List People" becomes "_action_listPeople"
     * If the action attribute is not specified, the value attribute will be used as part of the action name
     *
     *  <g:actionSubmitImage src="/images/submitButton.gif" action="Edit" />
     *
     */
    def actionSubmitImage = { attrs ->
        if(!attrs.value) {
            throwTagError("Tag [$tagName] is missing required attribute [value]")
        }
        
        // add action and value
        def value = attrs.remove('value')
		def action = attrs.action ? attrs.remove('action') : value
    
        out << "<input type=\"image\" name=\"_action_${action}\" value=\"${value}\" "

		// add image src        
        def src = attrs.remove('src')
        if(src) {
             out << "src=\"${src}\" "
        }

        // process remaining attributes
        outputAttributes(attrs)

        // close tag
        out << '/>'

    }

   /**
     * A simple date picker that renders a date as selects
     * eg. <g:datePicker name="myDate" value="${new Date()}" />
     */
    def datePicker = { attrs ->
        def xdefault = attrs['default']
		if (xdefault == null) {
			xdefault = new Date()
		} else if (xdefault != 'none') {
			xdefault = DateFormat.getInstance().parse(xdefault)
		} else {
			xdefault = null
		}

        def value = (attrs['value'] ? attrs['value'] : xdefault)
        def name = attrs['name']
        def id = attrs['id'] ? attrs['id'] : name

		def noSelection = attrs['noSelection']
		if (noSelection != null)
		{
		    noSelection = noSelection.entrySet().iterator().next()
		}

		def years = attrs['years']

        final PRECISION_RANKINGS = ["year":0, "month":10, "day":20, "hour":30, "minute":40]
        def precision = (attrs['precision'] ? PRECISION_RANKINGS[attrs['precision']] : PRECISION_RANKINGS["minute"])

        def day
        def month
        def year
        def hour
        def minute
        def dfs = new java.text.DateFormatSymbols(RCU.getLocale(request))

        def c = null
        if(value instanceof Calendar) {
            c = value
        }
        else if (value != null) {
            c = new GregorianCalendar();
            c.setTime(value)
        }

		if (c != null) {
	        day = c.get(GregorianCalendar.DAY_OF_MONTH)
	        month = c.get(GregorianCalendar.MONTH)
	        year = c.get(GregorianCalendar.YEAR)
	        hour = c.get(GregorianCalendar.HOUR_OF_DAY)
	        minute = c.get(GregorianCalendar.MINUTE)
		}

		if (years == null) {
			def tempyear
			if (year == null) {
				// If no year, we need to get current year to setup a default range... ugly
            	def tempc = new GregorianCalendar()
            	tempc.setTime(new Date())
	        	tempyear = tempc.get(GregorianCalendar.YEAR)
			} else {
				tempyear = year
			}
			years = (tempyear-100)..(tempyear+100)
		}

        out << "<input type=\"hidden\" name=\"${name}\" value=\"struct\" />"

        // create day select
        if (precision >= PRECISION_RANKINGS["day"]) {
            out.println "<select name=\"${name}_day\" id=\"${id}_day\">"

            if (noSelection) {
	    		renderNoSelectionOption( noSelection.key, noSelection.value, '')
                out.println()
            }

            for(i in 1..31) {
                out.println "<option value=\"${i}\""
				if (i == day) {
					out.println " selected=\"selected\""
				}
				out.println ">${i}</option>"
            }
            out.println '</select>'
        }

        // create month select
        if (precision >= PRECISION_RANKINGS["month"]) {
            out.println "<select name=\"${name}_month\" id=\"${id}_month\">"

            if (noSelection) {
	    		renderNoSelectionOption( noSelection.key, noSelection.value, '')
                out.println()
            }

            dfs.months.eachWithIndex { m,i ->
                if(m) {
                    def monthIndex = i + 1
                    out << "<option value=\"${monthIndex}\""
                    if(month == i) out << " selected=\"selected\""
                    out << '>'
                    out << m
                    out.println '</option>'
                }
            }
            out.println '</select>'
        }

        // create year select
        if (precision >= PRECISION_RANKINGS["year"]) {
            out.println "<select name=\"${name}_year\" id=\"${id}_year\">"

            if (noSelection) {
    			renderNoSelectionOption( noSelection.key, noSelection.value, '')
                out.println()
            }

            for(i in years) {
                out.println "<option value=\"${i}\""
				if (i == year) {
					out.println " selected=\"selected\""
				}
				out.println ">${i}</option>"
            }
            out.println '</select>'
        }

        // do hour select
        if (precision >= PRECISION_RANKINGS["hour"]) {
            out.println "<select name=\"${name}_hour\" id=\"${id}_hour\">"

            if (noSelection) {
	    		renderNoSelectionOption( noSelection.key, noSelection.value, '')
                out.println()
            }

            for(i in 0..23) {
                def h = '' + i
                if(i < 10) h = '0' + h
                out << "<option value=\"${h}\" "
                if(hour == h.toInteger()) out << "selected=\"selected\""
                out << '>' << h << '</option>'
                out.println()
            }
            out.println '</select> :'

            // If we're rendering the hour, but not the minutes, then display the minutes as 00 in read-only format
            if (precision < PRECISION_RANKINGS["minute"]) {
                out.println '00'
            }
        }

        // do minute select
        if (precision >= PRECISION_RANKINGS["minute"]) {
            out.println "<select name=\"${name}_minute\" id=\"${id}_minute\">"

            if (noSelection) {
	    		renderNoSelectionOption( noSelection.key, noSelection.value, '')
                out.println()
            }

            for(i in 0..59) {
                def m = '' + i
                if(i < 10) m = '0' + m
                out << "<option value=\"${m}\" "
                if(minute == m.toInteger()) out << "selected=\"selected\""
                out << '>' << m << '</option>'
                out.println()
            }
            out.println '</select>'
        }
    }

	def renderNoSelectionOption = { noSelectionKey, noSelectionValue, value ->
		// If a label for the '--Please choose--' first item is supplied, write it out
        out << '<option value="' << (noSelectionKey == null ? "" : noSelectionKey) << '"'
        if(noSelectionKey.equals(value)) {
            out << ' selected="selected" '
        }
        out << '>' << noSelectionValue.encodeAsHTML() << '</option>'
	}

    /**
     *  A helper tag for creating TimeZone selects
     * eg. <g:timeZoneSelect name="myTimeZone" value="${tz}" />
     */
    def timeZoneSelect = { attrs ->
        attrs['from'] = TimeZone.getAvailableIDs();
        attrs['value'] = (attrs['value'] ? attrs['value'].ID : TimeZone.getDefault().ID )
        def date = new Date()

        // set the option value as a closure that formats the TimeZone for display
        attrs['optionValue'] = {
            TimeZone tz = TimeZone.getTimeZone(it);
            def shortName = tz.getDisplayName(tz.inDaylightTime(date),TimeZone.SHORT);
            def longName = tz.getDisplayName(tz.inDaylightTime(date),TimeZone.LONG);

            def offset = tz.rawOffset;
            def hour = offset / (60*60*1000);
            def min = Math.abs(offset / (60*1000)) % 60;

            return "${shortName}, ${longName} ${hour}:${min}"
        }

        // use generic select
        out << select( attrs )
    }

    /**
     *  A helper tag for creating locale selects
     *
     * eg. <g:localeSelect name="myLocale" value="${locale}" />
     */
    def localeSelect = {attrs ->
        attrs['from'] = Locale.getAvailableLocales()
        attrs['value'] = (attrs['value'] ? attrs['value'] : RCU.getLocale(request) )
        // set the key as a closure that formats the locale
        attrs['optionKey'] = { "${it.language}_${it.country}" }
        // set the option value as a closure that formats the locale for display
        attrs['optionValue'] = { "${it.language}, ${it.country},  ${it.displayName}" }

        // use generic select
        out << select( attrs )
    }

    /**
     * A helper tag for creating currency selects
     *
     * eg. <g:currencySelect name="myCurrency" value="${currency}" />
     */
    def currencySelect = { attrs, body ->
        if(!attrs['from']) {
            attrs['from'] = ['EUR', 'XCD','USD','XOF','NOK','AUD','XAF','NZD','MAD','DKK','GBP','CHF','XPF','ILS','ROL','TRL']
        }
		try {
	        def currency = (attrs['value'] ? attrs['value'] : Currency.getInstance( RCU.getLocale(request) ))
	        attrs.value = currency.currencyCode
		}
		catch(IllegalArgumentException iae) {
		   	attrs.value = null
		}
        // invoke generic select
        out << select( attrs )
    }

    /**
     * A helper tag for creating HTML selects
     *
     * Examples:
     * <g:select name="user.age" from="${18..65}" value="${age}" />
     * <g:select name="user.company.id" from="${Company.list()}" value="${user?.company.id}" optionKey="id" />
     */
    def select = { attrs ->
	    def messageSource = grailsAttributes.getApplicationContext().getBean("messageSource")
		def locale = RCU.getLocale(request)

        def from = attrs.remove('from')
        def keys = attrs.remove('keys')
        def optionKey = attrs.remove('optionKey')
        def optionValue = attrs.remove('optionValue')
        def value = attrs.remove('value')
        def valueMessagePrefix = attrs.remove('valueMessagePrefix')
		def noSelection = attrs.remove('noSelection')
        if (noSelection != null) {
            noSelection = noSelection.entrySet().iterator().next()
        }

        out << "<select name=\"${attrs.remove('name')}\" "
        // process remaining attributes
        outputAttributes(attrs)

        out << '>'
        out.println()

        if (noSelection) {
		    renderNoSelectionOption(noSelection.key, noSelection.value, value)
            out.println()
        }

        // create options from list
        if(from) {
            from.eachWithIndex { el,i ->
            	def keyValue = null
                out << '<option '
                if(keys) {
                    keyValue = keys[i]
                    out << 'value="' << keyValue << '" '
                    if(keyValue == value) {
                        out << 'selected="selected" '
                    }
                }
                else if(optionKey) {
                    if(optionKey instanceof Closure) {
                        keyValue = optionKey(el)
                        out << 'value="' << keyValue << '" '
                    }
                    else if(el !=null && optionKey == 'id' && grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, el.getClass().name)) {
                        keyValue = el.ident()
                        out << 'value="' << keyValue << '" '
                    }
                    else {
                        keyValue = el.properties[optionKey]
                        out << 'value="' << keyValue << '" '
                    }

                    if(keyValue == value) {
                        out << 'selected="selected" '
                    }
                }
                else {
                	keyValue = el
                    out << "value=\"${keyValue}\" "
                    if(keyValue == value) {
                        out << 'selected="selected" '
                    }
                }
                out << '>'
                if(optionValue) {
                    if(optionValue instanceof Closure) {
                         out << optionValue(el).toString().encodeAsHTML()
                    }
                    else {
                        out << el.properties[optionValue].toString().encodeAsHTML()
                    }
                }
                else if(valueMessagePrefix) {
                	def message = messageSource.getMessage("${valueMessagePrefix}.${keyValue}", null, null, locale)
                	if(message) {
                		out << message.encodeAsHTML()
                	}
                	else if (keyValue) {
                		out << keyValue.encodeAsHTML()
                	}
					else {
        	            def s = el.toString()
    	                if(s) out << s.encodeAsHTML()
	                }
                }
                else {
                    def s = el.toString()
                    if(s) out << s.encodeAsHTML()
                }
                out << '</option>'
                out.println()
            }
        }
        // close tag
        out << '</select>'
    }

    /**
     * A helper tag for creating checkboxes
     **/
    def checkBox = { attrs ->
          def value = attrs.remove('value')
          def name = attrs.remove('name')
          if(!value) value = false
          out << '<input type="hidden" '
          out << "name=\"_${name}\" />"
          out << '<input type="checkbox" '
          out << "name=\"${name}\" "
          if(value) {
                out << 'checked="checked" '
          }
          out << "value=\"true\" "
        // process remaining attributes
        outputAttributes(attrs)

        // close the tag, with no body
        out << ' />'

    }

    /**
     * A helper tag for creating radio buttons
     */
     def radio = { attrs ->
          def value = attrs.remove('value')
          def name = attrs.remove('name')
          def checked = (attrs.remove('checked') ? true : false)
          out << '<input type="radio" '
          out << "name=\"${name}\" "
          if(checked) {
                out << 'checked="checked" '
          }
          out << "value=\"${value.toString().encodeAsHTML()}\" "
        // process remaining attributes
        outputAttributes(attrs)

        // close the tag, with no body
        out << ' />'
     }
}