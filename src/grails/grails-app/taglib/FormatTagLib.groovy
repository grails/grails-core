
/* Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT c;pWARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 /**
 * The base application tag library for Grails many of which take inspiration from Rails helpers (thanks guys! :)
 * This tag library tends to get extended by others as tags within here can be re-used in said libraries
 *
 * @author Jason Rudolph
 * @since 0.6
 *
 * Created:17-Jan-2006
 */
class FormatTagLib {
    /**
     * Outputs the given <code>Date</code> object in the specified format.  If
     * the <code>date</code> is not given, then the current date/time is used.
     * If the <code>format</code> option is not given, then the date is output
     * using the default format.
     *
     * e.g., <g:formatDate date="${myDate}" format="yyyy-MM-dd HH:mm" />
     *
     * @see java.text.SimpleDateFormat
     */
    def formatDate = { attrs ->
        def date = attrs.get('date')

        if (!date) {
                date = new Date()
        }

        def format = attrs.get('format')
        if (!format) {
                format = "yyyy-MM-dd HH:mm:ss z"
        }

        out << new java.text.SimpleDateFormat(format).format(date)
    }

    /**
     * Outputs the given number in the specified format.  If no
     * <code>number</code> is given, then zero is used.  If the
     * <code>format</code> option is not given, then the number is output
     * using the default format.
     *
     * e.g., <g:formatNumber number="${myNumber}" format="###,##0" />
     *
     * @see java.text.DecimalFormat
     */
    def formatNumber = { attrs ->
        def number = attrs.get('number')

        if (!number) {
                number = new Double(0)
        }

        def format = attrs.get('format')
        if (!format) {
                format = "0"
        }

        out << new java.text.DecimalFormat(format).format((Double)number)
    }
}