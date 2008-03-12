package org.codehaus.groovy.grails.orm.hibernate
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
class Face {
    Long id
    Long version
    Nose nose
}

class Nose {
    Long id
    Long version
    Face face
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

        face = faceClass.get(1)
        nose = noseClass.get(1)
        assert face.nose
        assert nose.face
                                       
    }
}