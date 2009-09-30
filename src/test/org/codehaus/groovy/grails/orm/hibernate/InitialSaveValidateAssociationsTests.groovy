package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class InitialSaveValidateAssociationsTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Album  {
      String title
      List songs
       static hasMany = [songs:Song]
 }

@Entity
class Song  {
      String title
      Integer duration
      static belongsTo = [Album]
      static constraints = {
	     title blank:false
             duration(min:1)
    }
 }
''')
    }


    void testDontSaveInvalidAssociationWithInitialSave() {
        def Album = ga.getDomainClass("Album").clazz
        def Song = ga.getDomainClass("Song").clazz

         def  album1 = Album.newInstance(title:"Sam's Town")
         album1.addToSongs(
                   Song.newInstance(title:"Sam's Town", duration:-1)
                   )
         album1.save(flush:true)

        session.clear()

        assertEquals 0, Album.count()
        assertEquals 0, Song.count()
    }

}