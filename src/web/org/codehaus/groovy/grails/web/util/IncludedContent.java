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
    private String content = "";
    private String redirectURL;

    public IncludedContent(String contentType, String content) {
        if(contentType!=null)
            this.contentType = contentType;
        this.content = content;
    }

    public IncludedContent(String redirectURL) {
        this.redirectURL = redirectURL;
    }

    /**
     * Returns the URL of a redirect if a redirect was issue in the Include otherwise it returns null if there was no redirect
     * @return The redirect URL
     */
    public String getRedirectURL() {
        return redirectURL;
    }

    /**
     * Returns the included content type (default is text/html;charset=UTF=8)
     * @return The content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns the included content
     * @return The content
     */
    public String getContent() {
        return content;
    }
}
