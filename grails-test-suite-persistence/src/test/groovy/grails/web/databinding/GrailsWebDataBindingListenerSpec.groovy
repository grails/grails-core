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
package grails.web.databinding

import grails.databinding.SimpleMapDataBindingSource
import grails.databinding.events.DataBindingListenerAdapter
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.grails.testing.GrailsUnitTest
import spock.lang.Specification

class GrailsWebDataBindingListenerSpec extends Specification implements GrailsUnitTest {

    DataBindingListenerAdapter dataBindingListenerAdapter = Mock()

    Closure doWithSpring() { { ->
            testWidgetDataBindingListener(InstanceFactoryBean, dataBindingListenerAdapter, DataBindingListenerAdapter)
        }
    }

    void "test that DataBindingListener is added to GrailsWebDataBinder"() {

        given:
        GrailsWebDataBinder binder = grailsApplication.mainContext.getBean(DataBindingUtils.DATA_BINDER_BEAN_NAME)
        TestWidget testWidget = new TestWidget()

        when:
        binder.bind(testWidget, ["name": "Clock"] as SimpleMapDataBindingSource)

        then:
        1 * dataBindingListenerAdapter.supports(TestWidget) >> true
        1 * dataBindingListenerAdapter.beforeBinding(testWidget, _)
        1 * dataBindingListenerAdapter.afterBinding(testWidget, _)
    }

    static class TestWidget {
        String name
    }
}

