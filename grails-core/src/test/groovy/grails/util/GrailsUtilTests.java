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
package grails.util;

import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the GrailsUtils class.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
public class GrailsUtilTests {

    public void testGrailsVersion() {
        // assertEquals("4.1.0.M1", GrailsUtil.getGrailsVersion());
        assertEquals("4.1.0.BUILD-SNAPSHOT", GrailsUtil.getGrailsVersion());
    }

    @AfterEach
    protected void tearDown() throws Exception {
        System.setProperty(Environment.KEY, "");
    }
}
