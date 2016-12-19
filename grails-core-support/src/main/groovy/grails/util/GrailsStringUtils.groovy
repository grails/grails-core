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

package grails.util

import groovy.transform.CompileStatic
import org.springframework.util.StringUtils

import java.util.regex.Pattern

/**
 * Extra methods for string manipulation
 *
 * @author Graeme Rocher
 * @since 2.3.6
 */
@CompileStatic
abstract class GrailsStringUtils extends StringUtils{

    private static final Pattern BOOLEAN_PATTERN = Pattern.compile(/^on$|^true$|^yes$|^1$/, Pattern.CASE_INSENSITIVE)

    /**
     * Converts a string to a boolean.
     *
     * The values 'true', 'on', 'yes' and '1' result in true being returned, otherwise false is returned
     *
     * @param str The string
     *
     * @return A boolean value of true or false
     */
    static boolean toBoolean(String str) {
        str != null && str ==~ BOOLEAN_PATTERN
    }

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
     * Returns a substring before the last occurance of the given token
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
    static String substringBeforeLast(String str, String token)  {
        if(token == null) return str
        def i = str.lastIndexOf(token)

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
     * Returns a substring after the last occurrence of the given token
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
    static String substringAfterLast(String str, String token)  {
        if(token == null) return str
        def i = str.lastIndexOf(token)

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
     * Same as {@link StringUtils#isEmpty(java.lang.Object)} but trims the string for surrounding whitespace
     */
    static boolean isBlank(String str) {
        isEmpty(str?.trim())
    }

    /**
     * Opposite of {@link GrailsStringUtils#isBlank(java.lang.String)}
     */
    static boolean isNotBlank(String str) {
        !isBlank(str?.trim())
    }

    /**
     * Opposite of {@link GrailsStringUtils#isEmpty(java.lang.Object)}
     */
    static boolean isNotEmpty(String str) {
        !isEmpty(str)
    }

    /**
     * Obtains the base name of a file excluding path and extension
     * @param path The path
     * @return The name of the file excluding path and extension
     */
    static String getFileBasename(String path) {
        stripFilenameExtension( getFilename(path) )
    }

}
