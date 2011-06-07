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
package org.codehaus.groovy.grails.plugins.web.taglib

import grails.artefact.Artefact

import java.text.DateFormat
import java.text.DateFormatSymbols

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.codehaus.groovy.grails.web.util.StreamCharBuffer
import org.springframework.beans.SimpleTypeConverter
import org.springframework.context.MessageSourceResolvable
import org.springframework.http.HttpMethod
import org.springframework.web.servlet.support.RequestContextUtils as RCU

/**
 * Tags for working with form controls.
 *
 * @author Graeme Rocher
 */
@Artefact("TagLibrary")
class FormTagLib {

    private static final DEFAULT_CURRENCY_CODES = ['EUR', 'XCD', 'USD', 'XOF', 'NOK', 'AUD',
                                                   'XAF', 'NZD', 'MAD', 'DKK', 'GBP', 'CHF',
                                                   'XPF', 'ILS', 'ROL', 'TRL']

    def out // to facilitate testing

    def grailsApplication

    /**
     * Creates a new text field.
     *
     * @emptyTag
     * 
     * @attr name REQUIRED the field name
     * @attr value the field value
     */
    def textField = { attrs ->
        attrs.type = "text"
        attrs.tagName = "textField"
        fieldImpl(out, attrs)
    }

    /**
     * Creates a new password field.
     *
     * @emptyTag
     * 
     * @attr name REQUIRED the field name
     * @attr value the field value
     */
    def passwordField = { attrs ->
        attrs.type = "password"
        attrs.tagName = "passwordField"
        fieldImpl(out, attrs)
    }

    /**
     * Creates a hidden field.
     *
     * @attr name REQUIRED the field name
     * @attr value the field value
     */
    def hiddenField = { attrs ->
        hiddenFieldImpl(out, attrs)
    }

    def hiddenFieldImpl(out, attrs) {
        attrs.type = "hidden"
        attrs.tagName = "hiddenField"
        fieldImpl(out, attrs)
    }

    /**
     * Creates a submit button.
     *
     * @emptyTag
     * 
     * @attr name REQUIRED the field name
     * @attr value the button text
     * @attr type input type; defaults to 'submit'
     * @attr event the webflow event id
     */
    def submitButton = { attrs ->
        attrs.type = attrs.type ?: "submit"
        attrs.tagName = "submitButton"
        if (request.flowExecutionKey) {
            attrs.name = attrs.event ? "_eventId_${attrs.event}" : "_eventId_${attrs.name}"
        }
        if (attrs.name && (attrs.value == null)) {
            attrs.value = attrs.name
        }
        fieldImpl(out, attrs)
    }

    /**
     * A general tag for creating fields.
     *
     * @attr type REQUIRED the input type
     */
    def field = { attrs ->
        fieldImpl(out, attrs)
    }

    def fieldImpl(out, attrs) {
        resolveAttributes(attrs)
        attrs.id = attrs.id ?: attrs.name
        out << "<input type=\"${attrs.remove('type')}\" "
        outputAttributes(attrs, getOut())
        out << "/>"
    }

    /**
     * A helper tag for creating checkboxes.
     *
     * @emptyTag
     * 
     * @attr name REQUIRED the name of the checkbox
     * @attr value  the value of the checkbox
     * @attr checked if evaluates to true sets to checkbox to checked
     * @attr disabled if evaluates to true sets to checkbox to disabled
     * @attr id DOM element id; defaults to name
     */
    def checkBox = { attrs ->
        attrs.id = attrs.id ?: attrs.name
        def value = attrs.remove('value')
        def name = attrs.remove('name')
        def disabled = attrs.remove('disabled')
        if (disabled && Boolean.valueOf(disabled)) {
            attrs.disabled = 'disabled'
        }

        // Deal with the "checked" attribute. If it doesn't exist, we
        // default to a value of "true", otherwise we use Groovy Truth
        // to determine whether the HTML attribute should be displayed or not.
        def checked = true
        def checkedAttributeWasSpecified = false
        if (attrs.containsKey('checked')) {
            checkedAttributeWasSpecified = true
            checked = attrs.remove('checked')
        }

        if (checked instanceof String) checked = Boolean.valueOf(checked)

        if (value == null) value = false

        out << "<input type=\"hidden\" name=\"_${name}\" /><input type=\"checkbox\" name=\"${name}\" "

        if (checkedAttributeWasSpecified) {
            if (checked) {
                out << 'checked="checked" '
            }
        }
        else if (value) {
            out << 'checked="checked" '
        }

        def outputValue = !(value instanceof Boolean || value?.class == boolean.class)
        if (outputValue) {
            out << "value=\"${value}\" "
        }
        // process remaining attributes
        outputAttributes(attrs, getOut())

        // close the tag, with no body
        out << ' />'
    }

