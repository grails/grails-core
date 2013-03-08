package grails.async

import spock.lang.Specification

/**
 */
class PromiseMapSpec extends Specification{

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
}
