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
package org.codehaus.groovy.grails.web.sitemesh;

import com.opensymphony.module.sitemesh.PageParserSelector;
import com.opensymphony.module.sitemesh.filter.PageResponseWrapper;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.WebUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Graeme Rocher
 * @since 1.0.4
 *        <p/>
 *        Created: Nov 14, 2008
 */
public class GrailsPageResponseWrapper extends PageResponseWrapper{
    public GrailsPageResponseWrapper(HttpServletResponse response, PageParserSelector parserSelector) {
        super(response, parserSelector);
    }

    public void sendError(int sc) throws IOException {
        GrailsWebRequest webRequest = WebUtils.retrieveGrailsWebRequest();
        try {
            super.sendError(sc);
        } finally {
            WebUtils.storeGrailsWebRequest(webRequest);
        }
    }

    public void sendError(int sc, String msg) throws IOException {
        GrailsWebRequest webRequest = WebUtils.retrieveGrailsWebRequest();
        try {
            super.sendError(sc, msg);
        } finally {
            WebUtils.storeGrailsWebRequest(webRequest);
        }

    }
}