    /**
     * A general tag for creating textareas.
     *
     * @attr name REQUIRED the name of the textarea
     * @attr value  the text of the textarea; if not specified renders the body as the text
     * @attr escapeHtml if true escapes the text as HTML
     * @attr id DOM element id; defaults to name
     */
    def textArea = { attrs, body ->
        resolveAttributes(attrs)
        attrs.id = attrs.id ?: attrs.name
        // Pull out the value to use as content not attrib
        def value = attrs.remove('value')
        if (!value) {
            value = body()
        }

        boolean escapeHtml = true
        if (attrs.escapeHtml) escapeHtml = Boolean.valueOf(attrs.remove('escapeHtml'))

        out << "<textarea "
        outputAttributes(attrs, getOut())
        out << ">" << (escapeHtml ? value.encodeAsHTML() : value) << "</textarea>"
    }

    /**
     * Check required attributes, set the id to name if no id supplied, extract bean values etc.
     */
    void resolveAttributes(attrs) {
        if (!attrs.name && !attrs.field) {
            throwTagError("Tag [${attrs.tagName}] is missing required attribute [name] or [field]")
        }

        attrs.remove('tagName')

        attrs.id = attrs.id ?: attrs.name

        def val = attrs.remove('bean')
        if (val) {
            if (attrs.name.indexOf('.')) {
                attrs.name.split('\\.').each {val = val?."$it"}
            }
            else {
                val = val[name]
            }
            attrs.value = val
        }
        attrs.value = attrs.value != null ? attrs.value : "" // can't use ?: since 0 is groovy false
    }

    /**
     * Dump out attributes in HTML compliant fashion.
     */
    void outputAttributes(attrs, writer) {
        attrs.remove('tagName') // Just in case one is left
        attrs.each { k, v ->
            writer << "$k=\"${v.encodeAsHTML()}\" "
        }
    }

    /**
     * Same as &lt;g:form&gt;, except sets the relevant enctype for a file upload form.
     *
     * @attr action the name of the action to use in the link, if not specified the default action will be linked
     * @attr controller the name of the controller to use in the link, if not specified the current controller will be linked
     * @attr id The id to use in the link
     * @attr url A map containing the action,controller,id etc.
     * @attr name A value to use for both the name and id attribute of the form tag
     * @attr useToken Set whether to send a token in the request to handle duplicate form submissions. See Handling Duplicate Form Submissions
     * @attr method the form method to use, either 'POST' or 'GET'; defaults to 'POST'
     */
    def uploadForm = { attrs, body ->
        attrs.enctype = "multipart/form-data"
        out << form(attrs, body)
    }

    /**
     * General linking to controllers, actions etc. Examples:<br/>
     *
     * &lt;g:form action="myaction"&gt;...&lt;/g:form&gt;<br/>
     * &lt;g:form controller="myctrl" action="myaction"&gt;...&lt;/g:form&gt;<br/>
     *
     * @attr action the name of the action to use in the link, if not specified the default action will be linked
     * @attr controller the name of the controller to use in the link, if not specified the current controller will be linked
     * @attr id The id to use in the link
     * @attr url A map containing the action,controller,id etc.
     * @attr name A value to use for both the name and id attribute of the form tag
     * @attr useToken Set whether to send a token in the request to handle duplicate form submissions. See Handling Duplicate Form Submissions
     * @attr method the form method to use, either 'POST' or 'GET'; defaults to 'POST'
     */
    def form = { attrs, body ->

        def useToken = attrs.remove('useToken')
        def writer = getOut()

        def linkAttrs = attrs.subMap(LinkGenerator.LINK_ATTRIBUTES)

        writer << "<form action=\"${createLink(linkAttrs)}\" "

        // if URL is not nul remove attributes
        if (attrs.url == null) {
            attrs = attrs - linkAttrs
        }
        else {
            attrs.remove('url')
        }

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

        outputAttributes(attrs, getOut())

        writer << ">"
        if (request['flowExecutionKey']) {
            writer.println()
            hiddenFieldImpl(writer, [name: "execution", value: request['flowExecutionKey']])
        }

        if (notGet && httpMethod != HttpMethod.POST) {
            hiddenFieldImpl(writer, [name: "_method", value: httpMethod.toString()])
        }

        if (useToken) {
            def tokensHolder = SynchronizerTokensHolder.store(session)
            writer.println()
            hiddenFieldImpl(writer, [name: SynchronizerTokensHolder.TOKEN_KEY, value: tokensHolder.generateToken(request.forwardURI)])
            writer.println()
            hiddenFieldImpl(writer, [name: SynchronizerTokensHolder.TOKEN_URI, value: request.forwardURI])
        }

        // output the body
        writer << body()

        // close tag
        writer << "</form>"
    }

