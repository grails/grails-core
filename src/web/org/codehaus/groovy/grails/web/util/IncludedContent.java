/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.web.util;

import grails.util.GrailsWebUtil;

/**
 * A class that represents some content that has been used in an include request
 * 
 * @author Graeme Rocher
 * @since 1.1.1
 * 
 *        <p/>
 *        Created: Mar 26, 2009
 */
public class IncludedContent {

    private String contentType = GrailsWebUtil.getContentType("text/html","UTF-8");
    private String content;

    public IncludedContent(String contentType, String content) {
        if(contentType!=null)
            this.contentType = contentType;
        this.content = content;
    }

    public IncludedContent(String content) {
        this(null, content);
    }

    public String getContentType() {
        return contentType;
    }

    public String getContent() {
        return content;
    }
}
