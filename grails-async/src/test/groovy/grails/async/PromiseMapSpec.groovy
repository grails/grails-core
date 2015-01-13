/*
 * Copyright 2013 SpringSource
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
package grails.async

import spock.lang.Ignore
import spock.lang.Specification

/**
 * @author Graeme Rocher
 * @since 2.3
 */
class PromiseMapSpec extends Specification{

    void "Test PromiseMap with mixture of normal entries and promises populated via constructor"() {
        when:"A promise map is used with an onComplete handler"
            def map = new PromiseMap<String, Integer>(one:{1}, four:4, eight:{4*2})
            Map<String, Integer> result
            map.onComplete { Map<String, Integer> m ->
                result = m
            }

            sleep 300

        then:"An appropriately populated map is returned to the onComplete event"
            result != null
            result["one"] == 1
            result["four"] == 4
            result["eight"] == 8

    }
    void "Test PromiseMap with mixture of normal entries and promises"() {
        when:"A promise map is used with an onComplete handler"
            def map = new PromiseMap<String, Integer>()
            map["one"] = { 1 }
            map["four"] = 4
            map["eight"] = { 4 * 2 }

            Map<String, Integer> result
            map.onComplete { Map<String, Integer> m ->
                result = m
            }

            sleep 300

        then:"An appropriately populated map is returned to the onComplete event"
            result != null
            result["one"] == 1
            result["four"] == 4
            result["eight"] == 8

    }

    void "Test that a PromiseMap populates values from promises onComplete"() {
        when:"A promise map is used with an onComplete handler"
            def map = new PromiseMap<String, Integer>()
            map["one"] = { 1 }
            map["four"] = { 2 + 2 }
            map["eight"] = { 4 * 2 }

            Map<String, Integer> result
            map.onComplete { Map<String, Integer> m ->
                result = m
            }

            sleep 300

        then:"An appropriately populated map is returned to the onComplete event"
            result != null
            result["one"] == 1
            result["four"] == 4
            result["eight"] == 8


    }


    void "Test that a PromiseMap triggers onError for an exception and ignoresonComplete"() {
        when:"A promise map is used with an onComplete handler"
            def map = new PromiseMap<String, Integer>()
            map["one"] = { 1 }
            map["four"] = { throw new RuntimeException("bad") }
            map["eight"] = { 4 * 2 }

            Map<String, Integer> result
            Throwable err
            map.onComplete { Map<String, Integer> m ->
                result = m
            }
            map.onError {
                err = it
            }

        sleep 300

        then:"An appropriately populated map is returned to the onComplete event"
            result == null
            err != null
            err.message == "bad"

    }

    @Ignore
    void "Test PromiseMap with then chaining"() {
        when:"A promise map is used with then chaining"
            def map = new PromiseMap<String, Integer>()
            map["one"] = { 1 }
            def promise = map.then {
                println it
                it['four'] = 4; it
            }.then {
                println it
                it['eight'] = 8; it
            }
            def result = promise.get()
        then:"An appropriately populated map is returned to the onComplete event"
            result != null
            result["one"] == 1
            result["four"] == 4
            result["eight"] == 8

    }
}
