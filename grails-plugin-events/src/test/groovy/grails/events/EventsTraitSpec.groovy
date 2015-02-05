package grails.events

import reactor.Environment
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
class EventsTraitSpec extends Specification {

    void "Test event notification"() {
        when:"A new events class is created"

            def defaultEventBus = EventBus.create(new Environment())
            def f = new Foo(eventBus: defaultEventBus)
            def result
            f.on("test", {
                result = it
            })
            f.notify("test", 1)
            sleep 1000
        then:"The event was consumed"
            result == 1
    }
}

class Foo implements Events {

}
