/*
 * Copyright 2012 the original author or authors.
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

package grails.test.mixin

import grails.artefact.Artefact
import grails.rest.RestfulController
import org.springframework.http.HttpStatus
import spock.lang.Specification

/**
 */
@TestFor(SecondVideoController)
@Mock(Video)
class RestfulControllerSuperClassSpec extends Specification {

    def populateValidParams(params) {
        assert params != null
    }

    void "Test the index action returns the correct model"() {

        when:"The index action is executed"
        controller.index()

        then:"The model is correct"
        assert !model.videoList
        assert model.videoCount == 0
    }

    void "Test the save action returns the correct model, status and location"() {
        when:"The save action is executed"
            request.method = 'POST'
            controller.params['title'] = 'TestVideo'
            controller.save()

        then:"The model is created successfully"
            model.video != null
            response.status == HttpStatus.CREATED.value()
            response.getHeader('Location') != null

    }

    void "Test the update action returns the correct model, status and location"() {
        given: "An existing domain object and Restful controller"
            def video = new Video(title:'Existing').save()
        when:"The update action is executed on controller"
            request.method = 'PUT'
            controller.params['id']=video.id
            controller.params['title'] = 'Updated'
            controller.update()

        then:"The model is created successfully"
            model.video != null
            response.status == HttpStatus.OK.value()
            response.getHeader('Location') != null

    }

    void "Test the patch action returns the correct model, status and location"() {
        given: "An existing domain object and Restful controller"
            def video = new Video(title:'Existing').save()
        when:"The patch action is executed on controller"
            request.method = 'PATCH'
            controller.params['id']=video.id
            controller.params['title'] = 'Updated'
            controller.patch()

        then:"The model is created successfully"
            model.video != null
            response.status == HttpStatus.OK.value()
            response.getHeader('Location') != null

    }


}

@Artefact("Controller")
class SecondVideoController extends RestfulController<Video> {
    SecondVideoController() {
        super(Video)
    }
}
