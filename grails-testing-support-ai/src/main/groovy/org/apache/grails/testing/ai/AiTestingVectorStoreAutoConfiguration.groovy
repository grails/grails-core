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

import groovy.transform.CompileStatic

import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.SimpleVectorStore
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean

/**
 * Registers an in-memory {@link SimpleVectorStore} over the embedding model while the stubs are enabled
 * and the application has no vector store of its own, so code that retrieves documents can be tested
 * without a database.
 *
 * @since 8.1
 */
@CompileStatic
@AutoConfiguration(after = AiTestingAutoConfiguration)
@ConditionalOnClass(SimpleVectorStore)
@ConditionalOnProperty(prefix = AiStubProperties.PREFIX, name = 'enabled', havingValue = 'true', matchIfMissing = true)
class AiTestingVectorStoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(VectorStore)
    @ConditionalOnBean(EmbeddingModel)
    SimpleVectorStore vectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore.builder(embeddingModel).build()
    }

}
