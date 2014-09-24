package org.grails.web.binding

import grails.artefact.Artefact
import grails.converters.JSON
import grails.converters.XML
import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.web.JSONBuilder

import org.junit.Before
import org.junit.Test

@TestFor(UserController)
@Mock(User)
class JSONBindingToNullTests {

    @Before
    void addConfig() {
        grailsApplication.config.grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
                      xml: ['text/xml', 'application/xml'],
                      text: 'text/plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      csv: 'text/csv',
                      all: '*/*',
                      json: ['application/json','text/json'],
                      form: 'application/x-www-form-urlencoded',
                      multipartForm: 'multipart/form-data'
                    ]

    }

    @Test
    void testJsonBindingToNull() {

        def pebbles = new User(username:"pebbles", password:"letmein", firstName:"Pebbles", lastName:"Flintstone", middleName:"T", phone:"555-555-5555", email:'pebbles@flintstone.com', activationDate:new Date(), logonFailureCount:0, deactivationDate:null).save(flush:true)

        def builder = new JSONBuilder()
        request.method = 'PUT'
        request.json = builder.build { user = pebbles }
        response.format = "json"
        params.id = pebbles.id

        controller.update()

        // if any binding errors occurred this will break
        assert response.json.id == pebbles.id
    }

    @Test
    void testXmlBindingToNull() {
        def pebbles = new User(username:"pebbles", password:"letmein", firstName:"Pebbles", lastName:"Flintstone", middleName:"T", phone:"555-555-5555", email:'pebbles@flintstone.com', activationDate:new Date(), logonFailureCount:0, deactivationDate:null).save(flush:true)

        request.method = 'PUT'
        request.xml = pebbles
        params.id = pebbles.id

        controller.update()

        // if any binding errors occurred this will break
        assert response.xml.@id == pebbles.id
    }
}

@Artefact('Controller')
class UserController {
    def update() {
        println params
        if (params.id) {
            def user = User.get(params.id)
            if (user) {
                user.properties = params['user']
                if (!user.hasErrors() && user.save()) {
                    println "UPDATED!"
                    withFormat {
                        //html { render(view:"show", [user:user]) }
                        xml { render user as XML }
                        json { render user as JSON }
                    }
                } else {
                    println "ERRORS:${user.errors}"
                    withFormat {
                        //html { render(view:"update", [user:user]) }
                        xml { render user.errors as XML }
                        json { render user.errors as JSON }
                    }
                }
            } else {
                response.sendError 404
            }
        } else {
            response.sendError 400
        }
    }
}

@Entity
class User {
    String username
    String password
    String firstName
    String lastName
    String middleName
    String phone //need extension
    String email
    String activeDirectoryUsername
    Long createdBy
    Long lastUpdatedBy
    Long logonFailureCount
    boolean disabled
    boolean mustChangePassword
    boolean useActiveDirectory
    Date activationDate
    Date deactivationDate
    Date lastUpdatedDate
    Date lastAccessDate

    static constraints = {
        middleName(nullable:true)
        phone(nullable:true)
        email(nullable:true, email:true)
        activeDirectoryUsername(nullable:true)
        createdBy(nullable:true)
        lastUpdatedBy(nullable:true)
        logonFailureCount(nullable:false)
        deactivationDate(nullable:true)
        lastUpdatedDate(nullable:true)
        lastAccessDate(nullable:true)
    }
}
