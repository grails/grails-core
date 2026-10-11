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

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.Prompt

import spock.lang.Specification

class ScriptedChatModelSpec extends Specification {

    ScriptedChatModel model = new ScriptedChatModel()

    void 'answers with the scripted responses in order, then echoes the user message'() {
        given:
        model.respondWith('first', 'second')

        expect:
        model.call(new Prompt('one')).result.output.text == 'first'
        model.call(new Prompt('two')).result.output.text == 'second'
        model.call(new Prompt('three')).result.output.text == 'three'
    }

    void 'a scripted responder sees the prompt it answers'() {
        given:
        model.respondWith { Prompt prompt -> "You said: ${prompt.userMessage.text}".toString() }

        expect:
        model.call(new Prompt('hi')).result.output.text == 'You said: hi'
    }

    void 'records every prompt it receives'() {
        when:
        model.call(new Prompt('a'))
        model.stream(new Prompt('b')).blockLast()

        then:
        model.prompts*.contents == ['a', 'b']
        model.lastPrompt.userMessage.text == 'b'
        model.lastUserText == 'b'
    }

    void 'has received nothing to begin with'() {
        expect:
        model.prompts.empty
        model.lastPrompt == null
        model.lastUserText == null
    }

    void 'streams the answer a word at a time'() {
        given:
        model.respondWith('Hello streaming world')

        when:
        List<String> chunks = model.stream(new Prompt('x')).map { it.result.output.text }.collectList().block()

        then:
        chunks == ['Hello ', 'streaming ', 'world']
    }

    void 'the chunks of a streamed answer keep its whitespace'() {
        expect:
        ScriptedChatModel.chunk(text) == chunks
        chunks.join('') == text

        where:
        text       | chunks
        'one'      | ['one']
        'a  b\nc'  | ['a ', ' ', 'b\n', 'c']
        ''         | ['']
    }

    void 'answers a chat client, synchronously and streamed'() {
        given:
        model.respondWith('42')
        ChatClient chatClient = ChatClient.create(model)

        expect:
        chatClient.prompt().user('What is six times seven?').call().content() == '42'
        model.lastUserText == 'What is six times seven?'
        chatClient.prompt().user('again').stream().content().collectList().block() == ['again']
    }

    void 'reset forgets the script and the prompts'() {
        given:
        model.respondWith('scripted')
        model.call(new Prompt('before'))

        when:
        model.reset()

        then:
        model.prompts.empty
        model.call(new Prompt('after')).result.output.text == 'after'
    }

}
