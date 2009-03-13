package org.grails.samples

import grails.persistence.Entity;

/**
 * @author Graeme Rocher
 */

@Entity
class PetType  {
	String name

    String toString() { name }


}
