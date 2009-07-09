package org.codehaus.groovy.grails.orm.hibernate

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Apr 8, 2008
 */

@Entity
class ManyEntity {

    @Id
    @GeneratedValue(strategy = javax.persistence.GenerationType.IDENTITY)
    Long id    
}