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
package org.codehaus.groovy.grails.web.converters.marshaller.xml;

import grails.converters.XML;
import org.codehaus.groovy.grails.web.converters.ConverterUtil;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller;
import org.apache.commons.lang.time.FastDateFormat;

import java.text.Format;
import java.util.Date;

/**
 * @author Siegfried Puchbauer
 * @since 1.1
 */
public class DateMarshaller implements ObjectMarshaller<XML> {

    private final Format XML_DATE_FORMAT;

    public DateMarshaller(Format XML_DATE_FORMAT) {
        this.XML_DATE_FORMAT = XML_DATE_FORMAT;
    }

    public DateMarshaller() {
        this(FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.S z"));
    }

    public boolean supports(Object object) {
        return object instanceof Date;
    }

    public void marshalObject(Object object, XML xml) throws ConverterException {
        try {
            xml.chars(XML_DATE_FORMAT.format(object));
        } catch (Exception e) {
            throw ConverterUtil.resolveConverterException(e);
        }
    }
}
