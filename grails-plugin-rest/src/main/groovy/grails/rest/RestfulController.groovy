/*
 * Copyright 2013 GoPivotal, Inc. All Rights Reserved.
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
package grails.rest

import grails.artefact.Artefact
import grails.transaction.Transactional
import grails.util.GrailsNameUtils
import static org.springframework.http.HttpStatus.*

/**
 * Base class that can be extended to get the basic CRUD operations needed for a RESTful API
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@Artefact("Controller")
@Transactional
class RestfulController {
    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    Class resource
    String resourceName
    String resourceClassName

    RestfulController(Class resource) {
        this.resource = resource
        this.resourceClassName = resource.simpleName
        this.resourceName = GrailsNameUtils.getPropertyName(resource)
    }

    /**
     * Lists all resources up to the given maximum
     *
     * @param max The maximum
     * @return A list of resources
     */
    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        def model = [:]
        model.put("${resourceName}".toString(), resource.count())
        respond resource.list(params), model:model
    }

    /**
     * Shows a single resource
     * @param id The id of the resource
     * @return The rendered resource or a 404 if it doesn't exist
     */
    def show(Long id) {
        respond resource.get(id)
    }

    /**
     * Displays a form to create a new resource
     */
    def create() {
        respond resource.newInstance(getParametersToBind())
    }

    /**
     * Saves a resource
     */
    @Transactional
    def save() {
        def instance = resource.newInstance(getParametersToBind())
        instance.validate()
        if(instance.hasErrors()) {
            respond instance.errors, view:'create' // STATUS CODE 422
        }
        else {
            instance.save flush:true
            withFormat {
                html {
                    flash.message = message(code: 'default.created.message', args: [message(code: "${resourceName}.label".toString(), default: resourceClassName), instance.id])
                    redirect instance
                }
                '*' { render status: CREATED }
            }
        }
    }

    def edit(Long id) {
        respond resource.get(id)
    }

    /**
     * Updates a resource for the given id
     * @param id
     */
    @Transactional
    def update(Long id) {
        def instance = resource.get(id)
        if(instance == null) {
            render status:404
            return
        }
        else {
            instance.properties = getParametersToBind()
        }
        if(instance.hasErrors()) {
            respond instance.errors, view:'edit' // STATUS CODE 422
        }
        else {
            instance.save flush:true
            withFormat {
                html {
                    flash.message = message(code: 'default.updated.message', args: [message(code: "${resourceClassName}.label".toString(), default: resourceClassName), instance.id])
                    redirect instance
                }
                '*'{ render status: OK }
            }
        }
    }

    /**
     * Deletes a resource for the given id
     * @param id The id
     */
    @Transactional
    def delete(Long id ) {
        def instance = resource.get(id)
        if(instance) {
            instance.delete flush:true
            withFormat {
                html {
                    flash.message = message(code: 'default.deleted.message', args: [message(code: "${resourceClassName}.label".toString(), default: resourceClassName), instance.id])
                    redirect action:"index", method:"GET"
                }
                '*'{ render status: NO_CONTENT } // NO CONTENT STATUS CODE
            }
        }
        else {
            render status: NOT_FOUND
        }
    }

    /**
     * The parameters that can be bound to a domain instance. Defaults to all, subclasses should override and customize the behavior
     *
     * @return The parameters
     */
    protected Map getParametersToBind() {
        this.params
    }

}
