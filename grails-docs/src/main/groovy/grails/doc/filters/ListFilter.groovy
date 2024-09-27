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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.doc.filters

import org.radeox.filter.context.FilterContext
import org.radeox.regex.MatchResult

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class ListFilter extends org.radeox.filter.ListFilter {

    void handleMatch(StringBuffer buffer, MatchResult result, FilterContext context) {
        super.handleMatch(buffer, result, context)
        buffer << "\n\n"
    }
}
