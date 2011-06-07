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
import grails.util.GrailsUtil
import grails.util.Metadata
import org.apache.commons.io.FilenameUtils
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

 /**
 * The base application tag library for Grails many of which take inspiration from Rails helpers (thanks guys! :)
 * This tag library tends to get extended by others as tags within here can be re-used in said libraries
 *
 * @author Graeme Rocher
 */
@Artefact("TagLibrary")
class ApplicationTagLib implements ApplicationContextAware, InitializingBean, GrailsApplicationAware {

    ApplicationContext applicationContext
    GrailsPluginManager pluginManager
    GrailsApplication grailsApplication

    UrlMappingsHolder grailsUrlMappingsHolder

    @Autowired
    LinkGenerator linkGenerator

    def resourceService

    static final SCOPES = [page: 'pageScope',
                           application: 'servletContext',
                           request:'request',
                           session:'session',
                           flash:'flash']

    boolean useJsessionId = false

    void afterPropertiesSet() {
        def config = applicationContext.getBean(GrailsApplication.APPLICATION_ID).config
        if (config.grails.views.enable.jsessionid instanceof Boolean) {
            useJsessionId = config.grails.views.enable.jsessionid
        }
    }

    /**
     * Obtains the value of a cookie.
     *
     * @emptyTag
     * 
     * @attr name REQUIRED the cookie name
     */
    def cookie = { attrs ->
        def cke = request.cookies.find { it.name == attrs.name }
        if (cke) {
            out << cke.value
        }
    }

    /**
     * Renders the specified request header value.
     *
     * @emptyTag
     * 
     * @attr name REQUIRED the header name
     */
    def header = { attrs ->
        if (attrs.name) {
            def hdr = request.getHeader(attrs.name)
            if (hdr) out << hdr
        }
    }

    /**
     * Sets a variable in the pageContext or the specified scope.
     *
     * @attr var REQUIRED the variable name
     * @attr value the variable value; if not specified uses the rendered body
     * @attr scope the scope name; defaults to pageScope
     */
    def set = { attrs, body ->
        def var = attrs.var
        if (!var) throw new IllegalArgumentException("[var] attribute must be specified to for <g:set>!")

        def scope = attrs.scope ? SCOPES[attrs.scope] : 'pageScope'
        if (!scope) throw new IllegalArgumentException("Invalid [scope] attribute for tag <g:set>!")

        def value = attrs.value
        def containsValue = attrs.containsKey('value')

        if (!containsValue && body) value = body()

        this."$scope"."$var" = value
        null
    }

    /**
     * Creates a link to a resource, generally used as a method rather than a tag.<br/>
     *
     * eg. &lt;link type="text/css" href="${createLinkTo(dir:'css',file:'main.css')}" /&gt;
     * 
     * @emptyTag
     * 
     */
    def createLinkTo = { attrs ->
        GrailsUtil.deprecated "Tag [createLinkTo] is deprecated please use [resource] instead"
        out << resource(attrs)
    }

    /**
     * Creates a link to a resource, generally used as a method rather than a tag.<br/>
     *
     * eg. &lt;link type="text/css" href="${resource(dir:'css',file:'main.css')}" /&gt;
     *
     * @emptyTag
     * 
     * @attr base Sets the prefix to be added to the link target address, typically an absolute server URL. This overrides the behaviour of the absolute property, if both are specified.x≈
     * @attr contextPath the context path to use (relative to the application context path). Defaults to "" or path to the plugin for a plugin view or template.
     * @attr dir the name of the directory within the grails app to link to
     * @attr file the name of the file within the grails app to link to
     * @attr absolute If set to "true" will prefix the link target address with the value of the grails.serverURL property from Config, or http://localhost:&lt;port&gt; if no value in Config and not running in production.
     * @attr plugin The plugin to look for the resource in
     */
    def resource = { attrs ->
        if (pageScope.pluginContextPath) {
            attrs.pluginContextPath = pageScope.pluginContextPath
        }
        // Use resources plugin if present, but only if file is specified - resources require files
        // But users often need to link to a folder just using dir
        out << ((resourceService && attrs.file) ? r.resource(attrs) : linkGenerator.resource(attrs))
    }

