package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

/**
 * Tests that bidirectional one-to-one associations cascade correctly
 */
class BidirectionalOneToOneCascadeSpec extends GormSpec{

    void "Test that child is saved correctly when associating only the owning side"() {
        when:"An owner is saved by the inverse child is not associated"
            Face face = new Face()
            Nose nose = new Nose()
            face.nose = nose
            face.save(flush:true)
            session.clear()
        
            face = Face.get(1)

        then:"Both sides are correctly associated"
            face.nose != null
            face.nose.face != null
    }
    @Override
    List getDomainClasses() {
        [Face, Nose]
    }
}

@Entity
class Face {
    static hasOne = [nose: Nose]
}

@Entity
class Nose {
    Face face
}
