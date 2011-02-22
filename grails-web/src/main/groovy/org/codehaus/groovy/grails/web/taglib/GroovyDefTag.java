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
package org.codehaus.groovy.grails.web.taglib;

import grails.util.GrailsUtil;

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException;

/**
 * Allows defining of variables within the page context.
 *
 * @author Graeme Rocher
 */
public class GroovyDefTag extends GroovySyntaxTag {

    public static final String TAG_NAME = "def";
    private static final String ATTRIBUTE_VALUE = "value";

    public void doStartTag() {
        String expr = attributes.get(ATTRIBUTE_VALUE);
        if (StringUtils.isBlank(expr)) {
            throw new GrailsTagException("Tag [" + TAG_NAME + "] missing required attribute [" + ATTRIBUTE_VALUE + "]");
        }

        String var = attributes.get(ATTRIBUTE_VAR);
        if (StringUtils.isBlank(var)) {
            throw new GrailsTagException("Tag [" + TAG_NAME + "] missing required attribute [" + ATTRIBUTE_VAR + "]");
        }

        GrailsUtil.deprecated("The tag <g:def> is deprecated and will be removed in a future release. Use <g:set> instead.");
        out.print("def ");
        out.print(var.substring(1,var.length() -1));
        out.print('=');
        out.println(expr);
    }

    public void doEndTag() {
        // do nothing
    }

    public String getName() {
        return TAG_NAME;
    }

    @Override
    public boolean isKeepPrecedingWhiteSpace() {
        return true;
    }

    @Override
    public boolean isAllowPrecedingContent() {
        return true;
    }
}
