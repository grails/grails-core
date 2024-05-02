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
package grails.util

import grails.util.GrailsStringUtils
import spock.lang.Specification

/**
 */
class GrailsStringUtilsSpec extends Specification{

    void "Test toBoolean"() {
        expect:
            GrailsStringUtils.toBoolean("on") == true
            GrailsStringUtils.toBoolean("yes") == true
            GrailsStringUtils.toBoolean("true") == true
            GrailsStringUtils.toBoolean("1") == true
            GrailsStringUtils.toBoolean("ON") == true
            GrailsStringUtils.toBoolean("Yes") == true
            GrailsStringUtils.toBoolean("TRue") == true
            GrailsStringUtils.toBoolean("false") == false
            GrailsStringUtils.toBoolean("0") == false
            GrailsStringUtils.toBoolean("off") == false
    }
    void "Test substringBefore method"() {
        expect:
            GrailsStringUtils.substringBefore("abc", "a")   == ""
            GrailsStringUtils.substringBefore("abcba", "b") == "a"
            GrailsStringUtils.substringBefore("abc", "c")   == "ab"
            GrailsStringUtils.substringBefore("abc", "d")   == "abc"
            GrailsStringUtils.substringBefore("abc", "")    == ""
            GrailsStringUtils.substringBefore("abc", null)  == "abc"
    }

    void "Test substringAfter method"() {
        expect:
            GrailsStringUtils.substringAfter("abc", "a")   == "bc"
            GrailsStringUtils.substringAfter("abcba", "b") == "cba"
            GrailsStringUtils.substringAfter("abc", "c")   == ""
            GrailsStringUtils.substringAfter("abc", "d")   == "abc"
            GrailsStringUtils.substringAfter("abc", "")    == "abc"
    }

    void "Test trimStart method"() {
        expect:
            GrailsStringUtils.trimStart("abc", "") == 'abc'
            GrailsStringUtils.trimStart("abc", null) == 'abc'
            GrailsStringUtils.trimStart("abc", "a") == 'bc'
            GrailsStringUtils.trimStart("abc", "ab") == 'c'
            GrailsStringUtils.trimStart("abc", "c") == 'abc'
    }
}
