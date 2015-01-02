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

import com.opensymphony.module.sitemesh.RequestConstants
import grails.artefact.TagLibrary
import grails.gsp.TagLib
import groovy.transform.CompileStatic
import org.grails.buffer.FastStringWriter
import org.grails.buffer.GrailsPrintWriter
import org.grails.buffer.StreamCharBuffer
import org.grails.encoder.CodecLookup
import org.grails.encoder.Encoder
import org.grails.gsp.compiler.SitemeshPreprocessor
import org.grails.web.sitemesh.GSPSitemeshPage

/**
 * Internal Sitemesh pre-processor tags.
 *
 * @author Graeme Rocher
 * @since 1.2
 */
@CompileStatic
@TagLib
class SitemeshTagLib implements RequestConstants, TagLibrary {
    protected static final String GSP_SITEMESH_PAGE = 'org.grails.web.sitemesh.GrailsLayoutView.GSP_SITEMESH_PAGE'

    static String namespace = 'sitemesh'
    CodecLookup codecLookup

    def captureTagContent(GrailsPrintWriter writer, String tagname, Map attrs, Object body, boolean noEndTagForEmpty=false) {
        def content = null
        if (body != null) {
            if (body instanceof Closure) {
                content = body()
            }
            else {
                content = body
            }
        }

        if (content instanceof StreamCharBuffer) {
            content.setPreferSubChunkWhenWritingToOtherBuffer(true)
        }
        writer << '<'
        writer << tagname
        def useXmlClosingForEmptyTag = false
        if (attrs) {
            def xmlClosingString = attrs.remove(SitemeshPreprocessor.XML_CLOSING_FOR_EMPTY_TAG_ATTRIBUTE_NAME)
            if (xmlClosingString=='/') {
                useXmlClosingForEmptyTag = true
            }
            Encoder htmlEncoder = codecLookup?.lookupEncoder('HTML')
            attrs.each { k, v ->
                writer << ' '
                writer << k
                writer << '="'
                writer << (htmlEncoder != null ? htmlEncoder.encode(v) : v) 
                writer << '"'
            }
        }

        if (content) {
            writer << '>'
            // the following row must be written separately (append StreamCharBuffer gets appended as subchunk)
            writer << content
            writer << '</'
            writer << tagname
            writer << '>'
        }
        else {
            if (!useXmlClosingForEmptyTag) {
                writer << '>'
                // in valid HTML , closing of an empty tag depends on the element name
                // for empty title, the tag must be closed properly
                // for empty meta tag shouldn't be closed at all, see GRAILS-5696
                if (!noEndTagForEmpty) {
                    writer << '</'
                    writer << tagname
                    writer << '>'
                }
            }
            else {
                // XML / XHTML empty tag
                writer << '/>'
            }
        }
        content
    }

    def StreamCharBuffer wrapContentInBuffer(Object content) {
        if (content instanceof Closure) {
            content = content()
        }
        if (!(content instanceof StreamCharBuffer)) {
            // the body closure might be a string constant, so wrap it in a StreamCharBuffer in that case
            FastStringWriter stringWriter=new FastStringWriter()
            stringWriter.print((Object)content)
            StreamCharBuffer newbuffer = stringWriter.buffer
            newbuffer.setPreferSubChunkWhenWritingToOtherBuffer(true)
            return newbuffer
        } else {
            return (StreamCharBuffer)content
        }
    }

    /**
     * Captures the &lt;head&gt; tag.
     */
    Closure captureHead = { Map attrs, body ->
        def content = captureTagContent(out, 'head', attrs, body)

        if (content != null) {
            GSPSitemeshPage smpage = findGSPSitemeshPage(request)
            if (smpage) {
                smpage.setHeadBuffer(wrapContentInBuffer(content))
            }
        }
    }

    protected GSPSitemeshPage findGSPSitemeshPage(javax.servlet.http.HttpServletRequest request) {
        (GSPSitemeshPage)request.getAttribute(GSP_SITEMESH_PAGE)
    }

    /**
     * Allows passing of parameters to Sitemesh layout.<br/>
     *
     * &lt;sitemesh:parameter name="foo" value="bar" /&gt;
     */
    Closure parameter = { Map attrs, body ->
        GSPSitemeshPage smpage=findGSPSitemeshPage(request)
        def name = attrs.name?.toString()
        def val = attrs.value?.toString()
        if (smpage && name && val != null) {
            smpage.addProperty("page.$name", val)
        }
    }

    /**
     * Captures the &lt;body&gt; tag.
     */
    Closure captureBody = { Map attrs, body ->
        def content = captureTagContent(out, 'body', attrs, body)
        if (content != null) {
            GSPSitemeshPage smpage = findGSPSitemeshPage(request)
            if (smpage) {
                smpage.setBodyBuffer(wrapContentInBuffer(content))
                if (attrs) {
                    attrs.each { k, v ->
                        smpage.addProperty("body.${k?.toString()?.toLowerCase()}", v?.toString())
                    }
                }
            }
        }
    }

    /**
     * Captures the individual &lt;content&gt; tags.
     */
    Closure captureContent = { Map attrs, body ->
        if (body != null) {
            GSPSitemeshPage smpage=findGSPSitemeshPage(request)
            if (smpage && attrs.tag) {
                smpage.setContentBuffer(attrs.tag as String, wrapContentInBuffer(body))
            }
        }
    }

    /**
     * Captures the individual &lt;meta&gt; tags.
     */
    Closure captureMeta = { Map attrs, body ->
        def content = captureTagContent(out, 'meta', attrs, body, true)
        GSPSitemeshPage smpage = findGSPSitemeshPage(request)
        def val = attrs.content?.toString()
        if (attrs && smpage && val != null) {
            if (attrs.name) {
                smpage.addProperty("meta.${attrs.name}", val)
                smpage.addProperty("meta.${attrs.name.toString().toLowerCase()}", val)
            }
            else if (attrs['http-equiv']) {
                String httpEquiv = attrs['http-equiv'] as String
                def httpEquivFormats = [httpEquiv, httpEquiv.toLowerCase()]
                if(httpEquiv.equalsIgnoreCase('content-type')) {
                    httpEquivFormats << 'Content-Type'
                }
                for (def httpEquivFormat : httpEquivFormats) {
                    smpage.addProperty("meta.http-equiv.${httpEquivFormat}", val)
                }
            }
        }
    }

    /**
     * Captures the &lt;title&gt; tag.
     */
    Closure captureTitle = { Map attrs, body ->
        GSPSitemeshPage smpage = findGSPSitemeshPage(request)
        def content = captureTagContent(out, 'title', attrs, body)
        if (smpage && content != null) {
            smpage.addProperty('title', content?.toString())
            smpage.setTitleCaptured(true)
        }
    }

    /**
     * Wraps the title tag so that the buffer can be cleared out from the head buffer
     */
    Closure wrapTitleTag = { Map attrs, body ->
        if (body != null) {
            GSPSitemeshPage smpage=findGSPSitemeshPage(request)
            if (smpage) {
                def wrapped = wrapContentInBuffer(body)
                smpage.setTitleBuffer(wrapped)
                out << wrapped
            } else if (body instanceof Closure) {
                out << body()
            }
        }
    }
}