    /**
     * Render an img tag with src set to a static resource
     * @attr dir Optional name of resource directory, defaults to "images"
     * @attr file Name of resource file (optional if uri specified)
     * @attr plugin Optional the name of the grails plugin if the resource is not part of the application
     * @attr uri Optional app-relative URI path of the resource if not using dir/file attributes - only if Resources plugin is in use
     */
    def img = { attrs ->
        if (!attrs.uri && !attrs.dir) {
            attrs.dir = "images"
        }
        if (resourceService) {
            out << r.img(attrs)
        } else {
            def uri = attrs.uri ?: resource(attrs)

            def excludes = ['dir', 'uri', 'file', 'plugin']
            def entries = attrs.findAll { !(it.key in excludes) }.collect { "$it.key=\"$it.value\""}
            out << "<img src=\"${uri.encodeAsHTML()}\" ${entries.join(' ')} />"
        }
    }

    /**
     * General linking to controllers, actions etc. Examples:<br/>
     *
     * &lt;g:link action="myaction"&gt;link 1&lt;/gr:link&gt;<br/>
     * &lt;g:link controller="myctrl" action="myaction"&gt;link 2&lt;/gr:link&gt;<br/>
     *
     * @attr controller The name of the controller to use in the link, if not specified the current controller will be linked
     * @attr action The name of the action to use in the link, if not specified the default action will be linked
     * @attr uri relative URI
     * @attr url A map containing the action,controller,id etc.
     * @attr base Sets the prefix to be added to the link target address, typically an absolute server URL. This overrides the behaviour of the absolute property, if both are specified.
     * @attr absolute If set to "true" will prefix the link target address with the value of the grails.serverURL property from Config, or http://localhost:&lt;port&gt; if no value in Config and not running in production.
     * @attr id The id to use in the link
     * @attr fragment The link fragment (often called anchor tag) to use
     * @attr params A map containing URL query parameters
     * @attr mapping The named URL mapping to use to rewrite the link
     * @attr event Webflow _eventId parameter
     * @attr elementId DOM element id
     */
    def link = { attrs, body ->

        def writer = getOut()
        def elementId = attrs.remove('elementId')
        def linkAttrs
        if (attrs.params instanceof Map && attrs.params.containsKey('attrs')) {
            linkAttrs = attrs.params.remove('attrs').clone()
        }
        else {
            linkAttrs = [:]
        }
        writer <<  '<a href=\"'
        writer << createLink(attrs).encodeAsHTML()
        writer << '"'
        if (elementId) {
            writer << " id=\"${elementId}\""
        }

        def remainingKeys = attrs.keySet() - LinkGenerator.LINK_ATTRIBUTES
        for (key in remainingKeys) {
            writer << " $key=\"${attrs[key]?.encodeAsHTML()}\""
        }
        for (entry in linkAttrs) {
            writer << " ${entry.key}=\"${entry.value?.encodeAsHTML()}\""
        }
        writer << '>'
        writer << body()
        writer << '</a>'
    }

    static attrsToString(Map attrs) {
        // Output any remaining user-specified attributes
        final resultingAttributes = attrs.entrySet().collect { "$it.key=\"${it.value.encodeAsHTML()}\""}.join(' ')
        return " $resultingAttributes"
    }

    static LINK_WRITERS = [
        js: { url, constants, attrs ->
           return "<script src=\"${url}\"${getAttributesToRender(constants, attrs)}></script>"
        },

        link: { url, constants, attrs ->
           return "<link href=\"${url}\"${getAttributesToRender(constants, attrs)}/>"
        }
    ]

    static getAttributesToRender(constants, attrs) {
        return "${constants ? attrsToString(constants) : ''}${attrs ? attrsToString(attrs) : ''}"
    }

    static SUPPORTED_TYPES = [
        css:[type:"text/css", rel:'stylesheet', media:'screen, projector'],
        js:[type:'text/javascript', writer:'js'],

        gif:[rel:'shortcut icon'],
        jpg:[rel:'shortcut icon'],
        png:[rel:'shortcut icon'],
        ico:[rel:'shortcut icon'],
        appleicon:[rel:'apple-touch-icon']

        // @todo add feed link types here too
    ]

    /**
     * Render the appropriate kind of external link for use in <head> based on the type of the URI.
     * For JS will render <script> tags, for CSS will render <link> with the correct rel, and so on for icons.
     * @attr uri
     * @attr dir
     * @attr file
     * @attr plugin
     * @attr type
     */
    def external = { attrs ->
        if (!attrs.uri) {
            attrs.uri = g.resource(attrs).toString()
        }
        renderResourceLink(attrs)
    }

