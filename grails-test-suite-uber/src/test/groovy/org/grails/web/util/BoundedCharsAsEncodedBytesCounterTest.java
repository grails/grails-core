/*
 * Copyright 2024 original authors
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
package org.grails.web.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BoundedCharsAsEncodedBytesCounterTest {

    private static final String TEST_STRING = "Hello \u00f6\u00e4\u00e5\u00d6\u00c4\u00c5!";

    @Test
    public void testCalculation() throws Exception {
        BoundedCharsAsEncodedBytesCounter counter = new BoundedCharsAsEncodedBytesCounter(1024, "ISO-8859-1");
        counter.getCountingWriter();
        counter.update(TEST_STRING);
        assertEquals(13, counter.size());
        assertEquals(13, TEST_STRING.getBytes("ISO-8859-1").length);
        counter.update(TEST_STRING);
        assertEquals(26, counter.size());
    }
}
