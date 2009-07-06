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

package org.codehaus.groovy.grails.web.pages.ext.jsp

import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.Attributes
import org.springframework.util.ClassUtils

/**
 * A SAX parser implementation that reads the contents of a tag library definition (TLD) into two properties
 * called tags and listeners (for the tag listeners)
 */
class TldReader extends DefaultHandler{

    final Map tags = [:]
    final List listeners = []

    StringBuffer buf = null

    private tagName
    private className

    void startElement(String nsuri, String localName, String qName, Attributes attributes) {
        if("name" == qName || "tagclass" == qName || "tag-class" == qName || "listener-class" == qName) {
            buf = new StringBuffer()
        }
    }

    void characters(char[] chars, int offset, int length) {
        buf?.append chars, offset, length
    }

    void endElement(String nsuri, String localName, String qName) {
        switch(qName) {

            case "name":
                if(!tagName)
                    tagName = buf.toString().trim(); buf = null
            break
            case "tag":
                Class tagClass = ClassUtils.forName(className)
                tags[tagName] = tagClass
                tagName = null
                className = null
            break
            case "listener-class":
                Class listenerClass = ClassUtils.forName(buf.toString().trim())
                listeners << listenerClass.newInstance()
            break
            case ~/tag-{0,1}class/:
                className = buf.toString().trim(); buf = null
            break
        }
    }

            
}