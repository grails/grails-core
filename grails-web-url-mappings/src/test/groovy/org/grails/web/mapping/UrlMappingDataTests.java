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
package org.grails.web.mapping;

import grails.web.mapping.UrlMappingData;
import grails.web.mapping.UrlMappingParser;
import junit.framework.TestCase;
import org.grails.web.mapping.DefaultUrlMappingData;
import org.grails.web.mapping.DefaultUrlMappingParser;

/**
 * Tests for the UrlMappingData class
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Mar 5, 2007
 *        Time: 7:50:41 AM
 */
public class UrlMappingDataTests extends TestCase {

    public void testOptionals() {
        UrlMappingData data = new DefaultUrlMappingData("/surveys/(*)?");

        assertEquals(2, data.getTokens().length);
        assertTrue(data.isOptional(0));
    }

    public void testTokens() {
        UrlMappingData data = new DefaultUrlMappingData("/blog/(*)/2007/(*)?");

        String[] tokens = data.getTokens();

        assertFalse(tokens.length == 0);

        assertEquals("blog",tokens[0]);
        assertEquals("(*)",tokens[1]);
        assertEquals("2007",tokens[2]);
        assertEquals("(*)?",tokens[3]);

        assertFalse(data.isOptional(0));
        assertTrue(data.isOptional(1));
    }

    public void testLogicalUrls() {
        UrlMappingData data = new DefaultUrlMappingData("/blog/(*)?/2007/*/*?");

        String[] urls = data.getLogicalUrls();

        assertEquals(3, urls.length);

        assertEquals("/blog/(*)?/2007/*/*?", urls[0]);
        assertEquals("/blog/(*)?/2007/*", urls[1]);
        assertEquals("/blog", urls[2]);
    }

    public void testUrlMappingDataParser() {
        UrlMappingParser parser = new DefaultUrlMappingParser();

        UrlMappingData data = parser.parse("/blog/(*)?/2007/*/*?");

        String[] urls = data.getLogicalUrls();

        assertEquals(3, urls.length);

        assertEquals("/blog/(*)?/2007/*/*?", urls[0]);
        assertEquals("/blog/(*)?/2007/*", urls[1]);
        assertEquals("/blog", urls[2]);

    }
}
