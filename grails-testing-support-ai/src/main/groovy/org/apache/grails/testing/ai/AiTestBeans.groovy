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

import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.BeanRegistrar
import org.springframework.beans.factory.BeanRegistry
import org.springframework.core.env.Environment
import org.springframework.util.ClassUtils

/**
 * The beans a unit test gets from {@code AiUnitTest}: the scripted chat model, the stub embedding
 * model, a {@code ChatClient.Builder} over the scripted model, and an in-memory vector store when
 * Spring AI's vector store module is on the classpath.
 *
 * @since 8.1
 */
@CompileStatic
class AiTestBeans {

    public static final String CHAT_MODEL = 'chatModel'
    public static final String EMBEDDING_MODEL = 'embeddingModel'
    public static final String CHAT_CLIENT_BUILDER = 'chatClientBuilder'
    public static final String VECTOR_STORE = 'vectorStore'

    public static final String SIMPLE_VECTOR_STORE = 'org.springframework.ai.vectorstore.SimpleVectorStore'

    static BeanRegistrar registrar() {
        return { BeanRegistry registry, Environment environment ->
            registry.registerBean(CHAT_MODEL, ScriptedChatModel) { it.primary() }
            registry.registerBean(EMBEDDING_MODEL, StubEmbeddingModel) { it.primary() }
            registry.registerBean(CHAT_CLIENT_BUILDER, ChatClient.Builder) {
                it.prototype()
                it.supplier { context -> ChatClient.builder(context.bean(CHAT_MODEL, ScriptedChatModel)) }
            }
            if (ClassUtils.isPresent(SIMPLE_VECTOR_STORE, AiTestBeans.classLoader)) {
                SimpleVectorStoreBeans.register(registry)
            }
        } as BeanRegistrar
    }

}
