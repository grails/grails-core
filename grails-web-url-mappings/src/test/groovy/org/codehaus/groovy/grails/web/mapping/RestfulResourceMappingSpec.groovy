package org.codehaus.groovy.grails.web.mapping

import static org.springframework.http.HttpMethod.*
import grails.web.CamelCaseUrlConverter

import org.springframework.mock.web.MockServletContext

import spock.lang.Issue
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class RestfulResourceMappingSpec extends Specification{

    @Issue('GRAILS-10869')
    void 'Test resources and namespaced controller'() {
        given: 'A set of mappings'
        def urlMappingsHolder = getUrlMappingsHolder {
            "/a/parent"(resources: 'ParentA') {
                "/child"(resources: 'ChildA')
            }
            "/b/parent"(resources: 'ParentB', namespace: 'same') {
                "/child"(resources: 'ChildB', namespace: 'same')
            }
            "/c/parent"(resources: 'ParentC', namespace: 'uniqueParent') {
                "/child"(resources: 'ChildC', namespace: 'uniqueChild')
            }
        }

        when: 'The URL mappings are obtained'
        def urlMappings = urlMappingsHolder.urlMappings

        then:
        urlMappingsHolder.matchAll('/a/parent', 'GET')[0].controllerName == 'parentA'
        urlMappingsHolder.matchAll('/a/parent', 'GET')[0].namespace == null
        urlMappingsHolder.matchAll('/a/parent/1/child', 'GET')[0].controllerName == 'childA'
        urlMappingsHolder.matchAll('/a/parent/1/child', 'GET')[0].namespace == null
        
        urlMappingsHolder.matchAll('/b/parent', 'GET')[0].controllerName == 'parentB'
        urlMappingsHolder.matchAll('/b/parent', 'GET')[0].namespace == 'same'
        urlMappingsHolder.matchAll('/b/parent/1/child', 'GET')[0].controllerName == 'childB'
        urlMappingsHolder.matchAll('/b/parent/1/child', 'GET')[0].namespace == 'same'
        
        urlMappingsHolder.matchAll('/c/parent', 'GET')[0].controllerName == 'parentC'
        urlMappingsHolder.matchAll('/c/parent', 'GET')[0].namespace == 'uniqueParent'
        urlMappingsHolder.matchAll('/c/parent/1/child', 'GET')[0].controllerName == 'childC'
        urlMappingsHolder.matchAll('/c/parent/1/child', 'GET')[0].namespace == 'uniqueChild'
    }
    
    @Issue('GRAILS-10869')
    void 'Test resources and plugin controllers'() {
        given: 'A set of mappings'
        def urlMappingsHolder = getUrlMappingsHolder {
            "/a/parent"(resources: 'ParentA') {
                "/child"(resources: 'ChildA')
            }
            "/b/parent"(resources: 'ParentB', plugin: 'samePlugin') {
                "/child"(resources: 'ChildB', plugin: 'samePlugin')
            }
            "/c/parent"(resources: 'ParentC', plugin: 'uniqueParentPlugin') {
                "/child"(resources: 'ChildC', plugin: 'uniqueChildPlugin')
            }
        }

        when: 'The URL mappings are obtained'
        def urlMappings = urlMappingsHolder.urlMappings

        then:
        urlMappingsHolder.matchAll('/a/parent', 'GET')[0].controllerName == 'parentA'
        urlMappingsHolder.matchAll('/a/parent', 'GET')[0].pluginName == null
        urlMappingsHolder.matchAll('/a/parent/1/child', 'GET')[0].controllerName == 'childA'
        urlMappingsHolder.matchAll('/a/parent/1/child', 'GET')[0].pluginName == null
        
        urlMappingsHolder.matchAll('/b/parent', 'GET')[0].controllerName == 'parentB'
        urlMappingsHolder.matchAll('/b/parent', 'GET')[0].pluginName == 'samePlugin'
        urlMappingsHolder.matchAll('/b/parent/1/child', 'GET')[0].controllerName == 'childB'
        urlMappingsHolder.matchAll('/b/parent/1/child', 'GET')[0].pluginName == 'samePlugin'
        
        urlMappingsHolder.matchAll('/c/parent', 'GET')[0].controllerName == 'parentC'
        urlMappingsHolder.matchAll('/c/parent', 'GET')[0].pluginName == 'uniqueParentPlugin'
        urlMappingsHolder.matchAll('/c/parent/1/child', 'GET')[0].controllerName == 'childC'
        urlMappingsHolder.matchAll('/c/parent/1/child', 'GET')[0].pluginName == 'uniqueChildPlugin'
    }
    
    @Issue('GRAILS-10908')
    void 'Test groups with variables'()  {
        given: 'A resource mapping with child mappings'
        def urlMappingsHolder = getUrlMappingsHolder {
            group "/home", {
                "/browse/town/$town"(controller: 'browser', action:[GET:"index"]) {
                }

                "/browse/street/$street"(controller: 'browser', action:[GET:"index"]) {
                }
            }
        }

        when: 'The URL mappings are obtained'
        def urlMappings = urlMappingsHolder.urlMappings
        def browserMappings = urlMappings.findAll { it.controllerName == 'browser' }

        then: 'There are the correct number of mappings'
        urlMappings.size() == 2

        and: 'The browser controller has 2 mappings'
        browserMappings.size() == 2

        and: 'The mappings have the correct properties'
        browserMappings.find { it.constraints*.propertyName  == ['town']}
        browserMappings.find { it.constraints*.propertyName  == ['street']}
    }

    @Issue('GRAILS-10820')
    void 'Test that grouped params with dynamic variables product the correct mappings'() {
        given: 'A resource mapping with child mappings'
            def urlMappingsHolder = getUrlMappingsHolder {
                "/report"(controller: 'blah', action: 'report')
                group "/foo", {
                    "/blah/$foo/$action?"(controller: 'blah')
                    "/$foo/$action?"(controller: 'blah')
                }
                "/$foo?"(controller: 'blah', action: 'index')
            }

        when:"The URL mappings are obtained"
            def urlMappings = urlMappingsHolder.urlMappings

        then:"There are eight of them in total"
            urlMappings.size() == 4

        expect:
            urlMappingsHolder.matchAll('/report', 'GET')[0].controllerName == 'blah'
            urlMappingsHolder.matchAll('/report', 'GET')[0].actionName == 'report'
            urlMappingsHolder.matchAll('/foo/blah/stuff/go', 'GET')[0].controllerName == 'blah'
            urlMappingsHolder.matchAll('/foo/blah/stuff/go', 'GET')[0].parameters.action == 'go'
            urlMappingsHolder.matchAll('/foo/blah/stuff/go', 'GET')[0].parameters.foo == 'stuff'

            urlMappingsHolder.matchAll('/foo/stuff/go', 'GET')[0].controllerName == 'blah'
            urlMappingsHolder.matchAll('/foo/stuff/go', 'GET')[0].parameters.action == 'go'
            urlMappingsHolder.matchAll('/foo/stuff/go', 'GET')[0].parameters.foo == 'stuff'

            urlMappingsHolder.matchAll('/home', 'GET')[0].controllerName == 'blah'
            urlMappingsHolder.matchAll('/home', 'GET')[0].actionName == 'index'

    }
    @Issue('GRAILS-10835')
    void 'Test multiple nested mappings have correct constrained properties'() {
        given: 'A resource mapping with child mappings'
        def urlMappingsHolder = getUrlMappingsHolder {
            "/books"(resources: "book") {
                '/sellers'(resources:'seller') {
                    '/locations'(resources: 'location')
                }
                '/authors'(resources:'author')
                '/titles'(resources:'title')
            }
        }
        
        when: 'The URL mappings are obtained'
        def urlMappings = urlMappingsHolder.urlMappings
        def bookMappings = urlMappings.findAll { it.controllerName == 'book' }
        def authorMappings = urlMappings.findAll { it.controllerName == 'author' }
        def titleMappings = urlMappings.findAll { it.controllerName == 'title' }
        def sellersMappings = urlMappings.findAll { it.controllerName == 'seller' }
        def locationsMappings = urlMappings.findAll { it.controllerName == 'location' }
        
        then: 'There are the correct number of mappings'
        urlMappings.size() == 40
        
        and: 'Each controller has 7 mappings'
        bookMappings.size() == 8
        authorMappings.size() == 8
        titleMappings.size() == 8
        sellersMappings.size() == 8
        locationsMappings.size() == 8
        
        and: 'the book mappings have the expected constrained properties'
        bookMappings.find { it.actionName == 'index' }.constraints*.propertyName == ['format']
        bookMappings.find { it.actionName == 'create' }.constraints*.propertyName == []
        bookMappings.find { it.actionName == 'save' }.constraints*.propertyName == ['format']
        bookMappings.find { it.actionName == 'show' }.constraints*.propertyName == ['id', 'format']
        bookMappings.find { it.actionName == 'edit' }.constraints*.propertyName == ['id']
        bookMappings.find { it.actionName == 'update' }.constraints*.propertyName == ['id', 'format']
        bookMappings.find { it.actionName == 'patch' }.constraints*.propertyName == ['id', 'format']
        bookMappings.find { it.actionName == 'delete' }.constraints*.propertyName == ['id', 'format']
        
        and: 'the author mappings have the expected constrained properties'
        authorMappings.find { it.actionName == 'index' }.constraints*.propertyName == ['bookId', 'format']
        authorMappings.find { it.actionName == 'create' }.constraints*.propertyName == ['bookId']
        authorMappings.find { it.actionName == 'save' }.constraints*.propertyName == ['bookId', 'format']
        authorMappings.find { it.actionName == 'show' }.constraints*.propertyName == ['bookId', 'id', 'format']
        authorMappings.find { it.actionName == 'edit' }.constraints*.propertyName == ['bookId', 'id']
        authorMappings.find { it.actionName == 'update' }.constraints*.propertyName == ['bookId', 'id', 'format']
        authorMappings.find { it.actionName == 'patch' }.constraints*.propertyName == ['bookId', 'id', 'format']
        authorMappings.find { it.actionName == 'delete' }.constraints*.propertyName == ['bookId', 'id', 'format']
        
        and: 'the title mappings have the expected constrained properties'
        titleMappings.find { it.actionName == 'index' }.constraints*.propertyName == ['bookId', 'format']
        titleMappings.find { it.actionName == 'create' }.constraints*.propertyName == ['bookId']
        titleMappings.find { it.actionName == 'save' }.constraints*.propertyName == ['bookId', 'format']
        titleMappings.find { it.actionName == 'show' }.constraints*.propertyName == ['bookId', 'id', 'format']
        titleMappings.find { it.actionName == 'edit' }.constraints*.propertyName == ['bookId', 'id']
        titleMappings.find { it.actionName == 'update' }.constraints*.propertyName == ['bookId', 'id', 'format']
        titleMappings.find { it.actionName == 'patch' }.constraints*.propertyName == ['bookId', 'id', 'format']
        titleMappings.find { it.actionName == 'delete' }.constraints*.propertyName == ['bookId', 'id', 'format']
        
        and: 'the seller mappings have the expected constrained properties'
        sellersMappings.find { it.actionName == 'index' }.constraints*.propertyName == ['bookId', 'format']
        sellersMappings.find { it.actionName == 'create' }.constraints*.propertyName == ['bookId']
        sellersMappings.find { it.actionName == 'save' }.constraints*.propertyName == ['bookId', 'format']
        sellersMappings.find { it.actionName == 'show' }.constraints*.propertyName == ['bookId', 'id', 'format']
        sellersMappings.find { it.actionName == 'edit' }.constraints*.propertyName == ['bookId', 'id']
        sellersMappings.find { it.actionName == 'update' }.constraints*.propertyName == ['bookId', 'id', 'format']
        sellersMappings.find { it.actionName == 'patch' }.constraints*.propertyName == ['bookId', 'id', 'format']
        sellersMappings.find { it.actionName == 'delete' }.constraints*.propertyName == ['bookId', 'id', 'format']

        and: 'the location mappings have the expected constrained properties'
        locationsMappings.find { it.actionName == 'index' }.constraints*.propertyName == ['bookId', 'sellerId', 'format']
        locationsMappings.find { it.actionName == 'create' }.constraints*.propertyName == ['bookId', 'sellerId']
        locationsMappings.find { it.actionName == 'save' }.constraints*.propertyName == ['bookId', 'sellerId', 'format']
        locationsMappings.find { it.actionName == 'show' }.constraints*.propertyName == ['bookId', 'sellerId', 'id', 'format']
        locationsMappings.find { it.actionName == 'edit' }.constraints*.propertyName == ['bookId', 'sellerId', 'id']
        locationsMappings.find { it.actionName == 'update' }.constraints*.propertyName == ['bookId', 'sellerId', 'id', 'format']
        locationsMappings.find { it.actionName == 'patch' }.constraints*.propertyName == ['bookId', 'sellerId', 'id', 'format']
        locationsMappings.find { it.actionName == 'delete' }.constraints*.propertyName == ['bookId', 'sellerId', 'id', 'format']
    }
    
    void "Test that URL mappings with resources 3 levels deep works"() {
        given:"A resources definition with nested URL mappings"
        def urlMappingsHolder = getUrlMappingsHolder {
            "/books"(resources: "book") {
                '/authors'(resources:'author') {
                    "/publisher"(resource:"publisher")
                }

            }
        }

        when:"The URL mappings are obtained"
            def urlMappings = urlMappingsHolder.urlMappings

        then:"There are correct number of them in total"
            urlMappings.size() == 23

        expect:
            urlMappingsHolder.matchAll('/books/1/authors/create', 'GET')
            urlMappingsHolder.matchAll('/books/1/authors/create', 'GET')[0].actionName == 'create'
            urlMappingsHolder.matchAll('/books/1/authors/create', 'GET')[0].httpMethod == 'GET'


    }

    void "Test that normal URL mappings can be nested within resources"() {
        given:"A resources definition with nested URL mappings"
            def urlMappingsHolder = getUrlMappingsHolder {
                "/books"(resources: "book") {
                    "/publisher"(controller:"publisher")
                }
            }

        when:"The URL mappings are obtained"
            def urlMappings = urlMappingsHolder.urlMappings

        then:"There are the correct number of them in total"
            urlMappings.size() == 9

        expect:"That the appropriate URLs are matched for the appropriate HTTP methods"
            urlMappingsHolder.allowedMethods('/books') == [POST, GET] as Set
            urlMappingsHolder.allowedMethods('/books/1') == [GET, DELETE, PUT, PATCH] as Set
            urlMappingsHolder.matchAll('/books', 'GET')
            urlMappingsHolder.matchAll('/books', 'GET')[0].actionName == 'index'
            urlMappingsHolder.matchAll('/books', 'GET')[0].httpMethod == 'GET'

            !urlMappingsHolder.matchAll('/publisher', 'GET')
            urlMappingsHolder.matchAll('/books/publisher', 'GET')
            urlMappingsHolder.matchAll('/books/create', 'GET')
            urlMappingsHolder.matchAll('/books/create', 'GET')[0].actionName == 'create'
            urlMappingsHolder.matchAll('/books/create', 'GET')[0].httpMethod == 'GET'

            urlMappingsHolder.matchAll('/books/1/edit', 'GET')
            urlMappingsHolder.matchAll('/books/1/edit', 'GET')[0].actionName == 'edit'
            urlMappingsHolder.matchAll('/books/1/edit', 'GET')[0].httpMethod == 'GET'
            !urlMappingsHolder.matchAll('/books/1/edit', 'POST')
            !urlMappingsHolder.matchAll('/books/1/edit', 'PUT')
            !urlMappingsHolder.matchAll('/books/1/edit', 'PATCH')
            !urlMappingsHolder.matchAll('/books/1/edit', 'DELETE')

            urlMappingsHolder.matchAll('/books', 'POST')
            urlMappingsHolder.matchAll('/books', 'POST')[0].actionName == 'save'
            urlMappingsHolder.matchAll('/books', 'POST')[0].httpMethod == 'POST'

            urlMappingsHolder.matchAll('/books/1', 'PUT')
            urlMappingsHolder.matchAll('/books/1', 'PUT')[0].actionName == 'update'
            urlMappingsHolder.matchAll('/books/1', 'PUT')[0].httpMethod == 'PUT'

            urlMappingsHolder.matchAll('/books/1', 'PATCH')
            urlMappingsHolder.matchAll('/books/1', 'PATCH')[0].actionName == 'patch'
            urlMappingsHolder.matchAll('/books/1', 'PATCH')[0].httpMethod == 'PATCH'

            urlMappingsHolder.matchAll('/books/1', 'DELETE')
            urlMappingsHolder.matchAll('/books/1', 'DELETE')[0].actionName == 'delete'
            urlMappingsHolder.matchAll('/books/1', 'DELETE')[0].httpMethod == 'DELETE'

    }

    void "Test nested resource within another resource produce the correct URL mappings"() {
        given:"A URL mappings definition with nested resources"
        def urlMappingsHolder = getUrlMappingsHolder {
            "/books"(resources: "book") {
                "/authors"(resources:"author")
            }
        }
        when:"The URLs are obtained"
            def urlMappings = urlMappingsHolder.urlMappings

        then:"There are the correct number of them in total"
            urlMappings.size() == 16

        expect:"That the appropriate URLs are matched for the appropriate HTTP methods"
            !urlMappingsHolder.matchAll('/author/create', 'GET')
            !urlMappingsHolder.matchAll('/author/edit', 'GET')
            !urlMappingsHolder.matchAll('/author', 'POST')
            !urlMappingsHolder.matchAll('/author', 'PUT')
            !urlMappingsHolder.matchAll('/author', 'PATCH')
            !urlMappingsHolder.matchAll('/author', 'DELETE')
            !urlMappingsHolder.matchAll('/author', 'GET')
            urlMappingsHolder.matchAll('/books/create', 'GET')
            urlMappingsHolder.matchAll('/books/create', 'GET')[0].actionName == 'create'
            urlMappingsHolder.matchAll('/books/create', 'GET')[0].httpMethod == 'GET'

            urlMappingsHolder.matchAll('/books/1/edit', 'GET')
            urlMappingsHolder.matchAll('/books/1/edit', 'GET')[0].actionName == 'edit'
            urlMappingsHolder.matchAll('/books/1/edit', 'GET')[0].httpMethod == 'GET'
            !urlMappingsHolder.matchAll('/books/1/edit', 'POST')
            !urlMappingsHolder.matchAll('/books/1/edit', 'PUT')
            !urlMappingsHolder.matchAll('/books/1/edit', 'PATCH')
            !urlMappingsHolder.matchAll('/books/1/edit', 'DELETE')

            urlMappingsHolder.matchAll('/books', 'POST')
            urlMappingsHolder.matchAll('/books', 'POST')[0].actionName == 'save'
            urlMappingsHolder.matchAll('/books', 'POST')[0].httpMethod == 'POST'

            urlMappingsHolder.matchAll('/books/1', 'PUT')
            urlMappingsHolder.matchAll('/books/1', 'PUT')[0].actionName == 'update'
            urlMappingsHolder.matchAll('/books/1', 'PUT')[0].httpMethod == 'PUT'

            urlMappingsHolder.matchAll('/books/1', 'PATCH')
            urlMappingsHolder.matchAll('/books/1', 'PATCH')[0].actionName == 'patch'
            urlMappingsHolder.matchAll('/books/1', 'PATCH')[0].httpMethod == 'PATCH'

            urlMappingsHolder.matchAll('/books/1', 'DELETE')
            urlMappingsHolder.matchAll('/books/1', 'DELETE')[0].actionName == 'delete'
            urlMappingsHolder.matchAll('/books/1', 'DELETE')[0].httpMethod == 'DELETE'

            urlMappingsHolder.matchAll('/books', 'GET')
            urlMappingsHolder.matchAll('/books', 'GET')[0].actionName == 'index'
            urlMappingsHolder.matchAll('/books', 'GET')[0].httpMethod == 'GET'

            urlMappingsHolder.matchAll('/books/1/authors/create', 'GET')
            urlMappingsHolder.matchAll('/books/1/authors/create', 'GET')[0].actionName == 'create'
            urlMappingsHolder.matchAll('/books/1/authors/create', 'GET')[0].httpMethod == 'GET'

            urlMappingsHolder.matchAll('/books/1/authors/1/edit', 'GET')
            urlMappingsHolder.matchAll('/books/1/authors/1/edit', 'GET')[0].actionName == 'edit'
            urlMappingsHolder.matchAll('/books/1/authors/1/edit', 'GET')[0].httpMethod == 'GET'
            !urlMappingsHolder.matchAll('/books/1/authors/1/edit', 'POST')
            !urlMappingsHolder.matchAll('/books/1/authors/1/edit', 'PUT')
            !urlMappingsHolder.matchAll('/books/1/authors/1/edit', 'PATCH')
            !urlMappingsHolder.matchAll('/books/1/authors/1/edit', 'DELETE')

            urlMappingsHolder.matchAll('/books/1/authors', 'POST')
            urlMappingsHolder.matchAll('/books/1/authors', 'POST')[0].actionName == 'save'
            urlMappingsHolder.matchAll('/books/1/authors', 'POST')[0].httpMethod == 'POST'

            urlMappingsHolder.matchAll('/books/1/authors/1', 'PUT')
            urlMappingsHolder.matchAll('/books/1/authors/1', 'PUT')[0].actionName == 'update'
            urlMappingsHolder.matchAll('/books/1/authors/1', 'PUT')[0].httpMethod == 'PUT'

            urlMappingsHolder.matchAll('/books/1/authors/1', 'PATCH')
            urlMappingsHolder.matchAll('/books/1/authors/1', 'PATCH')[0].actionName == 'patch'
            urlMappingsHolder.matchAll('/books/1/authors/1', 'PATCH')[0].httpMethod == 'PATCH'

            urlMappingsHolder.matchAll('/books/1/authors/1', 'DELETE')
            urlMappingsHolder.matchAll('/books/1/authors/1', 'DELETE')[0].actionName == 'delete'
            urlMappingsHolder.matchAll('/books/1/authors/1', 'DELETE')[0].httpMethod == 'DELETE'

            urlMappingsHolder.matchAll('/books/1/authors/1', 'GET')
            urlMappingsHolder.matchAll('/books/1/authors/1', 'GET')[0].actionName == 'show'
            urlMappingsHolder.matchAll('/books/1/authors/1', 'GET')[0].httpMethod == 'GET'
    }

    void "Test nested single resource within another resource produce the correct URL mappings"() {
        given:"A URL mappings definition with nested resources"
            def urlMappingsHolder = getUrlMappingsHolder {
                "/books"(resources: "book") {
                    "/author"(resource:"author")
                }
            }
        when:"The URLs are obtained"
            def urlMappings = urlMappingsHolder.urlMappings

        then:"There are the correct number of them in total"
            urlMappings.size() == 15

        expect:"That the appropriate URLs are matched for the appropriate HTTP methods"
            !urlMappingsHolder.matchAll('/author/create', 'GET')
            !urlMappingsHolder.matchAll('/author/edit', 'GET')
            !urlMappingsHolder.matchAll('/author', 'POST')
            !urlMappingsHolder.matchAll('/author', 'PUT')
            !urlMappingsHolder.matchAll('/author', 'PATCH')
            !urlMappingsHolder.matchAll('/author', 'DELETE')
            !urlMappingsHolder.matchAll('/author', 'GET')
            urlMappingsHolder.matchAll('/books/create', 'GET')
            urlMappingsHolder.matchAll('/books/create', 'GET')[0].actionName == 'create'
            urlMappingsHolder.matchAll('/books/create', 'GET')[0].httpMethod == 'GET'

            urlMappingsHolder.matchAll('/books/1/edit', 'GET')
            urlMappingsHolder.matchAll('/books/1/edit', 'GET')[0].actionName == 'edit'
            urlMappingsHolder.matchAll('/books/1/edit', 'GET')[0].httpMethod == 'GET'
            !urlMappingsHolder.matchAll('/books/1/edit', 'POST')
            !urlMappingsHolder.matchAll('/books/1/edit', 'PUT')
            !urlMappingsHolder.matchAll('/books/1/edit', 'PATCH')
            !urlMappingsHolder.matchAll('/books/1/edit', 'DELETE')

            urlMappingsHolder.matchAll('/books', 'POST')
            urlMappingsHolder.matchAll('/books', 'POST')[0].actionName == 'save'
            urlMappingsHolder.matchAll('/books', 'POST')[0].httpMethod == 'POST'

            urlMappingsHolder.matchAll('/books/1', 'PUT')
            urlMappingsHolder.matchAll('/books/1', 'PUT')[0].actionName == 'update'
            urlMappingsHolder.matchAll('/books/1', 'PUT')[0].httpMethod == 'PUT'

            urlMappingsHolder.matchAll('/books/1', 'PATCH')
            urlMappingsHolder.matchAll('/books/1', 'PATCH')[0].actionName == 'patch'
            urlMappingsHolder.matchAll('/books/1', 'PATCH')[0].httpMethod == 'PATCH'

            urlMappingsHolder.matchAll('/books/1', 'DELETE')
            urlMappingsHolder.matchAll('/books/1', 'DELETE')[0].actionName == 'delete'
            urlMappingsHolder.matchAll('/books/1', 'DELETE')[0].httpMethod == 'DELETE'

            urlMappingsHolder.matchAll('/books', 'GET')
            urlMappingsHolder.matchAll('/books', 'GET')[0].actionName == 'index'
            urlMappingsHolder.matchAll('/books', 'GET')[0].httpMethod == 'GET'

            urlMappingsHolder.matchAll('/books/1/author/create', 'GET')
            urlMappingsHolder.matchAll('/books/1/author/create', 'GET')[0].actionName == 'create'
            urlMappingsHolder.matchAll('/books/1/author/create', 'GET')[0].httpMethod == 'GET'

            !urlMappingsHolder.matchAll('/books/1/author/create', 'POST')
            !urlMappingsHolder.matchAll('/books/1/author/create', 'PUT')
            !urlMappingsHolder.matchAll('/books/1/author/create', 'PATCH')
            !urlMappingsHolder.matchAll('/books/1/author/create', 'DELETE')

            urlMappingsHolder.matchAll('/books/1/author/edit', 'GET')
            urlMappingsHolder.matchAll('/books/1/author/edit', 'GET')[0].actionName == 'edit'
            urlMappingsHolder.matchAll('/books/1/author/edit', 'GET')[0].httpMethod == 'GET'
            !urlMappingsHolder.matchAll('/books/1/author/edit', 'POST')
            !urlMappingsHolder.matchAll('/books/1/author/edit', 'PUT')
            !urlMappingsHolder.matchAll('/books/1/author/edit', 'PATCH')
            !urlMappingsHolder.matchAll('/books/1/author/edit', 'DELETE')

            urlMappingsHolder.matchAll('/books/1/author', 'POST')
            urlMappingsHolder.matchAll('/books/1/author', 'POST')[0].actionName == 'save'
            urlMappingsHolder.matchAll('/books/1/author', 'POST')[0].httpMethod == 'POST'

            urlMappingsHolder.matchAll('/books/1/author', 'PUT')
            urlMappingsHolder.matchAll('/books/1/author', 'PUT')[0].actionName == 'update'
            urlMappingsHolder.matchAll('/books/1/author', 'PUT')[0].httpMethod == 'PUT'

            urlMappingsHolder.matchAll('/books/1/author', 'PATCH')
            urlMappingsHolder.matchAll('/books/1/author', 'PATCH')[0].actionName == 'patch'
            urlMappingsHolder.matchAll('/books/1/author', 'PATCH')[0].httpMethod == 'PATCH'

            urlMappingsHolder.matchAll('/books/1/author', 'DELETE')
            urlMappingsHolder.matchAll('/books/1/author', 'DELETE')[0].actionName == 'delete'
            urlMappingsHolder.matchAll('/books/1/author', 'DELETE')[0].httpMethod == 'DELETE'

            urlMappingsHolder.matchAll('/books/1/author', 'GET')
            urlMappingsHolder.matchAll('/books/1/author', 'GET')[0].actionName == 'show'
            urlMappingsHolder.matchAll('/books/1/author', 'GET')[0].httpMethod == 'GET'
    }

    void "Test multiple resources produce the correct URL mappings"() {
        given:"A URL mappings definition with a single resources"
            def urlMappingsHolder = getUrlMappingsHolder {
                "/books"(resources:"book")
            }
        when:"The URLs are obtained"
            def urlMappings = urlMappingsHolder.urlMappings

        then:"There are the correct number of them in total"
            urlMappings.size() == 8

        expect:"That the appropriate URLs are matched for the appropriate HTTP methods"
            urlMappingsHolder.matchAll('/books/create', 'GET')
            urlMappingsHolder.matchAll('/books/create', 'GET')[0].actionName == 'create'
            urlMappingsHolder.matchAll('/books/create', 'GET')[0].httpMethod == 'GET'

            urlMappingsHolder.matchAll('/books/1/edit', 'GET')
            urlMappingsHolder.matchAll('/books/1/edit', 'GET')[0].actionName == 'edit'
            urlMappingsHolder.matchAll('/books/1/edit', 'GET')[0].httpMethod == 'GET'
            !urlMappingsHolder.matchAll('/books/1/edit', 'POST')
            !urlMappingsHolder.matchAll('/books/1/edit', 'PUT')
            !urlMappingsHolder.matchAll('/books/1/edit', 'PATCH')
            !urlMappingsHolder.matchAll('/books/1/edit', 'DELETE')

            urlMappingsHolder.matchAll('/books', 'POST')
            urlMappingsHolder.matchAll('/books', 'POST')[0].actionName == 'save'
            urlMappingsHolder.matchAll('/books', 'POST')[0].httpMethod == 'POST'

            urlMappingsHolder.matchAll('/books/1', 'PUT')
            urlMappingsHolder.matchAll('/books/1', 'PUT')[0].actionName == 'update'
            urlMappingsHolder.matchAll('/books/1', 'PUT')[0].httpMethod == 'PUT'

            urlMappingsHolder.matchAll('/books/1', 'PATCH')
            urlMappingsHolder.matchAll('/books/1', 'PATCH')[0].actionName == 'patch'
            urlMappingsHolder.matchAll('/books/1', 'PATCH')[0].httpMethod == 'PATCH'

            urlMappingsHolder.matchAll('/books/1', 'DELETE')
            urlMappingsHolder.matchAll('/books/1', 'DELETE')[0].actionName == 'delete'
            urlMappingsHolder.matchAll('/books/1', 'DELETE')[0].httpMethod == 'DELETE'

            urlMappingsHolder.matchAll('/books', 'GET')
            urlMappingsHolder.matchAll('/books', 'GET')[0].actionName == 'index'
            urlMappingsHolder.matchAll('/books', 'GET')[0].httpMethod == 'GET'
    }

    void "Test multiple resources with excludes produce the correct URL mappings"() {
        given:"A URL mappings definition with a single resources"
            def urlMappingsHolder = getUrlMappingsHolder {
                "/books"(resources:"book", excludes:['delete'])
            }
        when:"The URLs are obtained"
            def urlMappings = urlMappingsHolder.urlMappings

        then:"There are the correct number of them in total"
            urlMappings.size() == 7

        expect:"That the appropriate URLs are matched for the appropriate HTTP methods"

            !urlMappingsHolder.matchAll('/books/1', 'DELETE')
            urlMappingsHolder.matchAll('/books/create', 'GET')
            urlMappingsHolder.matchAll('/books/create', 'GET')[0].actionName == 'create'
            urlMappingsHolder.matchAll('/books/create', 'GET')[0].httpMethod == 'GET'

            urlMappingsHolder.matchAll('/books/1/edit', 'GET')
            urlMappingsHolder.matchAll('/books/1/edit', 'GET')[0].actionName == 'edit'
            urlMappingsHolder.matchAll('/books/1/edit', 'GET')[0].httpMethod == 'GET'
            !urlMappingsHolder.matchAll('/books/1/edit', 'POST')
            !urlMappingsHolder.matchAll('/books/1/edit', 'PUT')
            !urlMappingsHolder.matchAll('/books/1/edit', 'PATCH')
            !urlMappingsHolder.matchAll('/books/1/edit', 'DELETE')

            urlMappingsHolder.matchAll('/books', 'POST')
            urlMappingsHolder.matchAll('/books', 'POST')[0].actionName == 'save'
            urlMappingsHolder.matchAll('/books', 'POST')[0].httpMethod == 'POST'

            urlMappingsHolder.matchAll('/books/1', 'PUT')
            urlMappingsHolder.matchAll('/books/1', 'PUT')[0].actionName == 'update'
            urlMappingsHolder.matchAll('/books/1', 'PUT')[0].httpMethod == 'PUT'

            urlMappingsHolder.matchAll('/books/1', 'PATCH')
            urlMappingsHolder.matchAll('/books/1', 'PATCH')[0].actionName == 'patch'
            urlMappingsHolder.matchAll('/books/1', 'PATCH')[0].httpMethod == 'PATCH'

            urlMappingsHolder.matchAll('/books', 'GET')
            urlMappingsHolder.matchAll('/books', 'GET')[0].actionName == 'index'
            urlMappingsHolder.matchAll('/books', 'GET')[0].httpMethod == 'GET'
    }

    void "Test multiple resources with includes produce the correct URL mappings"() {
        given:"A URL mappings definition with a single resources"
            def urlMappingsHolder = getUrlMappingsHolder {
                "/books"(resources:"book", includes:['save', 'update', 'index'])
            }
        when:"The URLs are obtained"
            def urlMappings = urlMappingsHolder.urlMappings

        then:"There are seven of them in total"
            urlMappings.size() == 3

        expect:"That the appropriate URLs are matched for the appropriate HTTP methods"

            !urlMappingsHolder.matchAll('/books/1', 'DELETE')
            !urlMappingsHolder.matchAll('/books/create', 'GET')
            !urlMappingsHolder.matchAll('/books/1/edit', 'GET')

            urlMappingsHolder.matchAll('/books', 'POST')
            urlMappingsHolder.matchAll('/books', 'POST')[0].actionName == 'save'
            urlMappingsHolder.matchAll('/books', 'POST')[0].httpMethod == 'POST'

            urlMappingsHolder.matchAll('/books/1', 'PUT')
            urlMappingsHolder.matchAll('/books/1', 'PUT')[0].actionName == 'update'
            urlMappingsHolder.matchAll('/books/1', 'PUT')[0].httpMethod == 'PUT'

            urlMappingsHolder.matchAll('/books', 'GET')
            urlMappingsHolder.matchAll('/books', 'GET')[0].actionName == 'index'
            urlMappingsHolder.matchAll('/books', 'GET')[0].httpMethod == 'GET'
    }

    void "Test a single resource within a namespace produces the correct URL mappings"() {
        given:"A URL mappings definition with a single resource"
        def urlMappingsHolder = getUrlMappingsHolder {
            group "/admin", {
                "/book"(resource:"book")
            }
        }

        when:"The URLs are obtained"
        def urlMappings = urlMappingsHolder.urlMappings

        then:"There are the correct number of them in total"
        urlMappings.size() == 7

        expect:"That the appropriate URLs are matched for the appropriate HTTP methods"
        urlMappingsHolder.matchAll('/admin/book/create', 'GET')
        urlMappingsHolder.matchAll('/admin/book/create', 'GET')[0].actionName == 'create'
        urlMappingsHolder.matchAll('/admin/book/create', 'GET')[0].httpMethod == 'GET'

        !urlMappingsHolder.matchAll('/admin/book/create', 'POST')
        !urlMappingsHolder.matchAll('/admin/book/create', 'PUT')
        !urlMappingsHolder.matchAll('/admin/book/create', 'PATCH')
        !urlMappingsHolder.matchAll('/admin/book/create', 'DELETE')

        urlMappingsHolder.matchAll('/admin/book/edit', 'GET')
        urlMappingsHolder.matchAll('/admin/book/edit', 'GET')[0].actionName == 'edit'
        urlMappingsHolder.matchAll('/admin/book/edit', 'GET')[0].httpMethod == 'GET'
        !urlMappingsHolder.matchAll('/admin/book/edit', 'POST')
        !urlMappingsHolder.matchAll('/admin/book/edit', 'PUT')
        !urlMappingsHolder.matchAll('/admin/book/edit', 'PATCH')
        !urlMappingsHolder.matchAll('/admin/book/edit', 'DELETE')

        urlMappingsHolder.matchAll('/admin/book', 'POST')
        urlMappingsHolder.matchAll('/admin/book', 'POST')[0].actionName == 'save'
        urlMappingsHolder.matchAll('/admin/book', 'POST')[0].httpMethod == 'POST'

        urlMappingsHolder.matchAll('/admin/book', 'PUT')
        urlMappingsHolder.matchAll('/admin/book', 'PUT')[0].actionName == 'update'
        urlMappingsHolder.matchAll('/admin/book', 'PUT')[0].httpMethod == 'PUT'

        urlMappingsHolder.matchAll('/admin/book', 'PATCH')
        urlMappingsHolder.matchAll('/admin/book', 'PATCH')[0].actionName == 'patch'
        urlMappingsHolder.matchAll('/admin/book', 'PATCH')[0].httpMethod == 'PATCH'

        urlMappingsHolder.matchAll('/admin/book', 'DELETE')
        urlMappingsHolder.matchAll('/admin/book', 'DELETE')[0].actionName == 'delete'
        urlMappingsHolder.matchAll('/admin/book', 'DELETE')[0].httpMethod == 'DELETE'

        urlMappingsHolder.matchAll('/admin/book', 'GET')
        urlMappingsHolder.matchAll('/admin/book', 'GET')[0].actionName == 'show'
        urlMappingsHolder.matchAll('/admin/book', 'GET')[0].httpMethod == 'GET'
    }

    void "Test a single resource produces the correct URL mappings"() {
        given:"A URL mappings definition with a single resource"
            def urlMappingsHolder = getUrlMappingsHolder {
                "/book"(resource:"book")
            }

        when:"The URLs are obtained"
            def urlMappings = urlMappingsHolder.urlMappings

        then:"There are the correct number of them in total"
            urlMappings.size() == 7

        expect:"That the appropriate URLs are matched for the appropriate HTTP methods"
            urlMappingsHolder.matchAll('/book/create', 'GET')
            urlMappingsHolder.matchAll('/book/create', 'GET')[0].actionName == 'create'
            urlMappingsHolder.matchAll('/book/create', 'GET')[0].httpMethod == 'GET'

            !urlMappingsHolder.matchAll('/book/create', 'POST')
            !urlMappingsHolder.matchAll('/book/create', 'PUT')
            !urlMappingsHolder.matchAll('/book/create', 'PATCH')
            !urlMappingsHolder.matchAll('/book/create', 'DELETE')

            urlMappingsHolder.matchAll('/book/edit', 'GET')
            urlMappingsHolder.matchAll('/book/edit', 'GET')[0].actionName == 'edit'
            urlMappingsHolder.matchAll('/book/edit', 'GET')[0].httpMethod == 'GET'
            !urlMappingsHolder.matchAll('/book/edit', 'POST')
            !urlMappingsHolder.matchAll('/book/edit', 'PUT')
            !urlMappingsHolder.matchAll('/book/edit', 'PATCH')
            !urlMappingsHolder.matchAll('/book/edit', 'DELETE')

            urlMappingsHolder.matchAll('/book', 'POST')
            urlMappingsHolder.matchAll('/book', 'POST')[0].actionName == 'save'
            urlMappingsHolder.matchAll('/book', 'POST')[0].httpMethod == 'POST'

            urlMappingsHolder.matchAll('/book', 'PUT')
            urlMappingsHolder.matchAll('/book', 'PUT')[0].actionName == 'update'
            urlMappingsHolder.matchAll('/book', 'PUT')[0].httpMethod == 'PUT'

            urlMappingsHolder.matchAll('/book', 'PATCH')
            urlMappingsHolder.matchAll('/book', 'PATCH')[0].actionName == 'patch'
            urlMappingsHolder.matchAll('/book', 'PATCH')[0].httpMethod == 'PATCH'

            urlMappingsHolder.matchAll('/book', 'DELETE')
            urlMappingsHolder.matchAll('/book', 'DELETE')[0].actionName == 'delete'
            urlMappingsHolder.matchAll('/book', 'DELETE')[0].httpMethod == 'DELETE'

            urlMappingsHolder.matchAll('/book', 'GET')
            urlMappingsHolder.matchAll('/book', 'GET')[0].actionName == 'show'
            urlMappingsHolder.matchAll('/book', 'GET')[0].httpMethod == 'GET'
    }

    void "Test a single resource with excludes produces the correct URL mappings"() {
        given:"A URL mappings definition with a single resource"
        def urlMappingsHolder = getUrlMappingsHolder {
            "/book"(resource:"book", excludes: "delete")
        }

        when:"The URLs are obtained"
            def urlMappings = urlMappingsHolder.urlMappings

        then:"There are correct of them in total"
            urlMappings.size() == 6

        expect:"That the appropriate URLs are matched for the appropriate HTTP methods"
            !urlMappingsHolder.matchAll('/book', 'DELETE')
            urlMappingsHolder.matchAll('/book/create', 'GET')
            urlMappingsHolder.matchAll('/book/create', 'GET')[0].actionName == 'create'
            urlMappingsHolder.matchAll('/book/create', 'GET')[0].httpMethod == 'GET'

            !urlMappingsHolder.matchAll('/book/create', 'POST')
            !urlMappingsHolder.matchAll('/book/create', 'PUT')
            !urlMappingsHolder.matchAll('/book/create', 'PATCH')
            !urlMappingsHolder.matchAll('/book/create', 'DELETE')

            urlMappingsHolder.matchAll('/book/edit', 'GET')
            urlMappingsHolder.matchAll('/book/edit', 'GET')[0].actionName == 'edit'
            urlMappingsHolder.matchAll('/book/edit', 'GET')[0].httpMethod == 'GET'
            !urlMappingsHolder.matchAll('/book/edit', 'POST')
            !urlMappingsHolder.matchAll('/book/edit', 'PUT')
            !urlMappingsHolder.matchAll('/book/edit', 'PATCH')
            !urlMappingsHolder.matchAll('/book/edit', 'DELETE')

            urlMappingsHolder.matchAll('/book', 'POST')
            urlMappingsHolder.matchAll('/book', 'POST')[0].actionName == 'save'
            urlMappingsHolder.matchAll('/book', 'POST')[0].httpMethod == 'POST'

            urlMappingsHolder.matchAll('/book', 'PUT')
            urlMappingsHolder.matchAll('/book', 'PUT')[0].actionName == 'update'
            urlMappingsHolder.matchAll('/book', 'PUT')[0].httpMethod == 'PUT'

            urlMappingsHolder.matchAll('/book', 'PATCH')
            urlMappingsHolder.matchAll('/book', 'PATCH')[0].actionName == 'patch'
            urlMappingsHolder.matchAll('/book', 'PATCH')[0].httpMethod == 'PATCH'

            urlMappingsHolder.matchAll('/book', 'GET')
            urlMappingsHolder.matchAll('/book', 'GET')[0].actionName == 'show'
            urlMappingsHolder.matchAll('/book', 'GET')[0].httpMethod == 'GET'
    }

    void "Test a single resource produces the correct links for reverse mapping"() {
        given:"A link generator definition with a single resource"
            def linkGenerator = getLinkGenerator {
                "/book"(resource:"book")
            }

        expect:"The generated links to be correct"
            linkGenerator.link(controller:"book", action:"save", method:"POST") == "http://localhost/book"
            linkGenerator.link(controller:"book", action:"show", method:"GET") == "http://localhost/book"
            linkGenerator.link(controller:"book", action:"edit", method:"GET") == "http://localhost/book/edit"
            linkGenerator.link(controller:"book", action:"delete", method:"DELETE") == "http://localhost/book"
            linkGenerator.link(controller:"book", action:"update", method:"PUT") == "http://localhost/book"
            linkGenerator.link(controller:"book", action:"patch", method:"PATCH") == "http://localhost/book"
            linkGenerator.link(controller:"book", action:"create") == "http://localhost/book/create"
            linkGenerator.link(controller:"book", action:"create", method:"GET") == "http://localhost/book/create"
    }

    void "Test a resource produces the correct links for reverse mapping"() {
        given:"A link generator definition with a single resource"
        def linkGenerator = getLinkGenerator {
            "/books"(resources:"book")
        }

        expect:"The generated links to be correct"
        linkGenerator.link(controller:"book", action:"show", id:1, method:"GET") == "http://localhost/books/1"
            linkGenerator.link(controller:"book", action:"create", method:"GET") == "http://localhost/books/create"
            linkGenerator.link(controller:"book", action:"save", method:"POST") == "http://localhost/books"
            linkGenerator.link(controller:"book", action:"edit", id:1, method:"GET") == "http://localhost/books/1/edit"
            linkGenerator.link(controller:"book", action:"delete", id:1, method:"DELETE") == "http://localhost/books/1"
            linkGenerator.link(controller:"book", action:"update", id:1, method:"PUT") == "http://localhost/books/1"
            linkGenerator.link(controller:"book", action:"patch", id:1, method:"PATCH") == "http://localhost/books/1"
    }

    void "Test it is possible to link to a single resource nested within another resource"() {
        given:"A link generator with nested resources"
            def linkGenerator = getLinkGenerator {
                "/books"(resources: "book") {
                    "/author"(resource:"author")
                }
            }

        expect:"The generated links to be correct"

            linkGenerator.link(resource:"book/author", action:"create", bookId:1) == "http://localhost/books/1/author/create"
            linkGenerator.link(resource:"book/author", action:"save", bookId:1) == "http://localhost/books/1/author"
            linkGenerator.link(resource:"book/author", action:"show", bookId:1) == "http://localhost/books/1/author"
            linkGenerator.link(resource:"book/author", action:"edit", bookId:1) == "http://localhost/books/1/author/edit"
            linkGenerator.link(resource:"book/author", action:"delete", bookId:1) == "http://localhost/books/1/author"
            linkGenerator.link(resource:"book/author", action:"update", bookId:1) == "http://localhost/books/1/author"
            linkGenerator.link(resource:"book/author", action:"patch", bookId:1) == "http://localhost/books/1/author"

            linkGenerator.link(controller:"author", action:"create", method:"GET", params:[bookId:1]) == "http://localhost/books/1/author/create"
            linkGenerator.link(controller:"author", action:"save", method:"POST", params:[bookId:1]) == "http://localhost/books/1/author"
            linkGenerator.link(controller:"author", action:"show", method:"GET", params:[bookId:1]) == "http://localhost/books/1/author"
            linkGenerator.link(controller:"author", action:"edit", method:"GET", params:[bookId:1]) == "http://localhost/books/1/author/edit"
            linkGenerator.link(controller:"author", action:"delete", method:"DELETE", params:[bookId:1]) == "http://localhost/books/1/author"
            linkGenerator.link(controller:"author", action:"update", method:"PUT", params:[bookId:1]) == "http://localhost/books/1/author"
            linkGenerator.link(controller:"author", action:"patch", method:"PATCH", params:[bookId:1]) == "http://localhost/books/1/author"

            linkGenerator.link(controller:"book", action:"create", method:"GET") == "http://localhost/books/create"
            linkGenerator.link(controller:"book", action:"save", method:"POST") == "http://localhost/books"
            linkGenerator.link(controller:"book", action:"show", id:1, method:"GET") == "http://localhost/books/1"
            linkGenerator.link(controller:"book", action:"edit", id:1, method:"GET") == "http://localhost/books/1/edit"
            linkGenerator.link(controller:"book", action:"delete", id:1, method:"DELETE") == "http://localhost/books/1"
            linkGenerator.link(controller:"book", action:"update", id:1, method:"PUT") == "http://localhost/books/1"
            linkGenerator.link(controller:"book", action:"patch", id:1, method:"PATCH") == "http://localhost/books/1"

    }


    void "Test it is possible to link to a resource nested within another resource"() {
        given:"A link generator with nested resources"
        def linkGenerator = getLinkGenerator {
            "/books"(resources: "book") {
                "/authors"(resources:"author")
            }
        }

        expect:"The generated links to be correct"

        linkGenerator.link(resource:"book/author", action:"create", bookId:1) == "http://localhost/books/1/authors/create"
        linkGenerator.link(resource:"book/author", action:"save", bookId:1) == "http://localhost/books/1/authors"
        linkGenerator.link(resource:"book/author", action:"show", id:1,bookId:1) == "http://localhost/books/1/authors/1"
        linkGenerator.link(resource:"book/author", action:"edit", id:1,bookId:1) == "http://localhost/books/1/authors/1/edit"
        linkGenerator.link(resource:"book/author", action:"delete", id:1,bookId:1) == "http://localhost/books/1/authors/1"
        linkGenerator.link(resource:"book/author", action:"update", id:1,bookId:1) == "http://localhost/books/1/authors/1"
        linkGenerator.link(resource:"book/author", action:"patch", id:1,bookId:1) == "http://localhost/books/1/authors/1"

        linkGenerator.link(controller:"author", action:"create", method:"GET", params:[bookId:1]) == "http://localhost/books/1/authors/create"
        linkGenerator.link(controller:"author", action:"save", method:"POST", params:[bookId:1]) == "http://localhost/books/1/authors"
        linkGenerator.link(controller:"author", action:"show", id:1,method:"GET", params:[bookId:1]) == "http://localhost/books/1/authors/1"
        linkGenerator.link(controller:"author", action:"edit", id:1,method:"GET", params:[bookId:1]) == "http://localhost/books/1/authors/1/edit"
        linkGenerator.link(controller:"author", action:"delete", id:1,method:"DELETE", params:[bookId:1]) == "http://localhost/books/1/authors/1"
        linkGenerator.link(controller:"author", action:"update", id:1,method:"PUT", params:[bookId:1]) == "http://localhost/books/1/authors/1"
        linkGenerator.link(controller:"author", action:"patch", id:1,method:"PATCH", params:[bookId:1]) == "http://localhost/books/1/authors/1"

        linkGenerator.link(controller:"book", action:"create", method:"GET") == "http://localhost/books/create"
        linkGenerator.link(controller:"book", action:"save", method:"POST") == "http://localhost/books"
        linkGenerator.link(controller:"book", action:"show", id:1, method:"GET") == "http://localhost/books/1"
        linkGenerator.link(controller:"book", action:"edit", id:1, method:"GET") == "http://localhost/books/1/edit"
        linkGenerator.link(controller:"book", action:"delete", id:1, method:"DELETE") == "http://localhost/books/1"
        linkGenerator.link(controller:"book", action:"update", id:1, method:"PUT") == "http://localhost/books/1"
        linkGenerator.link(controller:"book", action:"patch", id:1, method:"PATCH") == "http://localhost/books/1"

    }


    void "Test it is possible to link to a regular URL mapping nested within another resource"() {
        given:"A link generator with nested resources"
        def linkGenerator = getLinkGenerator {
            "/books"(resources: "book") {
                "/publisher"(controller:"publisher")
            }
        }

        expect:"The generated links to be correct"

        linkGenerator.link(controller:"publisher", params:[bookId:1]) == "http://localhost/books/1/publisher"
        linkGenerator.link(resource:"book/publisher", method:"GET", bookId:1) == "http://localhost/books/1/publisher"
        linkGenerator.link(resource:"book/publisher", bookId:1) == "http://localhost/books/1/publisher"



        linkGenerator.link(controller:"book", action:"create", method:"GET") == "http://localhost/books/create"
        linkGenerator.link(controller:"book", action:"save", method:"POST") == "http://localhost/books"
        linkGenerator.link(controller:"book", action:"show", id:1, method:"GET") == "http://localhost/books/1"
        linkGenerator.link(controller:"book", action:"edit", id:1, method:"GET") == "http://localhost/books/1/edit"
        linkGenerator.link(controller:"book", action:"delete", id:1, method:"DELETE") == "http://localhost/books/1"
        linkGenerator.link(controller:"book", action:"update", id:1, method:"PUT") == "http://localhost/books/1"
        linkGenerator.link(controller:"book", action:"patch", id:1, method:"PATCH") == "http://localhost/books/1"

    }

    LinkGenerator getLinkGenerator(Closure mappings) {
        def generator = new DefaultLinkGenerator("http://localhost", null)
        generator.grailsUrlConverter = new CamelCaseUrlConverter()
        generator.urlMappingsHolder = getUrlMappingsHolder mappings
        return generator;
    }
    UrlMappingsHolder getUrlMappingsHolder(Closure mappings) {
        def evaluator = new DefaultUrlMappingEvaluator(new MockServletContext())
        def allMappings = evaluator.evaluateMappings mappings
        return new DefaultUrlMappingsHolder(allMappings)
    }
}
