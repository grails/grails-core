package grails.test.mixin

import grails.test.mixin.domain.DomainClassUnitTestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
@TestMixin([ControllerUnitTestMixin, DomainClassUnitTestMixin])
class ResourceAnnotationRestfulControllerSpec extends Specification{

    Class domainClass
    Class controllerClass
    def controller

    void setup() {
        def gcl  = new GroovyClassLoader()
        gcl.parseClass('''
import grails.persistence.*
import grails.rest.*

@Entity
@Resource(formats=['html', 'xml'], uri="/videos")
class Video {
    String title
    static constraints = {
        title blank:false
    }
}
''')
        domainClass = gcl.loadClass('Video')
        controllerClass = gcl.loadClass('VideoController')
        controller = testFor(controllerClass)
        mockDomain(domainClass)
    }

    void "Test the index action returns the correct model"() {

        when:"The index action is executed"
            controller.index()

        then:"The model is correct"
            assert !model.videoList
            assert model.videoCount == 0
    }

    void "Test the create action returns the correct model"() {
        when:"The create action is executed"
            controller.create()

        then:"The model is correctly created"
            model.video != null
    }

    void "Test the save action correctly persists an instance"() {

        when:"The save action is executed with an invalid instance"
            def video = domainClass.newInstance(title: '')
            video.validate()
            controller.save(video)

        then:"The create view is rendered again with the correct model"
            model.video != null
            view == 'create'

        when:"The save action is executed with a valid instance"
            response.reset()
            video = domainClass.newInstance(title: "Game of Thrones")
            controller.save(video)

        then:"A redirect is issued to the show action"
            response.status == 201
            domainClass.count() == 1
    }

    void "Test that the show action returns the correct model"() {
        when:"The show action is executed with a null domain"
            controller.show(null)

        then:"A 404 error is returned"
            response.status == 404

        when:"A domain instance is passed to the show action"
            def video = domainClass.newInstance(title: "Game of Thrones")
            controller.show(video)


        then:"A model is populated containing the domain instance"
            model.video == video
    }

    void "Test that the edit action returns the correct model"() {
        when:"The edit action is executed with a null domain"
            controller.edit(null)

        then:"A 404 error is returned"
            response.status == 404

        when:"A domain instance is passed to the edit action"
            def video = domainClass.newInstance(title: "Game of Thrones")
            controller.edit(video)


        then:"A model is populated containing the domain instance"
            model.video == video
    }


    void "Test the update action performs an update on a valid domain instance"() {
        when:"Update is called for a domain instance that doesn't exist"
            controller.update(null)

        then:"A 404 error is returned"
            status == 404

        when:"An invalid domain instance is passed to the update action"
            response.reset()
            def video = domainClass.newInstance(title: '')
            video.validate()
            controller.update(video)

        then:"The edit view is rendered again with the invalid instance"
            view == 'edit'
            model.video == video

        when:"A valid domain instance is passed to the update action"
            response.reset()
            video = domainClass.newInstance(title: 'Game of Thrones')
            video.validate()
            request.contentType = 'application/x-www-form-urlencoded'
            controller.update(video)
            video.discard()

        then:"A redirect is issues to the show action"
            response.status == 200
            domainClass.get(video.id).title == 'Game of Thrones'

    }

    void "Test that the delete action deletes an instance if it exists"() {
        when:"The delete action is called for a null instance"
            controller.delete(null)

        then:"A 404 is returned"
            status == 404

        when:"A domain instance is created"
            response.reset()
            def video = domainClass.newInstance(title: 'Game of Thrones').save(flush: true)

        then:"It exists"
            domainClass.count() == 1

        when:"The domain instance is passed to the delete action"
            controller.delete(video)

        then:"The instance is deleted"
            response.status == 204
            domainClass.count() == 0

    }
}
