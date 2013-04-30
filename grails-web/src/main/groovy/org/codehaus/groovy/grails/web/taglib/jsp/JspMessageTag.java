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
package org.codehaus.groovy.grails.web.taglib.jsp;

/**
 * JSP facade onto the GSP message tag
 *
 * @author Graeme Rocher
 * @since 28-Feb-2006
 */
public class JspMessageTag extends JspInvokeGrailsTagLibTag {
    private static final long serialVersionUID = -3098229619044871773L;

    private static final String TAG_NAME = "message";

    private String code;
    private String error;

    public JspMessageTag() {
        setTagName(TAG_NAME);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
