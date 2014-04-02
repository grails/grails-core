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
package org.codehaus.groovy.grails.plugins.web.taglib

import grails.artefact.Artefact
import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.support.encoding.CodecLookup
import org.codehaus.groovy.grails.support.encoding.Encoder
import org.codehaus.groovy.grails.web.errors.ErrorsViewStackTracePrinter
import org.codehaus.groovy.grails.web.errors.GrailsExceptionResolver
import org.codehaus.groovy.grails.web.mapping.ForwardUrlMappingInfo
import org.codehaus.groovy.grails.web.mapping.UrlMapping
import org.codehaus.groovy.grails.web.mapping.UrlMappingUtils
import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods
import org.codehaus.groovy.grails.web.pages.FastStringWriter
import org.codehaus.groovy.grails.web.pages.GroovyPage
import org.codehaus.groovy.grails.web.pages.GroovyPageParser
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateRenderer
import org.codehaus.groovy.grails.web.pages.TagLibraryLookup
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.sitemesh.FactoryHolder
import org.codehaus.groovy.grails.web.sitemesh.GSPSitemeshPage
import org.codehaus.groovy.grails.web.sitemesh.GrailsPageFilter
import org.codehaus.groovy.grails.web.util.StreamCharBuffer
import org.codehaus.groovy.grails.web.util.TypeConvertingMap
import org.codehaus.groovy.grails.web.util.WebUtils
import org.springframework.http.HttpStatus
import org.springframework.util.StringUtils
import org.springframework.web.servlet.support.RequestContextUtils as RCU

import com.opensymphony.module.sitemesh.Factory
import com.opensymphony.module.sitemesh.Page
import com.opensymphony.module.sitemesh.RequestConstants
import com.opensymphony.module.sitemesh.parser.AbstractHTMLPage

/**
 * Tags to help rendering of views and layouts.
 *
 * @author Graeme Rocher
 */
@CompileStatic
@Artefact("TagLibrary")
class RenderTagLib implements RequestConstants {
    GroovyPagesTemplateRenderer groovyPagesTemplateRenderer
    ErrorsViewStackTracePrinter errorsViewStackTracePrinter
    GroovyPagesTemplateEngine groovyPagesTemplateEngine
    TagLibraryLookup gspTagLibraryLookup
    CodecLookup codecLookup

    protected AbstractHTMLPage getPage() {
        return (AbstractHTMLPage)getRequest().getAttribute(PAGE)
    }
    
    protected boolean isSitemeshPreprocessMode() {
        def preprocessConfig = grailsApplication?.getFlatConfig()?.get(GroovyPageParser.CONFIG_PROPERTY_GSP_SITEMESH_PREPROCESS)
        preprocessConfig == null || preprocessConfig
    }

    /**
     * Includes another controller/action within the current response.<br/>
     *
     * &lt;g:include controller="foo" action="test"&gt;&lt;/g:include&gt;<br/>
     *
     * @emptyTag
     *
     * @attr controller The name of the controller
     * @attr action The name of the action
     * @attr id The identifier
     * @attr params Any parameters
     * @attr view The name of the view. Cannot be specified in combination with controller/action/id
     * @attr model A model to pass onto the included controller in the request
     */
    Closure include = { Map attrs, body ->
        if (attrs.action && !attrs.controller) {
            def controller = request?.getAttribute(GrailsApplicationAttributes.CONTROLLER)
            def controllerName = ((GroovyObject)controller)?.getProperty(ControllerDynamicMethods.CONTROLLER_NAME_PROPERTY)
            attrs.controller = controllerName
        }

        if (attrs.controller || attrs.view) {
            def mapping = new ForwardUrlMappingInfo(controller: attrs.controller as String,
                    action: attrs.action as String,
                    view: attrs.view as String,
                    id: attrs.id as String,
                    params: attrs.params as Map)
            
            if (attrs.namespace != null) {
                mapping.namespace = attrs.namespace as String
            }
            if (attrs.plugin != null) {
                mapping.pluginName = attrs.plugin as String
            }
            out << UrlMappingUtils.includeForUrlMappingInfo(request, response, mapping, (Map)(attrs.model ?: [:]))?.content
        }
    }

