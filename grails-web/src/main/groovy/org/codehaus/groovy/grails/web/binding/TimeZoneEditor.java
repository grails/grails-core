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
package org.codehaus.groovy.grails.web.binding;

import java.beans.PropertyEditorSupport;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;

/**
 * A Property editor for converting instances of java.util.TimeZone.
 *
 * @author Graeme Rocher
 */
public class TimeZoneEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (StringUtils.isBlank(text)) {
            setValue(null);
        }
        try {
            setValue(TimeZone.getTimeZone(text));
        }
        catch (IllegalArgumentException e) {
            // ignore and just set to null
            setValue(null);
        }
    }

    @Override
    public String getAsText() {
        TimeZone tz = (TimeZone)getValue();
        if (tz == null) {
            return "";
        }

        return tz.getID();
    }
}
