/*
 * Copyright 2004-2008 the original author or authors.
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
package org.codehaus.groovy.grails.web.converters.marshaller.json;

import grails.converters.JSON;
import org.apache.commons.lang.time.FastDateFormat;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller;
import org.codehaus.groovy.grails.web.json.JSONException;

import java.text.Format;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * JSON ObjectMarshaller which converts a Date Object, conforming to the ECMA-Script-Specification Draft, to a String value
 *
 * @author Siegfried Puchbauer
 * @since 1.1
 */
public class DateMarshaller implements ObjectMarshaller<JSON> {

    // TODO Tests resulted in java.text.SimpleDateFormat beeing a bit faster - but it's not thread-safe - need to discuss
    private final Format JSON_DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("GMT"), Locale.US);

    public boolean supports(Object object) {
        return object instanceof Date;
    }

    public void marshalObject(Object object, JSON converter) throws ConverterException {
        try {
            converter.getWriter().value(JSON_DATE_FORMAT.format((Date) object));
        } catch (JSONException e) {
            throw new ConverterException(e);
        }
    }
}
