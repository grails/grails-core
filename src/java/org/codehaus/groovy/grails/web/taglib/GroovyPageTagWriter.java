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
package org.codehaus.groovy.grails.web.taglib;

import org.codehaus.groovy.grails.web.pages.FastStringWriter;

/**
 * A temporary writer used by GSP to write to a StringWriter and later retrieve the value. It also converts
 * nulls into blank strings.
 *
 * @author Graeme Rocher
 * @since 0.5
 *
 *        <p/>
 *        Created: Apr 19, 2007
 *        Time: 5:49:46 PM
 */
public class GroovyPageTagWriter extends FastStringWriter {
    public GroovyPageTagWriter() {
        super(64);
    }

    public String getValue() {
        return super.toString();
    }
}
