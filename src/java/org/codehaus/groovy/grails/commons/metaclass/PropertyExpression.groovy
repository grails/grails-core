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

package org.codehaus.groovy.grails.commons.metaclass
/**
 *
 * This class is designed for use with Groovy property access expressions like foo.bar.stuff
 *
 * It takes the expression and produces a String equivalent like "foo.bar.stuff" which can then later be
 * used to evaluate the value within a different context
 * 
 * @author Graeme Rocher
 * @since 1.1
 * 
 * Created: Jul 22, 2008
 */
class PropertyExpression {

    private StringBuffer propertyExpression

    def getValue() { propertyExpression.toString() }

    public PropertyExpression(String initialName) {
        super();
        propertyExpression = new StringBuffer(initialName)
    }

    public Object getProperty(String name) {
        propertyExpression << ".$name"

        return this
    }

}