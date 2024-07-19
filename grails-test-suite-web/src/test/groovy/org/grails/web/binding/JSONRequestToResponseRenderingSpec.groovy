package org.grails.web.binding

import grails.artefact.Artefact
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Ignore
import spock.lang.Specification

@Ignore('grails-gsp is not on jakarta.servlet yet')
class JSONRequestToResponseRenderingSpec extends Specification implements ControllerUnitTest<ArrayController> {

    def "Test that JSON arrays are correctly converted in controllers"() {
        given:"A JSON request containing arrays"
            request.json = '''
            {
	"track": {
		"start_time": 1316975696560,
		"segments": [
			{
				"coordinates": [
					[
						47.8897441833333,
						-122.732959033333,
						101.1,
						1316975697100
					],
					[
						47.8898427833333,
						-122.732921583333,
						109.4,
						1316975704100
					]
				]
			}
		]
	}
}
'''
        when:"The params are rendered as JSON"
            controller.list()
        then:"Check that the JSON is convereted back correctly"
            response.json.track.segments != null
    }
}

@Artefact('Controller')
class ArrayController {
    def list() {
        def json = request.JSON

        render json
    }
}
