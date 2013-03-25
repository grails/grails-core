/*
 * Copyright 2003-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.orm.hibernate.cfg

/**
 * Defines the identity generation strategy. In the case of a 'composite' identity the properties
 * array defines the property names that formulate the composite id.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class Identity {
    String generator = 'native'
    String column = 'id'
    String name
    NaturalId natural
    Class type = Long
    Map params = [:]

    String toString() { "id[generator:$generator, column:$column, type:$type]" }
}
