/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.grails.testing.ai

import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.SimpleVectorStore
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.ApplicationContextRunner

import spock.lang.Specification

class AiTestingAutoConfigurationSpec extends Specification {

    private static final String[] NO_PROVIDER = ['spring.ai.model.chat=none', 'spring.ai.model.embedding=none']

    private ApplicationContextRunner contextRunner() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AiTestingAutoConfiguration, AiTestingVectorStoreAutoConfiguration))
    }

    void 'the stubs stand in for the models when no provider is selected'() {
        expect:
        contextRunner().withPropertyValues(NO_PROVIDER).run { context ->
            assert context.getBean(ChatModel) instanceof ScriptedChatModel
            assert context.getBean(EmbeddingModel) instanceof StubEmbeddingModel
            assert context.getBean(EmbeddingModel).dimensions() == StubEmbeddingModel.DEFAULT_DIMENSIONS
            assert context.getBean(VectorStore) instanceof SimpleVectorStore
        }
    }

    void 'the stub embedding model takes its dimensions from the configuration'() {
        expect:
        contextRunner().withPropertyValues(NO_PROVIDER).withPropertyValues('grails.ai.stub.embedding-dimensions=16')
                .run { context ->
                    assert context.getBean(EmbeddingModel).dimensions() == 16
                }
    }

    void 'the stubs back off when a provider is selected'() {
        expect:
        contextRunner().withPropertyValues('spring.ai.model.chat=openai', 'spring.ai.model.embedding=openai')
                .run { context ->
                    assert !context.getBeanNamesForType(ChatModel)
                    assert !context.getBeanNamesForType(EmbeddingModel)
                    assert !context.getBeanNamesForType(VectorStore)
                }
    }

    void 'the stubs back off when disabled'() {
        expect:
        contextRunner().withPropertyValues(NO_PROVIDER).withPropertyValues('grails.ai.stub.enabled=false')
                .run { context ->
                    assert !context.getBeanNamesForType(ChatModel)
                    assert !context.getBeanNamesForType(EmbeddingModel)
                    assert !context.getBeanNamesForType(VectorStore)
                }
    }

    void 'the models and the vector store the application defines win'() {
        given:
        ChatModel ownChatModel = new ScriptedChatModel()
        EmbeddingModel ownEmbeddingModel = new StubEmbeddingModel(3)
        VectorStore ownVectorStore = SimpleVectorStore.builder(ownEmbeddingModel).build()

        expect:
        contextRunner().withPropertyValues(NO_PROVIDER)
                .withBean('ownChatModel', ChatModel, { -> ownChatModel })
                .withBean('ownEmbeddingModel', EmbeddingModel, { -> ownEmbeddingModel })
                .withBean('ownVectorStore', VectorStore, { -> ownVectorStore })
                .run { context ->
                    assert context.getBean(ChatModel).is(ownChatModel)
                    assert context.getBean(EmbeddingModel).is(ownEmbeddingModel)
                    assert context.getBean(VectorStore).is(ownVectorStore)
                    assert !context.containsBean('chatModel')
                    assert !context.containsBean('embeddingModel')
                    assert !context.containsBean('vectorStore')
                }
    }

    void 'no vector store is registered without the vector store module'() {
        expect:
        contextRunner().withPropertyValues(NO_PROVIDER).withClassLoader(new FilteredClassLoader(SimpleVectorStore))
                .run { context ->
                    assert context.getBean(ChatModel) instanceof ScriptedChatModel
                    assert !context.getBeanNamesForType(VectorStore)
                }
    }

}
