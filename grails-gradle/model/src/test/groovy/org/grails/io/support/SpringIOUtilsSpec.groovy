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
package org.grails.io.support

import java.nio.file.Files
import java.nio.file.Path

import grails.util.Metadata

import org.xml.sax.SAXParseException

import spock.lang.Specification
import spock.lang.TempDir

/**
 * Asserts the parser hardening applied by {@link SpringIOUtils} through observable parsing
 * behaviour rather than by reading feature flags back off the factory.
 *
 * <p>This is deliberate. Reading the flags back would require this spec to hold its own copy of
 * the feature identifiers, so a search-and-replace over those identifiers would rewrite the
 * production code and this spec together and the suite would still pass. Driving real documents
 * through the parser keeps the assertions independent of how the hardening is spelled.
 */
class SpringIOUtilsSpec extends Specification {

    /** Shape of a JSP 1.2 tag library descriptor, as shipped inside jakarta jstl. */
    private static final String TLD = '''<!DOCTYPE taglib
  PUBLIC "-//Sun Microsystems, Inc.//DTD JSP Tag Library 1.2//EN"
  "http://java.sun.com/dtd/web-jsptaglibrary_1_2.dtd">
<taglib>
  <uri>jakarta.tags.core</uri>
  <tag><name>out</name><tag-class>org.example.OutTag</tag-class></tag>
</taglib>'''

    @TempDir
    Path tempDir

    void cleanup() {
        System.clearProperty(SpringIOUtils.ALLOW_DOCTYPE_DECLARATION)
        Metadata.reset()
    }

    private static void allowDocTypeDeclarations() {
        System.setProperty(SpringIOUtils.ALLOW_DOCTYPE_DECLARATION, 'true')
        Metadata.reset()
    }

    void 'createXmlSlurper parses a document without a doctype'() {
        when:
        def xml = SpringIOUtils.createXmlSlurper().parseText('<root><child>ok</child></root>')

        then:
        xml.child.text() == 'ok'
    }

    void 'createXmlSlurper rejects a doctype declaration by default'() {
        when:
        SpringIOUtils.createXmlSlurper().parseText(TLD)

        then:
        thrown(SAXParseException)
    }

    void 'createXmlSlurper rejects an internal doctype subset by default'() {
        when:
        SpringIOUtils.createXmlSlurper().parseText('''<!DOCTYPE root [
<!ENTITY msg "safe">
]>
<root>&msg;</root>''')

        then:
        thrown(SAXParseException)
    }

    void 'the doctype configuration key lets an application parse descriptors that declare one'() {
        given: 'an application.yml opting in, as an application would configure it'
        Metadata.getInstance(new ByteArrayInputStream('''grails:
    xml:
        allowDocTypeDeclaration: true
'''.getBytes('UTF-8')))

        when:
        def parsed = SpringIOUtils.createXmlSlurper().parseText(TLD)

        then: 'the descriptor is readable'
        parsed.uri.text() == 'jakarta.tags.core'
        parsed.tag.name.text() == 'out'
    }

    void 'external entities stay blocked when doctype declarations are permitted'() {
        given: 'a document whose entity points at a readable file on disk'
        allowDocTypeDeclarations()
        Path secret = tempDir.resolve('secret.txt')
        Files.writeString(secret, 'top-secret-token')
        String xml = """<!DOCTYPE root [
<!ENTITY ext SYSTEM '${secret.toUri().toASCIIString()}'>
]>
<root>&ext;</root>"""

        when:
        def parsed = SpringIOUtils.createXmlSlurper().parseText(xml)

        then: 'relaxing the doctype rule does not reopen the XXE vector'
        !parsed.text().contains('top-secret-token')
    }

    void 'external dtds are skipped rather than retrieved when doctype declarations are permitted'() {
        given:
        allowDocTypeDeclarations()
        String xml = """<!DOCTYPE root SYSTEM '${tempDir.resolve('missing.dtd').toUri().toASCIIString()}'>
<root>ok</root>"""

        expect:
        SpringIOUtils.createXmlSlurper().parseText(xml).text() == 'ok'
    }

    void 'newSAXParser applies the same hardening as createXmlSlurper'() {
        given:
        allowDocTypeDeclarations()
        Path secret = tempDir.resolve('secret.txt')
        Files.writeString(secret, 'top-secret-token')
        String xml = """<!DOCTYPE root [
<!ENTITY ext SYSTEM '${secret.toUri().toASCIIString()}'>
]>
<root>&ext;</root>"""
        StringBuilder text = new StringBuilder()

        when:
        SpringIOUtils.newSAXParser().parse(new ByteArrayInputStream(xml.getBytes('UTF-8')),
                new org.xml.sax.helpers.DefaultHandler() {
                    @Override
                    void characters(char[] chars, int start, int length) {
                        text.append(chars, start, length)
                    }
                })

        then:
        !text.toString().contains('top-secret-token')
    }
}
