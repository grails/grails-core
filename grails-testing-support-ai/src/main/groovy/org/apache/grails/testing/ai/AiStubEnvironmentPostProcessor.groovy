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

import org.springframework.boot.SpringApplication
import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.core.Ordered
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

/**
 * Turns the provider auto-configurations off while the stubs are enabled, so a test environment needs
 * neither credentials nor a database: every {@code spring.ai.model.*} selection property, the chat,
 * embedding, image, audio and moderation models', and {@code spring.ai.vectorstore.type} default to
 * {@code none}. The defaults sit below every other property source, so an application that names a
 * provider in its configuration keeps it, and the stub for that kind of model then backs off.
 *
 * @since 8.1
 */
@CompileStatic
class AiStubEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    public static final String PROPERTY_SOURCE_NAME = 'grailsAiStubDefaults'
    public static final String STUB_ENABLED = AiStubProperties.PREFIX + '.enabled'
    public static final String CHAT_MODEL = 'spring.ai.model.chat'
    public static final String EMBEDDING_MODEL = 'spring.ai.model.embedding'
    public static final String TEXT_EMBEDDING_MODEL = 'spring.ai.model.embedding.text'
    public static final String MULTIMODAL_EMBEDDING_MODEL = 'spring.ai.model.embedding.multimodal'
    public static final String IMAGE_MODEL = 'spring.ai.model.image'
    public static final String AUDIO_TRANSCRIPTION_MODEL = 'spring.ai.model.audio.transcription'
    public static final String AUDIO_SPEECH_MODEL = 'spring.ai.model.audio.speech'
    public static final String MODERATION_MODEL = 'spring.ai.model.moderation'
    public static final String VECTOR_STORE = 'spring.ai.vectorstore.type'
    public static final String NONE = 'none'

    /**
     * Every selection property Spring AI reads: one per kind of model, and the vector store's. Each is
     * defaulted to {@code none}, so no provider auto-configures a model that would need credentials.
     */
    public static final List<String> SELECTION_PROPERTIES = List.of(
            CHAT_MODEL, EMBEDDING_MODEL, TEXT_EMBEDDING_MODEL, MULTIMODAL_EMBEDDING_MODEL, IMAGE_MODEL,
            AUDIO_TRANSCRIPTION_MODEL, AUDIO_SPEECH_MODEL, MODERATION_MODEL, VECTOR_STORE)

    @Override
    void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getProperty(STUB_ENABLED, Boolean, Boolean.TRUE)) {
            return
        }
        if (environment.propertySources.contains(PROPERTY_SOURCE_NAME)) {
            return
        }
        Map<String, Object> defaults = [:]
        for (String property : SELECTION_PROPERTIES) {
            defaults[property] = NONE
        }
        environment.propertySources.addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults))
    }

    @Override
    int getOrder() {
        Ordered.LOWEST_PRECEDENCE
    }

}
