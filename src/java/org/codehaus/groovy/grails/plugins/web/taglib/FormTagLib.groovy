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

import org.springframework.web.servlet.support.RequestContextUtils as RCU

import java.text.DateFormat
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.springframework.beans.SimpleTypeConverter
import org.codehaus.groovy.grails.web.util.StreamCharBuffer;
import org.springframework.http.HttpMethod;
import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerToken

/**
*  A  tag lib that provides tags for working with form controls
*
* @author Graeme Rocher
* @since 17-Jan-2006
*/

class FormTagLib {
    def out // to facilitate testing

	def grailsApplication

    /**
      * Creates a new text field
      */
    def textField = {attrs ->
        attrs.type = "text"
        attrs.tagName = "textField"
        fieldImpl(out, attrs)
    }

    /**
     * Creates a new password field
     */
    def passwordField = {attrs ->
        attrs.type = "password"
        attrs.tagName = "passwordField"
        fieldImpl(out, attrs)
    }

    /**
      * Creates a hidden field
      */
    def hiddenField = {attrs ->
        hiddenFieldImpl(out, attrs)        	
    }

    def hiddenFieldImpl(out, attrs) {
	    attrs.type = "hidden"
	    attrs.tagName = "hiddenField"
	    fieldImpl(out, attrs)        	
    }

    /**
      * Creates a submit button
      */
    def submitButton = {attrs ->
        attrs.type = attrs.type ?: "submit"
        attrs.tagName = "submitButton"
        if (request['flowExecutionKey']) {
            attrs.name = attrs.event ? "_eventId_${attrs.event}" : "_eventId_${attrs.name}"
        }
        if(attrs.name && (attrs.value == null)) {
            attrs.value = attrs.name
        }
        fieldImpl(out, attrs)
    }
    /**
      * A general tag for creating fields
      */
    def field = {attrs ->
        fieldImpl(out, attrs)
    }

    def fieldImpl(out, attrs) {
	    resolveAttributes(attrs)
	    attrs.id = attrs.id ? attrs.id : attrs.name
	    out << "<input type=\"${attrs.remove('type')}\" "
	    outputAttributes(attrs)
	    out << "/>"
    }

    /**
    * A helper tag for creating checkboxes
    **/
    def checkBox = {attrs ->
        attrs.id = attrs.id ? attrs.id : attrs.name
        def value = attrs.remove('value')
        def name = attrs.remove('name')
        def disabled = attrs.remove('disabled')
        if (disabled && Boolean.valueOf(disabled)) {
            attrs.disabled = 'disabled'
        }

        // Deal with the "checked" attribute. If it doesn't exist, we
        // default to a value of "true", otherwise we use Groovy Truth
        // to determine whether the HTML attribute should be displayed
        // or not.
        def checked = true
        if (attrs.containsKey('checked')) {
            checked = attrs.remove('checked')
        }

        if (checked instanceof String) checked = Boolean.valueOf(checked)

        if (value == null) value = false

        // the hidden field name should begin with an underscore unless it is
        // a dotted name, then the underscore should be inserted after the last
        // dot
        def lastDotInName = name.lastIndexOf('.')
        def hiddenFieldName = lastDotInName == -1 ? '_' + name : name[0..lastDotInName] + '_' + name[(lastDotInName+1)..-1]
        
        out << "<input type=\"hidden\" name=\"${hiddenFieldName}\" /><input type=\"checkbox\" name=\"${name}\" "
        if (value && checked) {
            out << 'checked="checked" '
        }
        def outputValue = !(value instanceof Boolean || value?.class == boolean.class)
        if (outputValue)
            out << "value=\"${value}\" "
        // process remaining attributes
        outputAttributes(attrs)

        // close the tag, with no body
        out << ' />'

    }
    /**
      * A general tag for creating textareas
      */
    def textArea = {attrs, body ->
        resolveAttributes(attrs)
        attrs.id = attrs.id ? attrs.id : attrs.name
        // Pull out the value to use as content not attrib
        def value = attrs.remove('value')
        if(!value) {
            value = body()
        }

        def escapeHtml = true
        if (attrs.escapeHtml) escapeHtml = Boolean.valueOf(attrs.remove('escapeHtml'))

        out << "<textarea "
        outputAttributes(attrs)
        out << ">" << (escapeHtml ? value.encodeAsHTML() : value) << "</textarea>"
    }

