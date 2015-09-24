package grails.events

import org.grails.events.spring.SpringEventTranslator
import org.springframework.context.event.ContextStartedEvent
import org.springframework.context.support.GenericApplicationContext
import reactor.Environment
import reactor.bus.Event
import reactor.bus.EventBus
import spock.lang.Specification

/*
 * Copyright 2014 original authors
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

/**
 * @author graemerocher
 */
class SpringEventTranslatorSpec extends Specification {

    void "Test event translator translates Spring events"() {
        given:"A translator"

        def eventBus = Mock(EventBus)
        def translator = new SpringEventTranslator(eventBus: eventBus)

        when:"A Spring event occurs"

        def ctx = new GenericApplicationContext()
        translator.onApplicationEvent(new ContextStartedEvent(ctx))

        then:"The event bus is notified"
        1 * eventBus.notify("spring:contextStarted", _)
    }

}
