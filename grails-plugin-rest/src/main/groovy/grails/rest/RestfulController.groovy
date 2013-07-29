/*
 * Copyright 2013 the original author or authors.
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

import static org.springframework.http.HttpStatus.*
import grails.artefact.Artefact
import grails.transaction.Transactional
import grails.util.GrailsNameUtils

/**
 * Base class that can be extended to get the basic CRUD operations needed for a RESTful API.
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@Artefact("Controller")
@Transactional(readOnly = true)
class RestfulController<T> {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    Class<T> resource
    String resourceName
    String resourceClassName

    RestfulController(Class<T> resource) {
        this.resource = resource
        resourceClassName = resource.simpleName
        resourceName = GrailsNameUtils.getPropertyName(resource)
    }

    /**
     * Lists all resources up to the given maximum
     *
     * @param max The maximum
     * @return A list of resources
     */
    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond listAllResources(params), model: [("${resourceName}Count".toString()): countResources()]
    }

    /**
     * Shows a single resource
     * @param id The id of the resource
     * @return The rendered resource or a 404 if it doesn't exist
     */
    def show() {
        respond queryForResource(params.id)
    }

    /**
     * Displays a form to create a new resource
     */
    def create() {
        respond createResource(getParametersToBind())
    }

    /**
     * Saves a resource
     */
    @Transactional
    def save() {
        def instance = createResource(getParametersToBind())
        instance.validate()
        if (instance.hasErrors()) {
            respond instance.errors, view:'create' // STATUS CODE 422
        } else {
            instance.save flush:true
            request.withFormat {
                form {
                    flash.message = message(code: 'default.created.message', args: [message(code: "${resourceName}.label".toString(), default: resourceClassName), instance.id])
                    redirect instance
                }
                '*' { respond instance, [status: CREATED] }
            }
        }
    }

    def edit() {
        respond queryForResource(params.id)
    }

    /**
     * Updates a resource for the given id
     * @param id
     */
    @Transactional
    def update() {
        T instance = queryForResource(params.id)
        if (instance == null) {
            render status:404
            return
        }

        instance.properties = getParametersToBind()

        if (instance.hasErrors()) {
            respond instance.errors, view:'edit' // STATUS CODE 422
            return
        }

        instance.save flush:true
        request.withFormat {
            form {
                flash.message = message(code: 'default.updated.message', args: [message(code: "${resourceClassName}.label".toString(), default: resourceClassName), instance.id])
                redirect instance
            }
            '*'{ respond instance, [status: OK] }
        }
    }

    /**
     * Deletes a resource for the given id
     * @param id The id
     */
    @Transactional
    def delete() {
        def instance = queryForResource(params.id)
        if (!instance) {
            render status: NOT_FOUND
            return
        }

        instance.delete flush:true
        request.withFormat {
            form {
                flash.message = message(code: 'default.deleted.message', args: [message(code: "${resourceClassName}.label".toString(), default: resourceClassName), instance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT } // NO CONTENT STATUS CODE
        }
    }

    /**
     * The parameters that can be bound to a domain instance. Defaults to all, subclasses should override and customize the behavior
     *
     * @return The parameters
     */
    protected Map getParametersToBind() {
        params
    }

    /**
     * Queries for a resource for the given id
     *
     * @param id The id
     * @return The resource or null if it doesn't exist
     */
    protected T queryForResource(Serializable id) {
        resource.get(id)
    }

    /**
     * Creates a new instance of the resource for the given parameters
     *
     * @param params The parameters
     * @return The resource instance
     */
    protected T createResource(Map params) {
        resource.newInstance(params)
    }

    /**
     * List all of resource based on parameters
     *
     * @return List of resources or empty if it doesn't exist
     */
    protected List<T> listAllResources(Map params) {
        resource.list(params)
    }

    /**
     * Counts all of resources
     *
     * @return List of resources or empty if it doesn't exist
     */
    protected Integer countResources() {
        resource.count()
    }
}
