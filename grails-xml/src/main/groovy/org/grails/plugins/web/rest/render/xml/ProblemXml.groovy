/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.grails.plugins.web.rest.render.xml

import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

import tools.jackson.databind.ObjectMapper

import org.springframework.http.ProblemDetail

/**
 * Writes a problem in the XML format of RFC 9457, as Spring's Jackson XML converter writes it, for an application without
 * {@code jackson-dataformat-xml}: a {@code problem} element in the {@code urn:ietf:rfc:7807} namespace, with the members
 * of the problem, then its extension members, which a mapper converts as it writes them. A list is written as an element
 * for each of its values, and a {@code null} as an {@code xsi:nil} element.
 *
 * @since 9.0
 */
@PackageScope
@CompileStatic
final class ProblemXml {

    static final String NAMESPACE = 'urn:ietf:rfc:7807'

    private static final String XSI = 'http://www.w3.org/2001/XMLSchema-instance'

    private static final XMLOutputFactory OUTPUT = XMLOutputFactory.newFactory()

    private ProblemXml() {
    }

    /**
     * @param problem the problem
     * @param writer the response writer
     * @param mapper converts the values of the extension members as it writes them
     */
    static void write(ProblemDetail problem, Writer writer, ObjectMapper mapper) {
        XMLStreamWriter xml = OUTPUT.createXMLStreamWriter(writer)
        xml.writeStartElement('problem')
        xml.writeDefaultNamespace(NAMESPACE)
        // In the order Jackson writes the members by default, leaving out those the problem does not have
        member(xml, 'detail', problem.detail)
        member(xml, 'instance', problem.instance)
        member(xml, 'status', problem.status)
        member(xml, 'title', problem.title)
        member(xml, 'type', problem.type)
        if (problem.properties) {
            mapper.convertValue(problem.properties, Map).each { Object name, Object value ->
                element(xml, name.toString(), value)
            }
        }
        xml.writeEndElement()
        xml.flush()
    }

    private static void member(XMLStreamWriter xml, String name, Object value) {
        if (value != null) {
            element(xml, name, value)
        }
    }

    private static void element(XMLStreamWriter xml, String name, Object value) {
        if (value instanceof List) {
            for (Object listed : (List) value) {
                element(xml, name, listed)
            }
        }
        else if (value == null) {
            xml.writeEmptyElement(name)
            xml.writeNamespace('xsi', XSI)
            xml.writeAttribute('xsi', XSI, 'nil', 'true')
        }
        else {
            xml.writeStartElement(name)
            if (value instanceof Map) {
                ((Map<?, ?>) value).each { Object key, Object member -> element(xml, key.toString(), member) }
            }
            else {
                xml.writeCharacters(value.toString())
            }
            xml.writeEndElement()
        }
    }
}
