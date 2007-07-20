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
import org.springframework.util.Assert;
import org.springframework.webflow.execution.repository.continuation.FlowExecutionContinuationFactory;
import org.springframework.webflow.execution.repository.continuation.SerializedFlowExecutionContinuationFactory;
import org.springframework.webflow.execution.repository.continuation.FlowExecutionContinuation;
import org.springframework.webflow.execution.repository.continuation.ContinuationCreationException;
import org.springframework.webflow.execution.FlowExecution;

/**
 * <p>Extends the default SerializedFlowExecutionContinuationFactory to provide a extended SerializedFlowExecutionContinuation
 * that is capable of loading classes from the GrailsApplication instance .
 *
 * @see org.springframework.webflow.execution.repository.continuation.SerializedFlowExecutionContinuation
 * @see SerializedFlowExecutionContinuationFactory
 *
 * @author Graeme Rocher
 * @since 0.6
 *
 *        <p/>
 *        Created: Jul 6, 2007
 *        Time: 10:57:27 PM
 */
public class GrailsAwareSerializedFlowExecutionContinuationFactory extends SerializedFlowExecutionContinuationFactory implements FlowExecutionContinuationFactory {
    private GrailsApplication grailsApplication;

    public GrailsAwareSerializedFlowExecutionContinuationFactory(GrailsApplication grailsApplication) {
        Assert.notNull(grailsApplication, "Argument [grailsApplication] is required!");
        this.grailsApplication = grailsApplication;
    }

    public FlowExecutionContinuation createContinuation(FlowExecution flowExecution) throws ContinuationCreationException {
        final GrailsAwareSerializedFlowExecutionContinuation executionContinuation = new GrailsAwareSerializedFlowExecutionContinuation(flowExecution, getCompress());
        executionContinuation.setGrailsApplication(grailsApplication);
        return executionContinuation;
    }
}
