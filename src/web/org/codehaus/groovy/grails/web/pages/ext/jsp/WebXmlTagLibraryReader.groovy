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

/**
 * A SAX handler that reads the tag library definitions from a web.xml file
 *
 * @author Graeme Rocher
 *
 */
class WebXmlTagLibraryReader extends DefaultHandler {

   static final TAG_TAGLIB_URI = "taglib-uri"
   static final TAG_TAGLIB_LOC = "taglib-location"
   static final TAG_TAGLIB = "taglib"
   /**
    * Contains a map of URI to tag library locations once the handler has read the web.xml file
    */
   Map tagLocations = [:]

   private location
   private uri
   private buf

    void startElement(String ns, String localName, String qName, Attributes attributes) {
        if(TAG_TAGLIB_URI == qName ||
           TAG_TAGLIB_LOC == qName) buf = new StringBuffer()
    }

    public void characters(char[] chars, int offset, int length) {
        buf?.append(chars,offset, length)
    }

    public void endElement(String ns, String localName, String qName) {
       switch(qName) {
           case TAG_TAGLIB_URI:
                uri = buf.toString().trim()
                break

           case TAG_TAGLIB_LOC:
                location = buf.toString().trim()
                break
           case TAG_TAGLIB:
                tagLocations[uri] = location
                break
       }
    }
}