    /**
     * Apply a layout to a particular block of text or to the given view or template.<br/>
     *
     * &lt;g:applyLayout name="myLayout"&gt;some text&lt;/g:applyLayout&gt;<br/>
     * &lt;g:applyLayout name="myLayout" template="mytemplate" /&gt;<br/>
     * &lt;g:applyLayout name="myLayout" url="http://www.google.com" /&gt;<br/>
     * &lt;g:applyLayout name="myLayout" action="myAction" controller="myController"&gt;<br/>
     *
     * @attr name The name of the layout
     * @attr template Optional. The template to apply the layout to
     * @attr url Optional. The URL to retrieve the content from and apply a layout to
     * @attr action Optional. The action to be called to generate the content to apply the layout to
     * @attr controller Optional. The controller that contains the action that will generate the content to apply the layout to
     * @attr contentType Optional. The content type to use, default is "text/html"
     * @attr encoding Optional. The encoding to use
     * @attr params Optional. The params to pass onto the page object
     * @attr parse Optional. If true, Sitemesh parser will always be used to parse the content.
     */
    Closure applyLayout = { Map attrs, body ->
        if (!groovyPagesTemplateEngine) throw new IllegalStateException("Property [groovyPagesTemplateEngine] must be set!")
        def oldPage = getPage()
        String contentType = attrs.contentType ? attrs.contentType as String : "text/html"

        Map pageParams = attrs.params instanceof Map ? (Map)attrs.params : [:]
        Map viewModel = attrs.model instanceof Map ? (Map)attrs.model : [:]
        Object content = null
        GSPSitemeshPage gspSiteMeshPage = null
        if (attrs.url) {
            content = new URL(attrs.url as String).getText("UTF-8")
        }
        else if (attrs.action && attrs.controller) {
            def includeAttrs = [action: attrs.action, controller: attrs.controller, params: pageParams, model: viewModel]
            content = GroovyPage.captureTagOutput(gspTagLibraryLookup, 'g', 'include', includeAttrs, null, webRequest)
        }
        else {
            def oldGspSiteMeshPage = request.getAttribute(GrailsPageFilter.GSP_SITEMESH_PAGE)
            try {
                gspSiteMeshPage = new GSPSitemeshPage()
                request.setAttribute(GrailsPageFilter.GSP_SITEMESH_PAGE, gspSiteMeshPage)
                if (attrs.view || attrs.template) {
                    content = GroovyPage.captureTagOutput(gspTagLibraryLookup, 'g', 'render', attrs, null, webRequest)
                }
                else {
                    def bodyClosure = GroovyPage.createOutputCapturingClosure(this, body, webRequest)
                    content = bodyClosure()
                }
                if (content instanceof StreamCharBuffer) {
                    gspSiteMeshPage.setPageBuffer(content)
                    gspSiteMeshPage.setUsed(isSitemeshPreprocessMode())
                }
                else if (content != null) {
                    FastStringWriter stringWriter=new FastStringWriter()
                    stringWriter.print((Object)content)
                    gspSiteMeshPage.setPageBuffer(stringWriter.buffer)
                    gspSiteMeshPage.setUsed(isSitemeshPreprocessMode())
                }
            }
            finally {
                request.setAttribute(GrailsPageFilter.GSP_SITEMESH_PAGE, oldGspSiteMeshPage)
            }
        }
        if(content==null) {
            content=''
        }

        Page page = null
        if (!((TypeConvertingMap)attrs).boolean('parse') && gspSiteMeshPage != null && gspSiteMeshPage.isUsed()) {
            page = gspSiteMeshPage
        }
        else {
            def parser = getFactory().getPageParser(contentType)
            char[] charArray
            if(content instanceof StreamCharBuffer) {
                charArray = ((StreamCharBuffer)content).toCharArray()
            } else {
                charArray = content.toString().toCharArray()
            }
            page = parser.parse(charArray)
        }

        def decoratorMapper = getFactory().getDecoratorMapper()
        if (decoratorMapper) {
            def d = decoratorMapper.getNamedDecorator(request, attrs.name as String)
            if (d && d.page) {
                pageParams.each { k, v ->
                    page.addProperty(k as String, v as String)
                }
                try {
                    request.setAttribute(PAGE, page)
                    def t = groovyPagesTemplateEngine.createTemplate(d.getPage())
                    def w = t.make(viewModel)
                    w.writeTo(out)
                }
                finally {
                    request.setAttribute(PAGE, oldPage)
                }
            }
        }
    }