    /**
     *
     * @attr uri
     * @attr type
     */
    protected renderResourceLink(attrs) {
        def uri = attrs.remove('uri')
        def type = attrs.remove('type')
        if (!type) {
            type = FilenameUtils.getExtension(uri)
        }

        def typeInfo = SUPPORTED_TYPES[type]?.clone()
        if (!typeInfo) {
            throwTagError "I can't work out the type of ${uri} with type [${type}]. Please check the URL, resource definition or specify [type] attribute"
        }

        def writerName = typeInfo.remove('writer')
        def writer = LINK_WRITERS[writerName ?: 'link']

        // Allow attrs to overwrite any constants
        attrs.each { typeInfo.remove(it.key) }

        out << writer(uri, typeInfo, attrs)
        out << "\r\n"
    }

    /**
     * Creates a grails application link from a set of attributes. This
     * link can then be included in links, ajax calls etc. Generally used as a method call
     * rather than a tag eg.<br/>
     *
     * &lt;a href="${createLink(action:'list')}"&gt;List&lt;/a&gt;
     *
     * @emptyTag
     * 
     * @attr controller The name of the controller to use in the link, if not specified the current controller will be linked
     * @attr action The name of the action to use in the link, if not specified the default action will be linked
     * @attr uri relative URI
     * @attr url A map containing the action,controller,id etc.
     * @attr base Sets the prefix to be added to the link target address, typically an absolute server URL. This overrides the behaviour of the absolute property, if both are specified.
     * @attr absolute If set to "true" will prefix the link target address with the value of the grails.serverURL property from Config, or http://localhost:&lt;port&gt; if no value in Config and not running in production.
     * @attr id The id to use in the link
     * @attr fragment The link fragment (often called anchor tag) to use
     * @attr params A map containing URL query parameters
     * @attr mapping The named URL mapping to use to rewrite the link
     * @attr event Webflow _eventId parameter
     */
    def createLink = { attrs ->
        def urlAttrs = attrs
        if (attrs.url instanceof Map) {
           urlAttrs = attrs.url
        }
        def params = urlAttrs.params && urlAttrs.params instanceof Map ? urlAttrs.params : [:]
        if (request['flowExecutionKey']) {
            params."execution" = request['flowExecutionKey']
            urlAttrs.params = params
        }
        if (urlAttrs.event) {
            params."_eventId" = urlAttrs.remove('event')
            urlAttrs.params = params
        }
        def generatedLink = linkGenerator.link(attrs, request.characterEncoding)
        def writer = getOut()

        if (useJsessionId) {
            writer << response.encodeURL(generatedLink)
        }
        else {
            writer << generatedLink
        }
    }

    /**
     * Helper method for creating tags called like:<br/>
     * <pre>
     *    withTag(name:'script',attrs:[type:'text/javascript']) {
     *    ...
     *    }
     * </pre>
     * @attr name REQUIRED the tag name
     * @attr attrs tag attributes
     */
    def withTag = { attrs, body ->
        def writer = out
        writer << "<${attrs.name}"
        attrs.attrs.each { k,v ->
            if (!v) return
            if (v instanceof Closure) {
                writer << " $k=\""
                v()
                writer << '"'
            }
            else {
                writer << " $k=\"$v\""
            }
        }
        writer << '>'
        writer << body()
        writer << "</${attrs.name}>"
    }

    /**
     * Uses the Groovy JDK join method to concatenate the toString() representation of each item
     * in this collection with the given separator.
     *
     * @emptyTag
     * 
     * @attr REQUIRED in The collection to iterate over
     * @attr delimiter The value of the delimiter to use during the join. If no delimiter is specified then ", " (a comma followed by a space) will be used as the delimiter.
     */
    def join = { attrs ->
        def collection = attrs.'in'
        if (collection == null) {
            throwTagError('Tag ["join"] missing required attribute ["in"]')
        }

        def delimiter = attrs.delimiter == null ? ', ' : attrs.delimiter
        out << collection.join(delimiter)
    }

    /**
     * Output application metadata that is loaded from application.properties.
     *
     * @emptyTag
     * 
     * @attr name REQUIRED the metadata key
     */
    def meta = { attrs ->
        if (!attrs.name) {
            throwTagError('Tag ["meta"] missing required attribute ["name"]')
        }
        out << Metadata.current[attrs.name]
    }
}
