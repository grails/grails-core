package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

class AssociationIdSpec extends GormSpec {

    void 'test retrieving association id'() {
        given:
        def pet = new AssociationPet(name: 'pet')
        pet.save()
        def owner = new PetOwner(pet: pet)
        owner.save()

        when:
        def petId = owner.petId

        then:
        petId != null
        petId == owner.pet.id
    }

    void 'test retrieving association id from a subclass'() {
        given:
        def pet = new AssociationPet(name: 'pet')
        pet.save()
        def owner = new PetOwnerSubclass(pet: pet)
        owner.save()

        when:
        def petId = owner.petId

        then:
        petId != null
        petId == owner.pet.id
    }

    @Override
    public List getDomainClasses() {
        [AssociationPet, PetOwner, PetOwnerSubclass]
    }

}

@Entity
class AssociationPet {
    String name
}

@Entity
class PetOwner {
    AssociationPet pet
}

@Entity
class PetOwnerSubclass extends PetOwner {
}