    /**
     * Check required attributes, set the id to name if no id supplied, extract bean values etc.
     */
    void resolveAttributes(attrs)
    {
        if (!attrs.name && !attrs.field) {
            throwTagError("Tag [${attrs.tagName}] is missing required attribute [name] or [field]")
        }
        attrs.remove('tagName')

        attrs.id = (!attrs.id ? attrs.name : attrs.id)

        def val = attrs.remove('bean')
        if (val) {
            if (attrs.name.indexOf('.'))
                attrs.name.split('\\.').each {val = val?."$it"}
            else {
                val = val[name]
            }
            attrs.value = val
        }
        attrs.value = (attrs.value != null ? attrs.value : "")
    }

    /**
     * Dump out attributes in HTML compliant fashion
     */
    void outputAttributes(attrs)
    {
        attrs.remove('tagName') // Just in case one is left
        def writer = getOut()
        attrs.each {k, v ->
            writer << "$k=\"${v.encodeAsHTML()}\" "
        }
    }

    /**
     * Same as <g:form>, except sets the relevant enctype for a file upload form
     */
    def uploadForm = {attrs, body ->
        attrs.enctype = "multipart/form-data"
        out << form(attrs, body)
    }

    
    /**
     *  General linking to controllers, actions etc. Examples:
     *
     *  <g:form action="myaction">...</gr:form>
     *  <g:form controller="myctrl" action="myaction">...</gr:form>
     */
    def form = {attrs, body ->

        def useToken = attrs.remove('useToken')
        def writer = getOut()

        writer << "<form action=\"${createLink(attrs)}\" "
        // default to post
        def method = attrs.remove('method')?.toUpperCase() ?: 'POST'
        def httpMethod = HttpMethod.valueOf(method)
        boolean notGet = httpMethod != HttpMethod.GET
        
        if (notGet) {
            writer << 'method="post" '
        }
        else {
        	writer << 'method="get" '
        }
        // process remaining attributes
        attrs.id = attrs.id ? attrs.id : attrs.name
        if (attrs.id == null) attrs.remove('id')


        outputAttributes(attrs)

        writer << ">"
        if (request['flowExecutionKey']) {
            writer.println()
            hiddenFieldImpl(writer, [name: "execution", value: request['flowExecutionKey']])
        }
        if(notGet && httpMethod != HttpMethod.POST) {
        	hiddenFieldImpl(writer, [name: "_method", value: httpMethod.toString()])
        }
        if(useToken) {            
            def token = SynchronizerToken.store(session)
            writer.println()
            hiddenFieldImpl(writer, [name: SynchronizerToken.KEY, value: token.currentToken])
            writer.println()
            hiddenFieldImpl(writer, [name: SynchronizerToken.URI, value: request.forwardURI])
        }
        
        // output the body
        writer << body()

        // close tag
        writer << "</form>"
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
    def actionSubmit = {attrs ->
        attrs.tagName = "actionSubmit"
        if (!attrs.value) {
            throwTagError("Tag [$attrs.tagName] is missing required attribute [value]")
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
    def actionSubmitImage = {attrs ->
        attrs.tagName = "actionSubmitImage"

        if (!attrs.value) {
            throwTagError("Tag [$attrs.tagName] is missing required attribute [value]")
        }

        // add action and value
        def value = attrs.remove('value')
        def action = attrs.action ? attrs.remove('action') : value

        out << "<input type=\"image\" name=\"_action_${action}\" value=\"${value}\" "

        // add image src
        def src = attrs.remove('src')
        if (src) {
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
    def datePicker = {attrs ->
    	def out = out
        def xdefault = attrs['default']
        if (xdefault == null) {
            xdefault = new Date()
        } else if (xdefault.toString() != 'none') {
            if (xdefault instanceof String) {
                xdefault = DateFormat.getInstance().parse(xdefault)
            }else if(!(xdefault instanceof Date)){
                throwTagError("Tag [datePicker] requires the default date to be a parseable String or a Date")
            }
        } else {
            xdefault = null
        }

        def value = attrs['value']
        if (value.toString() == 'none') {
            value = null
        } else if (!value) {
            value = xdefault
        }
        def name = attrs['name']
        def id = attrs['id'] ? attrs['id'] : name

        def noSelection = attrs['noSelection']
        if (noSelection != null)
        {
            noSelection = noSelection.entrySet().iterator().next()
        }

        def years = attrs['years']

        final PRECISION_RANKINGS = ["year": 0, "month": 10, "day": 20, "hour": 30, "minute": 40]
        def precision = (attrs['precision'] ? PRECISION_RANKINGS[attrs['precision']] :
			(grailsApplication.config.grails.tags.datePicker.default.precision ?
				PRECISION_RANKINGS["${grailsApplication.config.grails.tags.datePicker.default.precision}"] :
				PRECISION_RANKINGS["minute"]))

        def day
        def month
        def year
        def hour
        def minute
        def dfs = new java.text.DateFormatSymbols(RCU.getLocale(request))

        def c = null
        if (value instanceof Calendar) {
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
            years = (tempyear - 100)..(tempyear + 100)
        }

        out.println "<input type=\"hidden\" name=\"${name}\" value=\"date.struct\" />"

        // create day select
        if (precision >= PRECISION_RANKINGS["day"]) {
            out.println "<select name=\"${name}_day\" id=\"${id}_day\">"

            if (noSelection) {
                renderNoSelectionOptionImpl(out, noSelection.key, noSelection.value, '')
                out.println()
            }

            for (i in 1..31) {
                out.println "<option value=\"${i}\"${i == day ? ' selected="selected"' : ''}>${i}</option>"
            }
            out.println '</select>'
        }

        // create month select
        if (precision >= PRECISION_RANKINGS["month"]) {
            out.println "<select name=\"${name}_month\" id=\"${id}_month\">"

            if (noSelection) {
                renderNoSelectionOptionImpl(out, noSelection.key, noSelection.value, '')
                out.println()
            }

            dfs.months.eachWithIndex {m, i ->
                if (m) {
                    def monthIndex = i + 1
                    out.println "<option value=\"${monthIndex}\"${i == month ? ' selected="selected"' : ''}>$m</option>"
                }
            }
            out.println '</select>'
        }

        // create year select
        if (precision >= PRECISION_RANKINGS["year"]) {
            out.println "<select name=\"${name}_year\" id=\"${id}_year\">"

            if (noSelection) {
                renderNoSelectionOptionImpl(out, noSelection.key, noSelection.value, '')
                out.println()
            }

            for (i in years) {
                out.println "<option value=\"${i}\"${i == year ? ' selected="selected"' : ''}>${i}</option>"
            }
            out.println '</select>'
        }

        // do hour select
        if (precision >= PRECISION_RANKINGS["hour"]) {
            out.println "<select name=\"${name}_hour\" id=\"${id}_hour\">"

            if (noSelection) {
                renderNoSelectionOptionImpl(out, noSelection.key, noSelection.value, '')
                out.println()
            }

            for (i in 0..23) {
                def h = '' + i
                if (i < 10) h = '0' + h
                out.println "<option value=\"${h}\"${i == hour ? ' selected="selected"' : ''}>$h</option>"                
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
                renderNoSelectionOptionImpl(out, noSelection.key, noSelection.value, '')
                out.println()
            }

            for (i in 0..59) {
                def m = '' + i
                if (i < 10) m = '0' + m
                out.println "<option value=\"${m}\"${i == minute ? ' selected="selected"' : ''}>$m</option>"
            }
            out.println '</select>'
        }
    }

    def renderNoSelectionOption = {noSelectionKey, noSelectionValue, value ->
    	renderNoSelectionOptionImpl(out, noSelectionKey, noSelectionValue, value)
    }

    def renderNoSelectionOptionImpl(out, noSelectionKey, noSelectionValue, value) {
        // If a label for the '--Please choose--' first item is supplied, write it out
        out << "<option value=\"${(noSelectionKey == null ? '' : noSelectionKey)}\"${noSelectionKey == value ? ' selected="selected"' : ''}>${noSelectionValue.encodeAsHTML()}</option>"
    }

    /**
     *  A helper tag for creating TimeZone selects
     * eg. <g:timeZoneSelect name="myTimeZone" value="${tz}" />
     */
    def timeZoneSelect = {attrs ->
        attrs['from'] = TimeZone.getAvailableIDs();
        attrs['value'] = (attrs['value'] ? attrs['value'].ID : TimeZone.getDefault().ID)
        def date = new Date()

        // set the option value as a closure that formats the TimeZone for display
        attrs['optionValue'] = {
            TimeZone tz = TimeZone.getTimeZone(it);
            def shortName = tz.getDisplayName(tz.inDaylightTime(date), TimeZone.SHORT);
            def longName = tz.getDisplayName(tz.inDaylightTime(date), TimeZone.LONG);

            def offset = tz.rawOffset;
            def hour = offset / (60 * 60 * 1000);
            def min = Math.abs(offset / (60 * 1000)) % 60;

            return "${shortName}, ${longName} ${hour}:${min}"
        }

        // use generic select
        out << select(attrs)
    }

    /**
     *  A helper tag for creating locale selects
     *
     * eg. <g:localeSelect name="myLocale" value="${locale}" />
     */
    def localeSelect = {attrs ->
        attrs['from'] = Locale.getAvailableLocales()
        attrs['value'] = (attrs['value'] ? attrs['value'] : RCU.getLocale(request))
        // set the key as a closure that formats the locale
        attrs['optionKey'] = {"${it.language}_${it.country}"}
        // set the option value as a closure that formats the locale for display
        attrs['optionValue'] = {"${it.language}, ${it.country},  ${it.displayName}"}

        // use generic select
        out << select(attrs)
    }

    /**
     * A helper tag for creating currency selects
     *
     * eg. <g:currencySelect name="myCurrency" value="${currency}" />
     */
    def currencySelect = {attrs, body ->
        if (!attrs['from']) {
            attrs['from'] = ['EUR', 'XCD', 'USD', 'XOF', 'NOK', 'AUD', 'XAF', 'NZD', 'MAD', 'DKK', 'GBP', 'CHF', 'XPF', 'ILS', 'ROL', 'TRL']
        }
        try {
            def currency = (attrs['value'] ? attrs['value'] : Currency.getInstance(RCU.getLocale(request)))
            attrs.value = currency.currencyCode
        }
        catch (IllegalArgumentException iae) {
            attrs.value = null
        }
        // invoke generic select
        out << select(attrs)
    }

    /**
     * A helper tag for creating HTML selects
     *
     * Examples:
     * <g:select name="user.age" from="${18..65}" value="${age}" />
     * <g:select name="user.company.id" from="${Company.list()}" value="${user?.company.id}" optionKey="id" />
     */
    def select = {attrs ->
        def messageSource = grailsAttributes.getApplicationContext().getBean("messageSource")
        def locale = RCU.getLocale(request)
        def writer = out
        attrs.id = attrs.id ? attrs.id : attrs.name
        def from = attrs.remove('from')
        def keys = attrs.remove('keys')
        def optionKey = attrs.remove('optionKey')
        def optionValue = attrs.remove('optionValue')
        def value = attrs.remove('value')
        if (value instanceof Collection && attrs.multiple == null) {
            attrs.multiple = 'multiple'
        }
        if (value instanceof StreamCharBuffer) {
        	value = value.toString()
        }
        def valueMessagePrefix = attrs.remove('valueMessagePrefix')
        def noSelection = attrs.remove('noSelection')
        if (noSelection != null) {
            noSelection = noSelection.entrySet().iterator().next()
        }
        def disabled = attrs.remove('disabled')
        if (disabled && Boolean.valueOf(disabled)) {
            attrs.disabled = 'disabled'
        }

        writer << "<select name=\"${attrs.remove('name')?.encodeAsHTML()}\" "
        // process remaining attributes
        outputAttributes(attrs)

        writer << '>'
        writer.println()

        if (noSelection) {
            renderNoSelectionOptionImpl(writer, noSelection.key, noSelection.value, value)
            writer.println()
        }

        // create options from list
        if (from) {
            from.eachWithIndex {el, i ->
                def keyValue = null
                writer << '<option '
                if (keys) {
                    keyValue = keys[i]
                    writeValueAndCheckIfSelected(keyValue, value, writer)
                }
                else if (optionKey) {
                    def keyValueObject = null
                    if (optionKey instanceof Closure) {
                        keyValue = optionKey(el)
                    }
                    else if (el != null && optionKey == 'id' && grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, el.getClass().name)) {
                        keyValue = el.ident()
                        keyValueObject = el
                    }
                    else {
                        keyValue = el[optionKey]
                        keyValueObject = el
                    }
                    writeValueAndCheckIfSelected(keyValue, value, writer, keyValueObject)
                }
                else {
                    keyValue = el
                    writeValueAndCheckIfSelected(keyValue, value, writer)
                }
                writer << '>'
                if (optionValue) {
                    if (optionValue instanceof Closure) {
                        writer << optionValue(el).toString().encodeAsHTML()
                    }
                    else {
                        writer << el[optionValue].toString().encodeAsHTML()
                    }
                }
                else if (valueMessagePrefix) {
                    def message = messageSource.getMessage("${valueMessagePrefix}.${keyValue}", null, null, locale)
                    if (message != null) {
                        writer << message.encodeAsHTML()
                    }
                    else if (keyValue) {
                        writer << keyValue.encodeAsHTML()
                    }
                    else {
                        def s = el.toString()
                        if (s) writer << s.encodeAsHTML()
                    }
                }
                else {
                    def s = el.toString()
                    if (s) writer << s.encodeAsHTML()
                }
                writer << '</option>'
                writer.println()
            }
        }
        // close tag
        writer << '</select>'
    }

    def typeConverter = new SimpleTypeConverter()
    private writeValueAndCheckIfSelected(keyValue, value, writer) {
        writeValueAndCheckIfSelected(keyValue, value, writer, null)
    }

    private writeValueAndCheckIfSelected(keyValue, value, writer, el) {

        boolean selected = false
        def keyClass = keyValue?.getClass()
        if (keyClass.isInstance(value)) {
            selected = (keyValue == value)
        }
        else if (value instanceof Collection) {
            // first try keyValue
            selected = value.contains(keyValue)
            if (! selected && el != null) {
                selected = value.contains(el)
            }
        }
        else if (keyClass && value) {
            try {
                value = typeConverter.convertIfNecessary(value, keyClass)
                selected = (keyValue == value)
            } catch (Exception) {
                // ignore
            }
        }
        writer << "value=\"${keyValue}\" "
        if (selected) {
            writer << 'selected="selected" '
        }
    }

    /**
     * A helper tag for creating radio buttons
     */
    def radio = {attrs ->
        def value = attrs.remove('value')
        attrs.id = attrs.id ? attrs.id : attrs.name
        def name = attrs.remove('name')
        def disabled = attrs.remove('disabled')
        if (disabled && Boolean.valueOf(disabled)) {
            attrs.disabled = 'disabled'
        }
        def checked = (attrs.remove('checked') ? true : false)
        out << "<input type=\"radio\" name=\"${name}\"${ checked ? ' checked="checked" ' : ' '}value=\"${value?.toString()?.encodeAsHTML()}\" "
        // process remaining attributes
        outputAttributes(attrs)

        // close the tag, with no body
        out << ' />'
    }

    /**
    * A helper tag for creating radio button groups
    */
    def radioGroup = {attrs, body ->
        def value = attrs.remove('value')
        def values = attrs.remove('values')
        def labels = attrs.remove('labels')
        def name = attrs.remove('name')
        values.eachWithIndex {val, idx ->
            def it = new Expando();
            it.radio = "<input type=\"radio\" name=\"${name}\" "
            if (value?.toString().equals(val.toString())) {
                it.radio += 'checked="checked" '
            }
            it.radio += "value=\"${val.toString().encodeAsHTML()}\" />"

            it.label = labels == null ? 'Radio ' + val : labels[idx]

            out << body(it)
            out.println()
        }
    }
}