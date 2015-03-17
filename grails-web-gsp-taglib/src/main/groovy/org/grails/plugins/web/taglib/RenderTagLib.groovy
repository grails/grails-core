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
package org.grails.plugins.web.taglib

import com.opensymphony.module.sitemesh.*
import com.opensymphony.module.sitemesh.parser.AbstractHTMLPage
import grails.artefact.TagLibrary
import grails.gsp.TagLib
import grails.util.TypeConvertingMap
import groovy.text.Template
import groovy.transform.CompileStatic
import org.grails.buffer.FastStringWriter
import org.grails.buffer.StreamCharBuffer
import org.grails.encoder.CodecLookup
import org.grails.encoder.Encoder
import org.grails.exceptions.ExceptionUtils
import org.grails.gsp.GroovyPageTemplate
import org.grails.gsp.GroovyPagesTemplateEngine
import org.grails.gsp.compiler.GroovyPageParser
import org.grails.taglib.TagLibraryLookup
import org.grails.taglib.TagOutput
import org.grails.taglib.encoder.OutputContextLookupHelper
import org.grails.web.errors.ErrorsViewStackTracePrinter
import org.grails.web.gsp.GroovyPagesTemplateRenderer
import org.grails.web.sitemesh.*
import org.grails.web.util.WebUtils
import org.springframework.http.HttpStatus
import org.springframework.util.StringUtils

import javax.servlet.http.HttpServletRequest

/**
 * Tags to help rendering of views and layouts.
 *
 * @author Graeme Rocher
 */
@CompileStatic
@TagLib
class RenderTagLib implements RequestConstants, TagLibrary {
    GroovyPagesTemplateRenderer groovyPagesTemplateRenderer
    ErrorsViewStackTracePrinter errorsViewStackTracePrinter
    GroovyPagesTemplateEngine groovyPagesTemplateEngine
    TagLibraryLookup gspTagLibraryLookup
    CodecLookup codecLookup
    GroovyPageLayoutFinder groovyPageLayoutFinder

    protected HTMLPage getPage() {
        return (HTMLPage)getRequest().getAttribute(PAGE)
    }
    
    protected boolean isSitemeshPreprocessMode() {
        def preprocessConfig = grailsApplication?.getFlatConfig()?.get(GroovyPageParser.CONFIG_PROPERTY_GSP_SITEMESH_PREPROCESS)
        preprocessConfig == null || preprocessConfig
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
            content = TagOutput.captureTagOutput(gspTagLibraryLookup, 'g', 'include', includeAttrs, null, OutputContextLookupHelper.lookupOutputContext())
        }
        else {
            def oldGspSiteMeshPage = request.getAttribute(GrailsLayoutView.GSP_SITEMESH_PAGE)
            try {
                gspSiteMeshPage = new GSPSitemeshPage()
                request.setAttribute(GrailsLayoutView.GSP_SITEMESH_PAGE, gspSiteMeshPage)
                if (attrs.view || attrs.template) {
                    content = TagOutput.captureTagOutput(gspTagLibraryLookup, 'g', 'render', attrs, null, OutputContextLookupHelper.lookupOutputContext())
                }
                else {
                    def bodyClosure = TagOutput.createOutputCapturingClosure(this, body, OutputContextLookupHelper.lookupOutputContext())
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
                request.setAttribute(GrailsLayoutView.GSP_SITEMESH_PAGE, oldGspSiteMeshPage)
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
            def parser = createPageParser(contentType)
            char[] charArray
            if(content instanceof StreamCharBuffer) {
                charArray = ((StreamCharBuffer)content).toCharArray()
            } else {
                charArray = content.toString().toCharArray()
            }
            page = parser.parse(charArray)
        }

        def decorator = findDecorator(request, attrs.name as String)
        if (decorator && decorator.page) {
            pageParams.each { k, v ->
                page.addProperty(k as String, v as String)
            }
            try {
                request.setAttribute(PAGE, page)
                Template template = findTemplate(decorator)
                template.make(viewModel).writeTo(out)
            }
            finally {
                request.setAttribute(PAGE, oldPage)
            }
        } else {
            out << content
        }
    }

    protected Template findTemplate(Decorator decorator) {
        Template template
        if(decorator instanceof SpringMVCViewDecorator) {
            template = ((SpringMVCViewDecorator)decorator).getTemplate()
            if(template instanceof GroovyPageTemplate) {
                GroovyPageTemplate gpt = (GroovyPageTemplate)template
                gpt = (GroovyPageTemplate)gpt.clone()
                gpt.setAllowSettingContentType(false)
                template = gpt
            }
        } else {
            template = groovyPagesTemplateEngine.createTemplate(decorator.getPage())
        }
        return template
    }

    protected Decorator findDecorator(HttpServletRequest req, String layoutName) {
        DecoratorMapper decoratorMapper = sitemeshFactory?.getDecoratorMapper()
        Decorator d
        if(decoratorMapper) {
            d = decoratorMapper.getNamedDecorator(req, layoutName)
        } else {
            d = groovyPageLayoutFinder.getNamedDecorator(req, layoutName)
        }
        d
    }

    protected PageParser createPageParser(String contentType) {
        PageParser parser = sitemeshFactory?.getPageParser(contentType)
        if(parser == null) {
            parser = new GrailsHTMLPageParser()
        }
        return parser
    }

    protected Factory getSitemeshFactory() {
        return FactoryHolder.getSitemeshFactory()
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
        null
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

        def root = ExceptionUtils.getRootCause(exception)
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