    private Factory getFactory() {
        return FactoryHolder.getFactory()
    }
    
    private callLink(Map attrs, Object body) {
        GroovyPage.captureTagOutput(gspTagLibraryLookup, 'g', 'link', attrs, body, webRequest)
    }

    /**
     * Used to retrieve a property of the decorated page.<br/>
     *
     * &lt;g:pageProperty default="defaultValue" name="body.onload" /&gt;<br/>
     *
     * @emptyTag
     *
     * @attr REQUIRED name the property name
     * @attr default the default value to use if the property is null
     * @attr writeEntireProperty if true, writes the property in the form 'foo = "bar"', otherwise renders 'bar'
     */
    Closure pageProperty = { Map attrs ->
        if (!attrs.name) {
            throwTagError("Tag [pageProperty] is missing required attribute [name]")
        }

        String propertyName = attrs.name as String
        def htmlPage = getPage()
        def propertyValue

        if (htmlPage instanceof GSPSitemeshPage) {
            // check if there is an component content buffer
            propertyValue = ((GSPSitemeshPage)htmlPage).getContentBuffer(propertyName)
        }

        if (!propertyValue) {
            propertyValue = htmlPage.getProperty(propertyName)
        }

        if (!propertyValue) {
            propertyValue = attrs.'default'
        }

        if (propertyValue) {
            if (attrs.writeEntireProperty) {
                out << ' '
                out << propertyName.substring(propertyName.lastIndexOf('.') + 1)
                out << "=\""
                out << propertyValue
                out << "\""
            }
            else {
                out << propertyValue
            }
        }
    }

    /**
     * Invokes the body of this tag if the page property exists:<br/>
     *
     * &lt;g:ifPageProperty name="meta.index"&gt;body to invoke&lt;/g:ifPageProperty&gt;<br/>
     *
     * or it equals a certain value:<br/>
     *
     * &lt;g:ifPageProperty name="meta.index" equals="blah"&gt;body to invoke&lt;/g:ifPageProperty&gt;
     *
     * @attr name REQUIRED the property name
     * @attr equals optional value to test against
     */
    Closure ifPageProperty = { Map attrs, body ->
        if (!attrs.name) {
            return
        }

        def htmlPage = getPage()
        List names = ((attrs.name instanceof List) ? (List)attrs.name : [attrs.name])

        def invokeBody = true
        for (i in 0..<names.size()) {
            String propertyName = names[i] as String
            def propertyValue = null
            if (htmlPage instanceof GSPSitemeshPage) {
                // check if there is an component content buffer
                propertyValue = htmlPage.getContentBuffer(propertyName)
            }

            if (!propertyValue) {
                propertyValue = htmlPage.getProperty(propertyName)
            }

            if (propertyValue) {
                if (attrs.containsKey('equals')) {
                    if (attrs.equals instanceof List) {
                        invokeBody = ((List)attrs.equals)[i] == propertyValue
                    }
                    else {
                        invokeBody = attrs.equals == propertyValue
                    }
                }
            }
            else {
                invokeBody = false
                break
            }
        }
        if (invokeBody && body instanceof Closure) {
            out << body()
        }
    }

    /**
     * Used in layouts to render the page title from the SiteMesh page.<br/>
     *
     * &lt;g:layoutTitle default="The Default title" /&gt;
     *
     * @emptyTag
     *
     * @attr default the value to use if the title isn't specified in the GSP
     */
    Closure layoutTitle = { Map attrs ->
        String title = page.title
        if (!title && attrs.'default') title = attrs.'default'
        if (title) out << title
    }

    /**
     * Used in layouts to render the body of a SiteMesh layout.<br/>
     *
     * &lt;g:layoutBody /&gt;
     *
     * @emptyTag
     */
    Closure layoutBody = { Map attrs ->
        getPage().writeBody(out)
    }

    /**
     * Used in layouts to render the head of a SiteMesh layout.<br/>
     *
     * &lt;g:layoutHead /&gt;
     *
     * @emptyTag
     */
    Closure layoutHead = { Map attrs ->
        getPage().writeHead(out)
    }

