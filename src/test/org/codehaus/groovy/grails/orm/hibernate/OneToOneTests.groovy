package org.codehaus.groovy.grails.orm.hibernate

import org.springframework.dao.DataIntegrityViolationException

/**
 * Created by IntelliJ IDEA.                                        
 * User: grocher
 * Date: Jan 21, 2008
 * Time: 10:43:34 PM
 * To change this template use File | Settings | File Templates.
 */
class OneToOneTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class Face {
    Nose nose

    static mapping = {
        nose unique:true
    }
}

@Entity
class Nose {
    static belongsTo = [face: Face]
}
'''
    }

    

    void testPersistAssociation() {
        def faceClass = ga.getDomainClass("Face").clazz
        def noseClass = ga.getDomainClass("Nose").clazz

        def nose = noseClass.newInstance()
        def face = faceClass.newInstance(nose:nose)

        assert face.nose
        assert nose.face

        println face.nose
        println nose.face

        assert face.save(flush:true)

        session.clear()

        def newFace = faceClass.get(1)
        def newNose = noseClass.get(1)

        println newFace.nose
        println newNose.face
        assert newFace.nose
        assert newNose.face

        def differentFace = faceClass.newInstance(nose:newNose)

        shouldFail(DataIntegrityViolationException) {
            differentFace.save(flush:true)
        }
                                       
    }

    void testOneToOneTableStructure() {
        def conn = session.connection()

        conn.prepareStatement("select nose_id from face").execute()

        shouldFail {
            conn.prepareStatement("select face_id from face").execute()
        }

        // only the owner should have the foreign key
        shouldFail {
            conn.prepareStatement("select face_id from nose").execute()    
        }
    }
}