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
package org.codehaus.groovy.grails.scaffolding;

import groovy.text.Template;
import groovy.lang.Writable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine;
import org.codehaus.groovy.grails.web.servlet.view.GroovyPageView;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * A special Spring View for scaffolding that renders an in-memory scaffolded view to the response.
 *
 * @author Graeme Rocher
 * @since 0.5
 *
 *        <p/>
 *        Created: Feb 27, 2007
 *        Time: 11:30:10 AM
 */
public class ScaffoldedGroovyPageView extends GroovyPageView {
    private String contents;
    private static final Log LOG = LogFactory.getLog(ScaffoldedGroovyPageView.class);

    public ScaffoldedGroovyPageView(String uri, String contents) {
        if(StringUtils.isBlank(contents)) throw new IllegalArgumentException("Argument [contents] cannot be blank or null");
        if(StringUtils.isBlank(uri)) throw new IllegalArgumentException("Argument [uri] cannot be blank or null");

        this.contents = contents;
        setUrl(uri);
    }


    /**
     * Used for debug reporting
     *
     * @return The URL of the view
     */
    public String getBeanName() {
        return getUrl();
    }


    /**
     * Overrides the default implementation to render a GSP view using an in-memory representation held in the #contents property
     *
     * @param templateEngine The GroovyPagesTemplateEngine instance
     * @param model The model
     * @param response The HttpServletResponse instance
     * 
     * @throws IOException Thrown if there was an IO error rendering the view
     */
    protected void renderWithTemplateEngine(GroovyPagesTemplateEngine templateEngine, Map model,
                                            HttpServletResponse response, HttpServletRequest request) throws IOException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Rendering scaffolded view ["+getUrl()+"] with model ["+model+"]");
        }
        Template t = templateEngine.createTemplate(contents, getUrl());
        Writable w = t.make(model);

        Writer out = null;
        try {
            out = createResponseWriter(response);
            w.writeTo(out);
        } catch(Exception e) {
            // create fresh response writer
            out = createResponseWriter(response);
            handleException(e, out, templateEngine, request);
        } finally {
            if(out!=null)out.close();
        }

    }
}