    /**
     * Creates next/previous links to support pagination for the current controller.<br/>
     *
     * &lt;g:paginate total="${Account.count()}" /&gt;<br/>
     *
     * @emptyTag
     *
     * @attr total REQUIRED The total number of results to paginate
     * @attr action the name of the action to use in the link, if not specified the default action will be linked
     * @attr controller the name of the controller to use in the link, if not specified the current controller will be linked
     * @attr id The id to use in the link
     * @attr params A map containing request parameters
     * @attr prev The text to display for the previous link (defaults to "Previous" as defined by default.paginate.prev property in I18n messages.properties)
     * @attr next The text to display for the next link (defaults to "Next" as defined by default.paginate.next property in I18n messages.properties)
     * @attr omitPrev Whether to not show the previous link (if set to true, the previous link will not be shown)
     * @attr omitNext Whether to not show the next link (if set to true, the next link will not be shown)
     * @attr omitFirst Whether to not show the first link (if set to true, the first link will not be shown)
     * @attr omitLast Whether to not show the last link (if set to true, the last link will not be shown)
     * @attr max The number of records displayed per page (defaults to 10). Used ONLY if params.max is empty
     * @attr maxsteps The number of steps displayed for pagination (defaults to 10). Used ONLY if params.maxsteps is empty
     * @attr offset Used only if params.offset is empty
     * @attr mapping The named URL mapping to use to rewrite the link
     * @attr fragment The link fragment (often called anchor tag) to use
     */
    Closure paginate = { Map attrsMap ->
        TypeConvertingMap attrs = (TypeConvertingMap)attrsMap
        def writer = out
        if (attrs.total == null) {
            throwTagError("Tag [paginate] is missing required attribute [total]")
        }

        def messageSource = grailsAttributes.messageSource
        def locale = RCU.getLocale(request)

        def total = attrs.int('total') ?: 0
        def offset = params.int('offset') ?: 0
        def max = params.int('max')
        def maxsteps = (attrs.int('maxsteps') ?: 10)

        if (!offset) offset = (attrs.int('offset') ?: 0)
        if (!max) max = (attrs.int('max') ?: 10)

        Map linkParams = [:]
        if (attrs.params instanceof Map) linkParams.putAll((Map)attrs.params)
        linkParams.offset = offset - max
        linkParams.max = max
        if (params.sort) linkParams.sort = params.sort
        if (params.order) linkParams.order = params.order

        Map linkTagAttrs = [:]
        def action
        if (attrs.containsKey('mapping')) {
            linkTagAttrs.mapping = attrs.mapping
            action = attrs.action
        } else {
            action = attrs.action ?: params.action
        }
        if (action) {
            linkTagAttrs.action = action
        }
        if (attrs.controller) {
            linkTagAttrs.controller = attrs.controller
        }
        if (attrs.containsKey(UrlMapping.PLUGIN)) {
            linkTagAttrs.put(UrlMapping.PLUGIN, attrs.get(UrlMapping.PLUGIN))
        }
        if (attrs.containsKey(UrlMapping.NAMESPACE)) {
            linkTagAttrs.put(UrlMapping.NAMESPACE, attrs.get(UrlMapping.NAMESPACE))
        }
        if (attrs.id != null) {
            linkTagAttrs.id = attrs.id
        }
        if (attrs.fragment != null) {
            linkTagAttrs.fragment = attrs.fragment
        }
        linkTagAttrs.params = linkParams

        // determine paging variables
        def steps = maxsteps > 0
        int currentstep = ((offset / max) as int) + 1
        int firststep = 1
        int laststep = Math.round(Math.ceil(total / max)) as int

        // display previous link when not on firststep unless omitPrev is true
        if (currentstep > firststep && !attrs.boolean('omitPrev')) {
            linkTagAttrs.put('class', 'prevLink')
            linkParams.offset = offset - max
            writer << callLink((Map)linkTagAttrs.clone()) {
                (attrs.prev ?: messageSource.getMessage('paginate.prev', null, messageSource.getMessage('default.paginate.prev', null, 'Previous', locale), locale))
            }
        }

        // display steps when steps are enabled and laststep is not firststep
        if (steps && laststep > firststep) {
            linkTagAttrs.put('class', 'step')

            // determine begin and endstep paging variables
            int beginstep = currentstep - (Math.round(maxsteps / 2.0d) as int) + (maxsteps % 2)
            int endstep = currentstep + (Math.round(maxsteps / 2.0d) as int) - 1

            if (beginstep < firststep) {
                beginstep = firststep
                endstep = maxsteps
            }
            if (endstep > laststep) {
                beginstep = laststep - maxsteps + 1
                if (beginstep < firststep) {
                    beginstep = firststep
                }
                endstep = laststep
            }

            // display firststep link when beginstep is not firststep
            if (beginstep > firststep && !attrs.boolean('omitFirst')) {
                linkParams.offset = 0
                writer << callLink((Map)linkTagAttrs.clone()) {firststep.toString()}
            }
            //show a gap if beginstep isn't immediately after firststep, and if were not omitting first or rev
            if (beginstep > firststep+1 && (!attrs.boolean('omitFirst') || !attrs.boolean('omitPrev')) ) {
                writer << '<span class="step gap">..</span>'
            }

            // display paginate steps
            (beginstep..endstep).each { int i ->
                if (currentstep == i) {
                    writer << "<span class=\"currentStep\">${i}</span>"
                }
                else {
                    linkParams.offset = (i - 1) * max
                    writer << callLink((Map)linkTagAttrs.clone()) {i.toString()}
                }
            }

            //show a gap if beginstep isn't immediately before firststep, and if were not omitting first or rev
            if (endstep+1 < laststep && (!attrs.boolean('omitLast') || !attrs.boolean('omitNext'))) {
                writer << '<span class="step gap">..</span>'
            }
            // display laststep link when endstep is not laststep
            if (endstep < laststep && !attrs.boolean('omitLast')) {
                linkParams.offset = (laststep - 1) * max
                writer << callLink((Map)linkTagAttrs.clone()) { laststep.toString() }
            }
        }

        // display next link when not on laststep unless omitNext is true
        if (currentstep < laststep && !attrs.boolean('omitNext')) {
            linkTagAttrs.put('class', 'nextLink')
            linkParams.offset = offset + max
            writer << callLink((Map)linkTagAttrs.clone()) {
                (attrs.next ? attrs.next : messageSource.getMessage('paginate.next', null, messageSource.getMessage('default.paginate.next', null, 'Next', locale), locale))
            }
        }
    }

