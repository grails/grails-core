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

package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import spock.lang.Issue

/**
 */
class CountByWithDefaultSortOrderSpec extends GormSpec{

    @Issue('GRAILS-10232')
    void "Test that countBy works when there is a default sort order defined" () {
        when:"A countBy query is executed"
            int count = CountByWithDefaultSortOrder.countByActivate(true)
        then:"No exception is thrown and the result is correct"
            count == 0
    }
    @Override
    List getDomainClasses() {
       [CountByWithDefaultSortOrder]
    }
}

@Entity
class CountByWithDefaultSortOrder {

    int sortIndex;
    String code;
    boolean activate = true;

    static mapping = {
        id generator: 'increment'
        code unique: true
        sort 'sortIndex'
    }

    static constraints = {
        code nullable: false, blank: false, minSize: 1, unique: true
        sortIndex nullable: false
    }
}