    /**
     * Creates a submit button that submits to an action in the controller specified by the form action.<br/>
     * The name of the action attribute is translated into the action name, for example "Edit" becomes
     * "_action_edit" or "List People" becomes "_action_listPeople".<br/>
     * If the action attribute is not specified, the value attribute will be used as part of the action name.
     *
     * &lt;g:actionSubmit value="Edit" /&gt;<br/>
     * &lt;g:actionSubmit action="Edit" value="Some label for editing" /&gt;<br/>
     *
     * @emptyTag
     * 
     * @attr value REQUIRED The title of the button and name of action when not explicitly defined.
     * @attr action The name of the action to be executed, otherwise it is derived from the value.
     */
    def actionSubmit = { attrs ->
        if (!attrs.value) {
            throwTagError("Tag [actionSubmit] is missing required attribute [value]")
        }

        attrs.tagName = "actionSubmit"

        // Strip out any 'name' attribute, since this tag overrides it.
        if (attrs.name) {
            log.warn "[actionSubmit] 'name' attribute will be ignored"
            attrs.remove('name')
        }

        // add action and value
        def value = attrs.remove('value')
        def action = attrs.remove('action') ?: value

        out << "<input type=\"submit\" name=\"_action_${action}\" value=\"${value}\" "

        // process remaining attributes
        outputAttributes(attrs, getOut())

        // close tag
        out << '/>'
    }

    /**
     * Creates a an image submit button that submits to an action in the controller specified by the form action.
     * The name of the action attribute is translated into the action name, for example "Edit" becomes
     * "_action_edit" or "List People" becomes "_action_listPeople".<br/>
     * If the action attribute is not specified, the value attribute will be used as part of the action name.<br/>
     *
     * &lt;g:actionSubmitImage src="/images/submitButton.gif" action="Edit" /&gt;
     *
     * @emptyTag
     * 
     * @attr value REQUIRED The title of the button and name of action when not explicitly defined.
     * @attr action The name of the action to be executed, otherwise it is derived from the value.
     * @attr src The source of the image to use
     */
    def actionSubmitImage = { attrs ->
        attrs.tagName = "actionSubmitImage"

        if (!attrs.value) {
            throwTagError("Tag [$attrs.tagName] is missing required attribute [value]")
        }

        // add action and value
        def value = attrs.remove('value')
        def action = attrs.remove('action') ?: value

        out << "<input type=\"image\" name=\"_action_${action}\" value=\"${value}\" "

        // add image src
        def src = attrs.remove('src')
        if (src) {
            out << "src=\"${src}\" "
        }

        // process remaining attributes
        outputAttributes(attrs, getOut())

        // close tag
        out << '/>'
    }

