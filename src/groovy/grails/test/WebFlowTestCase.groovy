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
package grails.test

import org.springframework.webflow.test.execution.AbstractFlowExecutionTests
import org.springframework.webflow.definition.FlowDefinition
import org.codehaus.groovy.grails.webflow.engine.builder.FlowBuilder
import org.springframework.webflow.execution.ViewSelection
import org.springframework.webflow.core.collection.ParameterMap
import org.springframework.webflow.core.collection.LocalParameterMap

/**
* A test harness for testing Grails flows
*
*/
abstract class WebFlowTestCase extends AbstractFlowExecutionTests {

    /**
     * <p>Subclasses should return the flow closure that within the controller. For example:
     * <code>return new TestController().myFlow</code>
     */
    abstract getFlow();

    /**
     * Sub classes should override to change flow id
     */
    String getFlowId() { "test" }

    FlowDefinition getFlowDefinition() {
        new FlowBuilder(getFlowId()).flow( getFlow() )
    }

    protected ViewSelection signalEvent(String name, Map params) {        
        return super.signalEvent(name, new LocalParameterMap(params));
    }


}