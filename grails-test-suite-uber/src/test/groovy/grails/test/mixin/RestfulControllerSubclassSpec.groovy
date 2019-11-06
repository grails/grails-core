package grails.test.mixin

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.rest.RestfulController
import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Issue
import spock.lang.Specification

class RestfulControllerSubclassSpec extends Specification implements ControllerUnitTest<ArtistController>, DomainUnitTest<Album> {

    void 'Test that save populates the newly created instance with values from the request body'() {
        when:
        request.method = 'POST'
        request.json = '{"title":"Red","artist":"King Crimson"}'
        controller.save()
        def album = model.album
        
        then:
        album instanceof Album
        album.title == 'Red'
        album.artist == 'King Crimson'
    }

    void 'Test that save populates the newly created instance with request parameters'() {
        when:
        request.method = 'POST'
        params.title = 'Discipline'
        params.artist = 'King Crimson'
        controller.save()
        def album = model.album
        
        then:
        album instanceof Album
        album.title == 'Discipline'
        album.artist == 'King Crimson'
    }

    void 'Test that create populates the newly created instance with values from the request body'() {
        when:
        request.method = 'POST'
        request.json = '{"title":"Starless And Bible Black","artist":"King Crimson"}'
        controller.create()
        def album = model.album
        
        then:
        album instanceof Album
        album.title == 'Starless And Bible Black'
        album.artist == 'King Crimson'
    }
    
    @Issue('GRAILS-11462')
    void 'Test that update populates the instance with values from the request body'() {
        given:
        def album = new Album(artist: 'Riverside', title: 'Second Life Syndrome').save()
        
        when:
        request.method = 'PUT'
        request.JSON = '{"title": "Rapid Eye Movement"}'
        params.id = album.id
        controller.update()
        def updatedAlbum = model.album
        
        then:
        updatedAlbum instanceof Album
        updatedAlbum.artist == 'Riverside'
        updatedAlbum.title == 'Rapid Eye Movement'
    }
    
    @Issue('GRAILS-11462')
    void 'Test that update populates the instance with values from the request parameters'() {
        given:
        def album = new Album(artist: 'Riverside', title: 'Second Life Syndrome').save()
        
        when:
        request.method = 'PUT'
        params.title = 'Out Of Myself'
        params.id = album.id
        controller.update()
        def updatedAlbum = model.album
        
        then:
        updatedAlbum instanceof Album
        updatedAlbum.artist == 'Riverside'
        updatedAlbum.title == 'Out Of Myself'
    }

    void 'Test that update validates input data and returns an error when validation fails'() {
        given:
        def album = new Album(artist: 'Riverside', title: 'Second Life Syndrome').save(flush: true)

        when:
        request.method = 'PUT'
        params.title = ''
        params.id = album.id
        controller.update()
        def updatedAlbum = model.album
        def persistedAlbum = Album.get(album.id)

        then:
        persistedAlbum.title == 'Second Life Syndrome'
        updatedAlbum instanceof Album
        updatedAlbum.title == null
        updatedAlbum.errors.hasFieldErrors('title')
    }
    
    void 'Test that create populates the newly created instance with request parameters'() {
        when:
        request.method = 'POST'
        params.title = 'Happy With What You Have To Be Happy With'
        params.artist = 'King Crimson'
        controller.create()
        def album = model.album
        
        then:
        album instanceof Album
        album.title == 'Happy With What You Have To Be Happy With'
        album.artist == 'King Crimson'
    }

    @Issue('GRAILS-11958')
    void 'test compiling a subclass of a subclass of RestfulController'() {
        given:
        def gcl = new GroovyClassLoader()

        when: 'compiling a subclass of a subclass of RestfulController'
        def c = gcl.parseClass('''
import grails.test.mixin.Album
import grails.rest.RestfulController
import grails.artefact.Artefact

@Artefact('Controller')
class Middle<T> extends RestfulController<T> {
    public Middle(Class c) {
        super(c)
    }
}

@Artefact('Controller')
class Bottom extends Middle<Album> {
    public Bottom() {
        super(Album)
    }
}
''')
        then: 'no compilation errors occur'
        c
    }
}

@Entity
class Album {
    String title
    String artist
}

@Artefact('Controller')
class ArtistController extends RestfulController<Album> {
    ArtistController() {
        super(Album)
    }
}
