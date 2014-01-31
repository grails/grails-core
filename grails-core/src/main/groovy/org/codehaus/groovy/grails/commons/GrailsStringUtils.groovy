/*
 * Copyright 2012 the original author or authors.
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

package org.codehaus.groovy.grails.commons

import groovy.transform.CompileStatic
import org.springframework.util.StringUtils

/**
 * Extra methods for string manipulation
 *
 * @author Graeme Rocher
 * @since 2.3.6
 */
@CompileStatic
abstract class GrailsStringUtils extends StringUtils{

    /**
     * Returns a substring before the given token
     *
     * GrailsStringUtils.substringBefore(null, *)      = null
     * GrailsStringUtils.substringBefore("", *)        = ""
     * GrailsStringUtils.substringBefore("abc", "a")   = ""
     * GrailsStringUtils.substringBefore("abcba", "b") = "a"
     * GrailsStringUtils.substringBefore("abc", "c")   = "ab"
     * GrailsStringUtils.substringBefore("abc", "d")   = "abc"
     * GrailsStringUtils.substringBefore("abc", "")    = ""
     * GrailsStringUtils.substringBefore("abc", null)  = "abc"
     *
     * @param str The string to apply the substring
     * @param token The token to match
     */
    static String substringBefore(String str, String token)  {
        if(token == null) return str
        def i = str.indexOf(token)

        if(i > -1) {
            return str.substring(0, i)
        }
        return str
    }

    /**
     * Returns a substring after the given token
     *
     * GrailsStringUtils.substringAfter(null, *)      = null
     * GrailsStringUtils.substringAfter("", *)        = ""
     * GrailsStringUtils.substringAfter(*, null)      = ""
     * GrailsStringUtils.substringAfter("abc", "a")   = "bc"
     * GrailsStringUtils.substringAfter("abcba", "b") = "cba"
     * GrailsStringUtils.substringAfter("abc", "c")   = ""
     * GrailsStringUtils.substringAfter("abc", "d")   = ""
     * GrailsStringUtils.substringAfter("abc", "")    = "abc"
     *
     * @param str The string to apply the substring
     * @param token The token to match
     */
    static String substringAfter(String str, String token)  {
        if(token == null) return str
        def i = str.indexOf(token)

        if(i > -1) {
            return str.substring(i + token.length())
        }
        return str
    }
    /**
     * Trims the start of the string
     * @param str The string to trim
     * @param start The start to trim
     *
     * @return The trimmed string
     */
    static String trimStart(String str, String start) {
        if(!str || !start || !str.startsWith(start)) {
            return str
        }
        else {
            return str.substring(start.length())
        }
    }

    /**
     * Same as {@link StringUtils#isEmpty(java.lang.Object)} }
     */
    static boolean isBlank(String str) {
        isEmpty(str)
    }
}
