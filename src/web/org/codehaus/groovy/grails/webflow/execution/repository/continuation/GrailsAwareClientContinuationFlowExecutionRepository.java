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

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.webflow.conversation.ConversationManager;
import org.springframework.webflow.execution.repository.continuation.ClientContinuationFlowExecutionRepository;
import org.springframework.webflow.execution.repository.continuation.FlowExecutionContinuation;
import org.springframework.webflow.execution.repository.continuation.ContinuationUnmarshalException;
import org.springframework.webflow.execution.repository.support.FlowExecutionStateRestorer;
import org.springframework.webflow.execution.repository.FlowExecutionKey;
import org.springframework.webflow.execution.repository.FlowExecutionRestorationFailureException;
import org.springframework.webflow.execution.FlowExecution;

/**
 * extends Webflows default ClientContinuationFlowExecutionRepository to allow the continuation factory to
 * be aware of the Grails class loader
 *
 * @author Graeme Rocher
 * @since 0.6
 *        <p/>
 *        Created: Jul 20, 2007
 *        Time: 8:48:01 AM
 */
public class GrailsAwareClientContinuationFlowExecutionRepository extends ClientContinuationFlowExecutionRepository {
    private GrailsApplication grailsApplication;

    public GrailsAwareClientContinuationFlowExecutionRepository(FlowExecutionStateRestorer executionStateRestorer, ConversationManager conversationManagerToUse, GrailsApplication grailsApplication) {
        super(executionStateRestorer, conversationManagerToUse);

        setContinuationFactory(new GrailsAwareSerializedFlowExecutionContinuationFactory(grailsApplication));
        this.grailsApplication = grailsApplication;
    }

    public GrailsAwareClientContinuationFlowExecutionRepository(FlowExecutionStateRestorer executionStateRestorer, GrailsApplication grailsApplication) {
        super(executionStateRestorer);
        setContinuationFactory(new GrailsAwareSerializedFlowExecutionContinuationFactory(grailsApplication));
        this.grailsApplication = grailsApplication;
    }

	public FlowExecution getFlowExecution(FlowExecutionKey key) {
		if (logger.isDebugEnabled()) {
			logger.debug("Getting flow execution with key '" + key + "'");
		}

		// note that the call to getConversationScope() below will try to obtain
		// the conversation identified by the key, which will fail if that conversation
		// is no longer managed by the conversation manager (i.e. it has expired)

		FlowExecutionContinuation continuation = decode((String)getContinuationId(key));
        if(continuation instanceof GrailsAwareSerializedFlowExecutionContinuation) {
            ((GrailsAwareSerializedFlowExecutionContinuation)continuation).setGrailsApplication(grailsApplication);
        }
        try {
			FlowExecution execution = continuation.unmarshal();
			// the flow execution was deserialized so we need to restore transient
			// state
			return getExecutionStateRestorer().restoreState(execution, getConversationScope(key));
		}
		catch (ContinuationUnmarshalException e) {
			throw new FlowExecutionRestorationFailureException(key, e);
		}
	}
}
