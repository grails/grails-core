package grails.test.mixin

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.rest.RestfulController
import spock.lang.Specification

@TestFor(ArtistController)
@Mock(Album)
class RestfulControllerSubclassSpec extends Specification {

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