    /**
     * Renders a sortable column to support sorting in list views.<br/>
     *
     * Attribute title or titleKey is required. When both attributes are specified then titleKey takes precedence,
     * resulting in the title caption to be resolved against the message source. In case when the message could
     * not be resolved, the title will be used as title caption.<br/>
     *
     * Examples:<br/>
     *
     * &lt;g:sortableColumn property="title" title="Title" /&gt;<br/>
     * &lt;g:sortableColumn property="title" title="Title" style="width: 200px" /&gt;<br/>
     * &lt;g:sortableColumn property="title" titleKey="book.title" /&gt;<br/>
     * &lt;g:sortableColumn property="releaseDate" defaultOrder="desc" title="Release Date" /&gt;<br/>
     * &lt;g:sortableColumn property="releaseDate" defaultOrder="desc" title="Release Date" titleKey="book.releaseDate" /&gt;<br/>
     *
     * @emptyTag
     *
     * @attr property - name of the property relating to the field
     * @attr defaultOrder default order for the property; choose between asc (default if not provided) and desc
     * @attr title title caption for the column
     * @attr titleKey title key to use for the column, resolved against the message source
     * @attr params a map containing request parameters
     * @attr action the name of the action to use in the link, if not specified the list action will be linked
     * @attr params A map containing URL query parameters
     * @attr class CSS class name
     */
    Closure sortableColumn = { Map attrs ->
        def writer = out
        if (!attrs.property) {
            throwTagError("Tag [sortableColumn] is missing required attribute [property]")
        }

        if (!attrs.title && !attrs.titleKey) {
            throwTagError("Tag [sortableColumn] is missing required attribute [title] or [titleKey]")
        }

        def property = attrs.remove("property")
        def action = attrs.action ? attrs.remove("action") : (actionName ?: "list")

        def defaultOrder = attrs.remove("defaultOrder")
        if (defaultOrder != "desc") defaultOrder = "asc"

        // current sorting property and order
        def sort = params.sort
        def order = params.order

        // add sorting property and params to link params
        Map linkParams = [:]
        if (params.id) linkParams.put("id", params.id)
        def paramsAttr = attrs.remove("params")
        if (paramsAttr instanceof Map) linkParams.putAll(paramsAttr)
        linkParams.sort = property

        // propagate "max" and "offset" standard params
        if (params.max) linkParams.max = params.max
        if (params.offset) linkParams.offset = params.offset

        // determine and add sorting order for this column to link params
        attrs['class'] = (attrs['class'] ? "${attrs['class']} sortable" : "sortable")
        if (property == sort) {
            attrs['class'] = (attrs['class'] as String) + " sorted " + order
            if (order == "asc") {
                linkParams.order = "desc"
            }
            else {
                linkParams.order = "asc"
            }
        }
        else {
            linkParams.order = defaultOrder
        }

        // determine column title
        String title = attrs.remove("title") as String
        String titleKey = attrs.remove("titleKey") as String
        Object mapping = attrs.remove('mapping')
        if (titleKey) {
            if (!title) title = titleKey
            def messageSource = grailsAttributes.messageSource
            def locale = RCU.getLocale(request)
            title = messageSource.getMessage(titleKey, null, title, locale)
        }

        writer << "<th "
        // process remaining attributes
        Encoder htmlEncoder = codecLookup.lookupEncoder('HTML')
        attrs.each { k, v ->
            writer << k
            writer << "=\""
            writer << htmlEncoder.encode(v)
            writer << "\" "
        }
        writer << '>'
        Map linkAttrs = [:]
        linkAttrs.params = linkParams
        if (mapping) {
            linkAttrs.mapping = mapping
        }

        linkAttrs.action = action
        
        writer << callLink((Map)linkAttrs) {
            title
        }
        writer << '</th>'
    }

