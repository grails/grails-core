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

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException;

/**
 * @author Graeme Rocher
 * @since 18-Jan-2006
 */
public class GroovyElseIfTag extends GroovySyntaxTag {
    public static final String TAG_NAME = "elseif";
    private static final String ATTRIBUTE_TEST = "test";
    private static final String ATTRIBUTE_ENV = "env";

    public void doStartTag() {
        String env = (String) attributes.get(ATTRIBUTE_ENV);
        String test = (String) attributes.get(ATTRIBUTE_TEST);
        env = StringUtils.isBlank(env) ? null : env;
        test = StringUtils.isBlank(test) ? null : test; 
        if((env == null) && (test == null))
            throw new GrailsTagException(
                "Tag ["+TAG_NAME+"] must have one or both of the attributes ["+ATTRIBUTE_TEST+"] or ["+ATTRIBUTE_ENV+"]");
        if (env != null) {
            env = calculateExpression(env);
        }
        if ((env != null) && (test != null)) {
            out.print("else if((GrailsUtil.environment == '"+env+"') && (");
            out.print(test);
            out.println(")) {");
        } else if (env != null) {
            // double (( is deliberate... to avoid thorny logic
            out.print("else if(GrailsUtil.environment == '"+env+"') {");
        } else {
            out.print("else if(");
            out.print(test);
            out.println(") {");
        }
    }

    public void doEndTag() {
        out.println("}");
    }

    public String getName() {
        return TAG_NAME;
    }

    public boolean isBufferWhiteSpace() {
        return true;
    }

    public boolean hasPrecedingContent() {
        return false;
    }
}
