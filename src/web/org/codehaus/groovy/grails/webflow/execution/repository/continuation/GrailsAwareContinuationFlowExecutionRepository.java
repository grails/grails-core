/* Copyright 2006-2007 Graeme Rocher
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
package org.codehaus.groovy.grails.webflow.execution.repository.continuation;

import org.springframework.webflow.execution.repository.continuation.ContinuationFlowExecutionRepository;
import org.springframework.webflow.execution.repository.support.FlowExecutionStateRestorer;
import org.springframework.webflow.conversation.ConversationManager;
import org.codehaus.groovy.grails.commons.GrailsApplication;

/**
 * extends Webflows default ContinuationFlowExecutionRepository to allow the continuation factory to
 * be aware of the Grails class loader
 *
 * @author Graeme Rocher
 * @since 0.6
 * 
 *        <p/>
 *        Created: Jul 6, 2007
 *        Time: 10:46:32 PM
 */
public class GrailsAwareContinuationFlowExecutionRepository extends ContinuationFlowExecutionRepository {
    public GrailsAwareContinuationFlowExecutionRepository(FlowExecutionStateRestorer executionStateRestorer, ConversationManager conversationManagerToUse, GrailsApplication grailsApplication) {
        super(executionStateRestorer, conversationManagerToUse);

        setContinuationFactory(new GrailsAwareSerializedFlowExecutionContinuationFactory(grailsApplication));
    }
}