    /**
     * A simple date picker that renders a date as selects.<br/>
     * e.g. &lt;g:datePicker name="myDate" value="${new Date()}" /&gt;
     *
     * @emptyTag
     * 
     * @attr name REQUIRED The name of the date picker field set
     * @attr value The current value of the date picker; defaults to now if not specified
     * @attr precision The desired granularity of the date to be rendered
     * @attr noSelection A single-entry map detailing the key and value to use for the "no selection made" choice in the select box. If there is no current selection this will be shown as it is first in the list, and if submitted with this selected, the key that you provide will be submitted. Typically this will be blank.
     * @attr years A list or range of years to display, in the order specified. i.e. specify 2007..1900 for a reverse order list going back to 1900. If this attribute is not specified, a range of years from the current year - 100 to current year + 100 will be shown.
     * @attr id the DOM element id
     */
    def datePicker = { attrs ->
        def out = out // let x = x ?
        def xdefault = attrs['default']
        if (xdefault == null) {
            xdefault = new Date()
        }
        else if (xdefault.toString() != 'none') {
            if (xdefault instanceof String) {
                xdefault = DateFormat.getInstance().parse(xdefault)
            }
            else if (!(xdefault instanceof Date)) {
                throwTagError("Tag [datePicker] requires the default date to be a parseable String or a Date")
            }
        }
        else {
            xdefault = null
        }

        def value = attrs.value
        if (value.toString() == 'none') {
            value = null
        }
        else if (!value) {
            value = xdefault
        }
        def name = attrs.name
        def id = attrs.id ?: name

        def noSelection = attrs.noSelection
        if (noSelection != null) {
            noSelection = noSelection.entrySet().iterator().next()
        }

        def years = attrs.years

        final PRECISION_RANKINGS = ["year": 0, "month": 10, "day": 20, "hour": 30, "minute": 40]
        def precision = (attrs.precision ? PRECISION_RANKINGS[attrs.precision] :
            (grailsApplication.config.grails.tags.datePicker.default.precision ?
                PRECISION_RANKINGS["${grailsApplication.config.grails.tags.datePicker.default.precision}"] :
                PRECISION_RANKINGS["minute"]))

        def day
        def month
        def year
        def hour
        def minute
        def dfs = new DateFormatSymbols(RCU.getLocale(request))

        def c = null
        if (value instanceof Calendar) {
            c = value
        }
        else if (value != null) {
            c = new GregorianCalendar()
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
            }
            else {
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
     * A helper tag for creating TimeZone selects.<br/>
     * eg. &lt;g:timeZoneSelect name="myTimeZone" value="${tz}" /&gt;
     *
     * @emptyTag
     * 
     * @attr name REQUIRED The name of the select
     * @attr value An instance of java.util.TimeZone. Defaults to the time zone for the current Locale if not specified
     */
    def timeZoneSelect = { attrs ->
        attrs.from = TimeZone.getAvailableIDs()
        attrs.value = (attrs.value ? attrs.value.ID : TimeZone.getDefault().ID)
        def date = new Date()

        // set the option value as a closure that formats the TimeZone for display
        attrs.optionValue = {
            TimeZone tz = TimeZone.getTimeZone(it)
            def shortName = tz.getDisplayName(tz.inDaylightTime(date), TimeZone.SHORT)
            def longName = tz.getDisplayName(tz.inDaylightTime(date), TimeZone.LONG)

            def offset = tz.rawOffset
            def hour = offset / (60 * 60 * 1000)
            def min = Math.abs(offset / (60 * 1000)) % 60

            return "${shortName}, ${longName} ${hour}:${min}"
        }

        // use generic select
        out << select(attrs)
    }

    /**
     * A helper tag for creating locale selects.<br/>
     *
     * eg. &lt;g:localeSelect name="myLocale" value="${locale}" /&gt;
     *
     * @emptyTag
     * 
     * @attr name REQUIRED The name of the select
     * @attr value The set locale, defaults to the current request locale if not specified
     */
    def localeSelect = { attrs ->
        attrs.from = Locale.getAvailableLocales()
        attrs.value = (attrs.value ?: RCU.getLocale(request))?.toString()
        // set the key as a closure that formats the locale
        attrs.optionKey = { it.country ? "${it.language}_${it.country}" : it.language }
        // set the option value as a closure that formats the locale for display
        attrs.optionValue = {it.country ? "${it.language}, ${it.country},  ${it.displayName}" : "${it.language}, ${it.displayName}" }

        // use generic select
        out << select(attrs)
    }

    /**
     * A helper tag for creating currency selects.<br/>
     *
     * eg. &lt;g:currencySelect name="myCurrency" value="${currency}" /&gt;
     * 
     * @emptyTag
     *
     * @attr from The currency symbols to select from, defaults to the major ones if not specified
     * @attr value The currency value as the currency code. Defaults to the currency for the current Locale if not specified
     */
    def currencySelect = { attrs, body ->
        if (!attrs.from) {
            attrs.from = DEFAULT_CURRENCY_CODES
        }
        try {
            def currency = attrs.value ?: Currency.getInstance(RCU.getLocale(request))
            attrs.value = currency.currencyCode
        }
        catch (IllegalArgumentException iae) {
            attrs.value = null
        }
        // invoke generic select
        out << select(attrs)
    }

    /**
     * A helper tag for creating HTML selects.<br/>
     *
     * Examples:<br/>
     * &lt;g:select name="user.age" from="${18..65}" value="${age}" /&gt;<br/>
     * &lt;g:select name="user.company.id" from="${Company.list()}" value="${user?.company.id}" optionKey="id" /&gt;<br/>
     *
     * @emptyTag
     * 
     * @attr name REQUIRED the select name
     * @attr id the DOM element id - uses the name attribute if not specified
     * @attr from REQUIRED The list or range to select from
     * @attr keys A list of values to be used for the value attribute of each "option" element.
     * @attr optionKey By default value attribute of each &lt;option&gt; element will be the result of a "toString()" call on each element. Setting this allows the value to be a bean property of each element in the list.
     * @attr optionValue By default the body of each &lt;option&gt; element will be the result of a "toString()" call on each element in the "from" attribute list. Setting this allows the value to be a bean property of each element in the list.
     * @attr value The current selected value that evaluates equals() to true for one of the elements in the from list.
     * @attr multiple boolean value indicating whether the select a multi-select (automatically true if the value is a collection, defaults to false - single-select)
     * @attr valueMessagePrefix By default the value "option" element will be the result of a "toString()" call on each element in the "from" attribute list. Setting this allows the value to be resolved from the I18n messages. The valueMessagePrefix will be suffixed with a dot ('.') and then the value attribute of the option to resolve the message. If the message could not be resolved, the value is presented.
     * @attr noSelection A single-entry map detailing the key and value to use for the "no selection made" choice in the select box. If there is no current selection this will be shown as it is first in the list, and if submitted with this selected, the key that you provide will be submitted. Typically this will be blank - but you can also use 'null' in the case that you're passing the ID of an object
     * @attr disabled boolean value indicating whether the select is disabled or enabled (defaults to false - enabled)
     */
    def select = { attrs ->
        if (!attrs.name) {
            throwTagError("Tag [select] is missing required attribute [name]")
        }
        if (!attrs.containsKey('from')) {
            throwTagError("Tag [select] is missing required attribute [from]")
        }
        def messageSource = grailsAttributes.getApplicationContext().getBean("messageSource")
        def locale = RCU.getLocale(request)
        def writer = out
        attrs.id = attrs.id ?: attrs.name
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
        outputAttributes(attrs, getOut())

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
                else if (el instanceof MessageSourceResolvable) {
                    writer << messageSource.getMessage(el, locale)
                }
                else if (valueMessagePrefix) {
                    def message = messageSource.getMessage("${valueMessagePrefix}.${keyValue}", null, null, locale)
                    if (message != null) {
                        writer << message.encodeAsHTML()
                    }
                    else if (keyValue && keys) {
                        def s = el.toString()
                        if (s) writer << s.encodeAsHTML()
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
            }
            catch (e) {
                // ignore
            }
        }
        writer << "value=\"${keyValue}\" "
        if (selected) {
            writer << 'selected="selected" '
        }
    }

    /**
     * A helper tag for creating radio buttons.
     *
     * @emptyTag
     * 
     * @attr value REQUIRED The value of the radio button
     * @attr name REQUIRED The name of the radio button
     * @attr checked boolean to indicate that the radio button should be checked
     * @attr disabled boolean to indicate that the radio button should be disabled
     * @attr id the DOM element id
     */
    def radio = { attrs ->
        def value = attrs.remove('value')
        attrs.id = attrs.id ?: attrs.name
        def name = attrs.remove('name')
        def disabled = attrs.remove('disabled')
        if (disabled && Boolean.valueOf(disabled)) {
            attrs.disabled = 'disabled'
        }
        def checked = attrs.remove('checked') ? true : false
        out << "<input type=\"radio\" name=\"${name}\"${ checked ? ' checked="checked" ' : ' '}value=\"${value?.toString()?.encodeAsHTML()}\" "
        // process remaining attributes
        outputAttributes(attrs, getOut())

        // close the tag, with no body
        out << ' />'
    }

    /**
     * A helper tag for creating radio button groups.
     *
     * @attr name REQUIRED The name of the group
     * @attr values REQUIRED The list values for the radio buttons
     * @attr value The current selected value
     * @attr labels Labels for each value contained in the values list. If this is ommitted the label property on the iterator variable (see below) will default to 'Radio ' + value.
     */
    def radioGroup = { attrs, body ->
        def value = attrs.remove('value')
        def values = attrs.remove('values')
        def labels = attrs.remove('labels')
        def name = attrs.remove('name')
        values.eachWithIndex {val, idx ->
            def it = new Expando()
            it.radio = new StringBuilder("<input type=\"radio\" name=\"${name}\" ")
            if (value?.toString().equals(val.toString())) {
                it.radio << 'checked="checked" '
            }
            it.radio << "value=\"${val.toString().encodeAsHTML()}\" "

            // process remaining attributes
            outputAttributes(attrs, it.radio)
            it.radio << "/>"

            it.label = labels == null ? 'Radio ' + val : labels[idx]

            out << body(it)
            out.println()
        }
    }
}