    /**
     * Renders a template inside views for collections, models and beans. Examples:<br/>
     *
     * &lt;g:render template="atemplate" collection="${users}" /&gt;<br/>
     * &lt;g:render template="atemplate" model="[user:user,company:company]" /&gt;<br/>
     * &lt;g:render template="atemplate" bean="${user}" /&gt;<br/>
     *
     * @attr template REQUIRED The name of the template to apply
     * @attr contextPath the context path to use (relative to the application context path). Defaults to "" or path to the plugin for a plugin view or template.
     * @attr bean The bean to apply the template against
     * @attr model The model to apply the template against as a java.util.Map
     * @attr collection A collection of model objects to apply the template to
     * @attr var The variable name of the bean to be referenced in the template
     * @attr plugin The plugin to look for the template in
     */
    Closure render = { Map attrs, body ->
        groovyPagesTemplateRenderer.render(getWebRequest(), getPageScope(), attrs, body, getOut())
    }

    /**
     * Renders an exception for the errors view
     *
     * @attr exception REQUIRED The exception to render
     */
    Closure renderException = { Map attrs ->
        if (!(attrs?.exception instanceof Throwable)) {
              return
        }
        Throwable exception = (Throwable)attrs.exception
        
        Encoder htmlEncoder = codecLookup.lookupEncoder('HTML')

        def currentOut = out
        int statusCode = request.getAttribute('javax.servlet.error.status_code') as int
        currentOut << """<h1>Error ${prettyPrintStatus(statusCode)}</h1>
<dl class="error-details">
<dt>URI</dt><dd>${htmlEncoder.encode(WebUtils.getForwardURI(request) ?: request.getAttribute('javax.servlet.error.request_uri'))}</dd>
"""

        def root = GrailsExceptionResolver.getRootCause(exception)
        currentOut << "<dt>Class</dt><dd>${root?.getClass()?.name ?: exception.getClass().name}</dd>"
        currentOut << "<dt>Message</dt><dd>${htmlEncoder.encode(exception.message)}</dd>"
        if (root != null && root != exception && root.message != exception.message) {
            currentOut << "<dt>Caused by</dt><dd>${htmlEncoder.encode(root.message)}</dd>"
        }
        currentOut << "</dl>"

        currentOut << errorsViewStackTracePrinter.prettyPrintCodeSnippet(exception)

        def trace = errorsViewStackTracePrinter.prettyPrint(exception.cause ?: exception)
        if (StringUtils.hasText(trace.trim())) {
            currentOut << "<h2>Trace</h2>"
            currentOut << '<pre class="stack">'
            currentOut << htmlEncoder.encode(trace)
            currentOut << '</pre>'
        }
    }

    private String prettyPrintStatus(int statusCode) {
        String httpStatusReason = HttpStatus.valueOf(statusCode).getReasonPhrase()
        "$statusCode: ${httpStatusReason}"
    }
}
