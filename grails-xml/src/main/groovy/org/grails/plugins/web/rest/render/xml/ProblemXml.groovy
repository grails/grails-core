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

import tools.jackson.core.StreamWriteFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.util.RawValue

import org.springframework.http.ProblemDetail

/**
 * Writes a problem in the XML format of RFC 9457, as Spring's Jackson XML converter writes it, for an application without
 * {@code jackson-dataformat-xml}: a {@code problem} element in the {@code urn:ietf:rfc:7807} namespace, with the members
 * of the problem, those of a subclass included, and its extension members, as a mapper with Spring's mix-in for problems
 * converts them, the properties of a bean in no namespace and the entries of a map in that of the map, as Spring's
 * converter writes them. A list is written as an element for each of its values, wrapped in an element of its name where
 * it is the property of a bean, and a {@code null} as an {@code xsi:nil} element.
 *
 * @since 9.0
 */
@PackageScope
@CompileStatic
final class ProblemXml {

    static final String NAMESPACE = 'urn:ietf:rfc:7807'

    private static final String XSI = 'http://www.w3.org/2001/XMLSchema-instance'

    /** The members of RFC 9457. */
    private static final Set<String> MEMBERS = ['type', 'title', 'status', 'detail', 'instance'] as Set<String>

    // The JDK's own writer, whatever StAX implementation another library puts on the classpath
    private static final XMLOutputFactory OUTPUT = XMLOutputFactory.newDefaultFactory()

    private ProblemXml() {
    }

    /**
     * @param problem the problem
     * @param writer the response writer
     * @param mapper a mapper with Spring's {@code ProblemDetailJacksonMixin}, which converts the problem as Spring's
     *        converters write it
     */
    static void write(ProblemDetail problem, Writer writer, ObjectMapper mapper) {
        XMLStreamWriter xml = OUTPUT.createXMLStreamWriter(writer)
        xml.writeStartElement('problem')
        xml.writeDefaultNamespace(NAMESPACE)
        Set<String> extensions = problem.properties?.keySet() ?: Collections.<String> emptySet()
        boolean plainDecimals = mapper.isEnabled(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
        Map<?, ?> converted = ProblemBeans.marking(mapper).convertValue(problem, Map)
        converted.each { Object name, Object value ->
            String member = name.toString()
            if (member != ProblemBeans.MARKER) {
                // Spring's converter writes the members of RFC 9457 and the extension members in its namespace, and the
                // properties of a subclass, as those of any bean, in none
                boolean property = !(MEMBERS.contains(member) || extensions.contains(member))
                element(xml, writer, member, value, property ? '' : NAMESPACE, NAMESPACE, plainDecimals, property)
            }
        }
        xml.writeEndElement()
        xml.flush()
    }

    /**
     * @param namespace the namespace of the element
     * @param enclosing the namespace of the element it is in, which it declares its own where they differ
     */
    private static void element(XMLStreamWriter xml, Writer writer, String name, Object value, String namespace,
            String enclosing, boolean plainDecimals, boolean property) {
        if (value instanceof List) {
            List<?> listed = (List<?>) value
            // a bean written as an array is not a collection
            boolean array = !listed.isEmpty() && listed[0] == ProblemBeans.ARRAY_MARKER
            List<?> values = array ? listed.subList(1, listed.size()) : listed
            if (property && !array) {
                // a collection a bean property holds is wrapped in an element of its name, as Jackson's XML mapper wraps it
                xml.writeStartElement(name)
                declare(xml, namespace, enclosing)
                for (Object each : values) {
                    element(xml, writer, name, each, namespace, namespace, plainDecimals, false)
                }
                xml.writeEndElement()
            }
            else {
                for (Object each : values) {
                    element(xml, writer, name, each, namespace, enclosing, plainDecimals, false)
                }
            }
        }
        else if (value == null) {
            xml.writeEmptyElement(name)
            declare(xml, namespace, enclosing)
            xml.writeNamespace('xsi', XSI)
            xml.writeAttribute('xsi', XSI, 'nil', 'true')
        }
        else {
            xml.writeStartElement(name)
            declare(xml, namespace, enclosing)
            if (value instanceof Map) {
                Map<?, ?> members = (Map<?, ?>) value
                // the properties of a bean are in no namespace, and the entries of a map in that of the map
                boolean bean = members.containsKey(ProblemBeans.MARKER)
                String inner = bean ? '' : namespace
                members.each { Object key, Object member ->
                    if (key != ProblemBeans.MARKER) {
                        element(xml, writer, key.toString(), member, inner, namespace, plainDecimals, bean)
                    }
                }
            }
            else if (value instanceof RawValue) {
                // written as it is, as Jackson writes a raw value: the start tag is closed, then the text follows it
                xml.writeCharacters('')
                xml.flush()
                writer.write(String.valueOf(((RawValue) value).rawValue()))
            }
            else {
                xml.writeCharacters(text(value, plainDecimals))
            }
            xml.writeEndElement()
        }
    }

    /**
     * The text of a value, as Jackson writes it: binary data in Base64, a decimal as the mapper's features say, and an
     * infinite floating-point number as XML Schema writes it, as Jackson's XML mapper does by default.
     */
    private static String text(Object value, boolean plainDecimals) {
        if (value instanceof byte[]) {
            return Base64.encoder.encodeToString((byte[]) value)
        }
        if ((value instanceof Double || value instanceof Float) && ((Number) value).doubleValue().infinite) {
            return ((Number) value).doubleValue() > 0 ? 'INF' : '-INF'
        }
        if (value instanceof BigDecimal && plainDecimals) {
            return ((BigDecimal) value).toPlainString()
        }
        value.toString()
    }

    private static void declare(XMLStreamWriter xml, String namespace, String enclosing) {
        if (namespace != enclosing) {
            xml.writeDefaultNamespace(namespace)
        }
    }
}
