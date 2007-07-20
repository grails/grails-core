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
package org.codehaus.groovy.grails.webflow.executor;

import org.springframework.webflow.executor.FlowExecutorImpl;
import org.springframework.webflow.executor.ResponseInstruction;
import org.springframework.webflow.definition.registry.FlowDefinitionLocator;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.execution.FlowExecutionFactory;
import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.execution.ViewSelection;
import org.springframework.webflow.execution.FlowExecutionContextHolder;
import org.springframework.webflow.execution.repository.FlowExecutionRepository;
import org.springframework.webflow.execution.repository.FlowExecutionKey;
import org.springframework.webflow.execution.repository.FlowExecutionLock;
import org.springframework.webflow.context.ExternalContext;
import org.springframework.webflow.context.ExternalContextHolder;
import org.springframework.webflow.core.FlowException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class description here.
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Jul 20, 2007
 *        Time: 5:46:45 PM
 */
public class GrailsFlowExecutor extends FlowExecutorImpl {

    private static final Log logger = LogFactory.getLog(FlowExecutorImpl.class);
    /**
     * Create a new flow executor.
     *
     * @param definitionLocator   the locator for accessing flow definitions to
     *                            execute
     * @param executionFactory    the factory for creating executions of flow
     *                            definitions
     * @param executionRepository the repository for persisting paused flow
     *                            executions
     */
    public GrailsFlowExecutor(FlowDefinitionLocator definitionLocator, FlowExecutionFactory executionFactory, FlowExecutionRepository executionRepository) {
        super(definitionLocator, executionFactory, executionRepository);
    }

    public ResponseInstruction launch(String flowDefinitionId, ExternalContext context) throws FlowException {
		if (logger.isDebugEnabled()) {
			logger.debug("Launching flow execution for flow definition '" + flowDefinitionId + "'");
		}
		// expose external context as a thread-bound service
		ExternalContextHolder.setExternalContext(context);
		try {
			FlowDefinition flowDefinition = getDefinitionLocator().getFlowDefinition(flowDefinitionId);
			FlowExecution flowExecution = getExecutionFactory().createFlowExecution(flowDefinition);
            FlowExecutionContextHolder.setFlowExecutionContext(flowExecution);
            ViewSelection selectedView = flowExecution.start(createInput(context), context);
			if (flowExecution.isActive()) {
				// execution still active => store it in the repository
                final FlowExecutionRepository executionRepository = getExecutionRepository();
                FlowExecutionKey key = executionRepository.generateKey(flowExecution);
				FlowExecutionLock lock = executionRepository.getLock(key);
				lock.lock();
				try {
					executionRepository.putFlowExecution(key, flowExecution);
				}
				finally {
					lock.unlock();
				}
				return new ResponseInstruction(key.toString(), flowExecution, selectedView);
			}
			else {
				// execution already ended => just render the selected view
				return new ResponseInstruction(flowExecution, selectedView);
			}
		}
		finally {
            cleanUpAfterFlowExecution();
		}
    }

    private void cleanUpAfterFlowExecution() {
        FlowExecutionContextHolder.setFlowExecutionContext(null);
        ExternalContextHolder.setExternalContext(null);
    }

    public ResponseInstruction resume(String flowExecutionKey, String eventId, ExternalContext context) throws FlowException {
        if (logger.isDebugEnabled()) {
            logger.debug("Resuming flow execution with key '" + flowExecutionKey +
                    "' on user event '" + eventId + "'");
        }
        // expose external context as a thread-bound service
        ExternalContextHolder.setExternalContext(context);
        try {
            final FlowExecutionRepository executionRepository = getExecutionRepository();
            FlowExecutionKey key = executionRepository.parseFlowExecutionKey(flowExecutionKey);
            FlowExecutionLock lock = executionRepository.getLock(key);
            // make sure we're the only one manipulating the flow execution
            lock.lock();
            try {
                FlowExecution flowExecution = executionRepository.getFlowExecution(key);
                FlowExecutionContextHolder.setFlowExecutionContext(flowExecution);
                ViewSelection selectedView = flowExecution.signalEvent(eventId, context);
                if (flowExecution.isActive()) {
                    // execution still active => store it in the repository
                    key = executionRepository.getNextKey(flowExecution, key);
                    executionRepository.putFlowExecution(key, flowExecution);
                    return new ResponseInstruction(key.toString(), flowExecution, selectedView);
                }
                else {
                    // execution ended => remove it from the repository
                    executionRepository.removeFlowExecution(key);
                    return new ResponseInstruction(flowExecution, selectedView);
                }
            }
            finally {
                lock.unlock();
            }
        }
        finally {
            cleanUpAfterFlowExecution();
        }

    }

    public ResponseInstruction refresh(String flowExecutionKey, ExternalContext context) throws FlowException {
		if (logger.isDebugEnabled()) {
			logger.debug("Refreshing flow execution with key '" + flowExecutionKey + "'");
		}
		// expose external context as a thread-bound service
		ExternalContextHolder.setExternalContext(context);
		try {
            final FlowExecutionRepository executionRepository = getExecutionRepository();
            FlowExecutionKey key = executionRepository.parseFlowExecutionKey(flowExecutionKey);
			FlowExecutionLock lock = executionRepository.getLock(key);
			// make sure we're the only one manipulating the flow execution
			lock.lock();
			try {
				FlowExecution flowExecution = executionRepository.getFlowExecution(key);
                FlowExecutionContextHolder.setFlowExecutionContext(flowExecution);
                ViewSelection selectedView = flowExecution.refresh(context);
				// don't generate a new key for a refresh, just update
				// the flow execution with it's existing key
				executionRepository.putFlowExecution(key, flowExecution);
				return new ResponseInstruction(key.toString(), flowExecution, selectedView);
			}
			finally {
				lock.unlock();
			}
		}
		finally {
		    cleanUpAfterFlowExecution();
		}
    }
}
