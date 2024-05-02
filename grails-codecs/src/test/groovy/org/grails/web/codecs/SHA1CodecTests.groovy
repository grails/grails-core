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
package org.grails.web.codecs

import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.*

class SHA1CodecTests {

    @Test
    void testEncode() {

        def expectedResult = '2ef7bde608ce5404e97d5f042f95f89f1c232871'

        // we want to verify that both array/list and String inputs work
        def primitiveResult = [72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 33].encodeAsSHA1()
        def toStringResult = 'Hello World!'.encodeAsSHA1()

        assertEquals(expectedResult, primitiveResult)
        assertEquals(expectedResult,toStringResult)

        //make sure encoding null returns null
        assertNull null.encodeAsSHA1()
    }

    @Test
    void testDecode() {
        assertThrows(UnsupportedOperationException, {
            [1,2,3,4,5].decodeSHA1()
        })
    }
}
