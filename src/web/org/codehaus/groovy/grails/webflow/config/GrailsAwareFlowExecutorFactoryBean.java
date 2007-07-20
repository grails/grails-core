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
package org.codehaus.groovy.grails.webflow.config;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.codehaus.groovy.grails.webflow.execution.repository.continuation.GrailsAwareClientContinuationFlowExecutionRepository;
import org.codehaus.groovy.grails.webflow.execution.repository.continuation.GrailsAwareContinuationFlowExecutionRepository;
import org.springframework.util.Assert;
import org.springframework.webflow.config.FlowExecutorFactoryBean;
import org.springframework.webflow.config.RepositoryType;
import org.springframework.webflow.conversation.ConversationManager;
import org.springframework.webflow.execution.repository.FlowExecutionRepository;
import org.springframework.webflow.execution.repository.continuation.ContinuationFlowExecutionRepository;
import org.springframework.webflow.execution.repository.support.FlowExecutionStateRestorer;

/**
 * <p>Extends Spring WebFlow's FlowExecutorFactoryBean to supply an alternative implementation of the
 * ContinuationFlowExecutionRepository that is capable of loading classes from the Grails class loader
 *
 * @see org.springframework.webflow.execution.repository.continuation.ContinuationFlowExecutionRepository
 * @see FlowExecutorFactoryBean 
 *
 * @author Graeme Rocher
 * @since 0.6
 *
 *        <p/>
 *        Created: Jul 6, 2007
 *        Time: 10:39:36 PM
 */
public class GrailsAwareFlowExecutorFactoryBean extends FlowExecutorFactoryBean implements GrailsApplicationAware {

    private GrailsApplication grailsApplication;

    /**
     * The GrailsApplication instance to use to load classes from
     *
     * @param grailsApplication The GrailsApplication instance
     */
    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    /**
     * Overrides default implementation to supply an alternative implemenation of ContinuationFlowExecutionRepository when
     * the repositoryType a CONTINUATION repository
     *
     * @see RepositoryType#CONTINUATION
     * @see org.springframework.webflow.config.FlowExecutorFactoryBean#createExecutionRepository(org.springframework.webflow.config.RepositoryType, org.springframework.webflow.execution.repository.support.FlowExecutionStateRestorer, org.springframework.webflow.conversation.ConversationManager)
     */
    protected FlowExecutionRepository createExecutionRepository(RepositoryType repositoryType, FlowExecutionStateRestorer executionStateRestorer, ConversationManager conversationManager) {
        Assert.notNull(grailsApplication, "The property [grailsApplication] must be set!");

        // determine the conversation manager to use
        ConversationManager conversationManagerToUse = conversationManager;
        if (conversationManagerToUse == null) {
            conversationManagerToUse = createDefaultConversationManager();
        }

        if (repositoryType == RepositoryType.CLIENT) {
			if (conversationManager == null) {
				// use the default no-op conversation manager
				return new GrailsAwareClientContinuationFlowExecutionRepository(executionStateRestorer, grailsApplication);
			}
			else {
				// use the conversation manager specified by the user
				return new GrailsAwareClientContinuationFlowExecutionRepository(executionStateRestorer, conversationManager, grailsApplication);
			}
		}else if (repositoryType == RepositoryType.CONTINUATION) {
            ContinuationFlowExecutionRepository repository =
                new GrailsAwareContinuationFlowExecutionRepository(executionStateRestorer, conversationManagerToUse, grailsApplication);
            if (getMaxContinuations() != null) {
                repository.setMaxContinuations(getMaxContinuations().intValue());
            }
            return repository;
        }
        else {
            return super.createExecutionRepository(repositoryType, executionStateRestorer, conversationManager);
        }
    }
}
