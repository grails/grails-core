package org.grails.samples

import grails.persistence.Entity;


/**
 * Simple domain object representing a veterinarian.
 *
 * @author Graeme Rocher
 */

@Entity
class Vet extends Person {

	static hasMany = [specialities:Speciality]
    static transients = ['nrOfSpecialties', 'new']

    boolean isNew() { id == null }
    int getNrOfSpecialties() { specialities.size() }
}
