package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

/**
 * Tests that bidirectional one-to-one associations cascade correctly
 */
class BidirectionalOneToOneCascadeSpec extends GormSpec{

    void "Test that child is saved correctly when associating only the owning side"() {
        when:"An owner is saved by the inverse child is not associated"
            BidirectionalOneToOneCascadeFace face = new BidirectionalOneToOneCascadeFace()
            BidirectionalOneToOneCascadeNose nose = new BidirectionalOneToOneCascadeNose()
            face.nose = nose
            face.save(flush:true)
            session.clear()

            face = BidirectionalOneToOneCascadeFace.get(1)

        then:"Both sides are correctly associated"
            face.nose != null
            face.nose.face != null
    }
    @Override
    List getDomainClasses() {
        [BidirectionalOneToOneCascadeFace, BidirectionalOneToOneCascadeNose]
    }
}

@Entity
class BidirectionalOneToOneCascadeFace {
    static hasOne = [nose: BidirectionalOneToOneCascadeNose]
}

@Entity
class BidirectionalOneToOneCascadeNose {
    BidirectionalOneToOneCascadeFace face
}
