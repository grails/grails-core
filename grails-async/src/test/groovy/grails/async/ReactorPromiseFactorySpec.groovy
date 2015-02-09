package grails.async

import org.grails.async.factory.reactor.ReactorPromiseFactory
import reactor.Environment
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
class ReactorPromiseFactorySpec extends Specification {

    void setup() {
        if(Environment.alive()) Environment.terminate()
    }

    void "Test that the ReactoryPromiseFactory can create promises"() {
        setup:
            def env = Environment.initialize()
            def promiseFactory = new ReactorPromiseFactory(env)

        when:"A promise is created"
            def promise = promiseFactory.createPromise({ 1 })

        then:"The value is returned"
            promise.get() == 1
        cleanup:
            env.shutdown()

    }
}
