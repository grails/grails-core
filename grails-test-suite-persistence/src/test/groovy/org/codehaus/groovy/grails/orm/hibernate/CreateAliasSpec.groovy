package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

import org.hibernate.criterion.CriteriaSpecification

class CreateAliasSpec extends GormSpec {

    @Override
    public List getDomainClasses() {
        [Pirate, Ship]
    }

    void 'Test createAlias with Groovy String arguments'() {
        given:
        def blackbeard = new Pirate(name: "Blackbeard")
        def jack = new Pirate(name: "Calico Jack")
        def bart = new Pirate(name: "Black Bart")
        [blackbeard, jack, bart]*.save()

        def ship1 = new Ship(name: "Queen Anne's Revenge")
        ship1.addToCrew blackbeard
        ship1.addToCrew jack

        def ship2 = new Ship(name: "Royal Fortune")
        ship2.addToCrew bart

        def ship3 = new Ship(name: "The Treasure")
        ship3.addToCrew jack
        ship3.addToCrew bart

        [ship1, ship2, ship3]*.save()
        session.flush()
        session.clear()
          
        when:
        def ships = Ship.withCriteria {
            def propertyName = 'crew'
            def aliasName = 'c'
            createAlias "${propertyName}", "${aliasName}"
            eq 'c.name', 'Blackbeard'
        }
        
        then:
        ships?.size() == 1
        
        when:
        def ship = ships[0]
        
        then:
        "Queen Anne's Revenge" == ship.name
        ship.crew?.size() == 2
        
        when:
        def crewNames = ship.crew.name
        
        then:
        'Blackbeard' in crewNames
        'Calico Jack' in crewNames
          
        when:
        ships = Ship.withCriteria {
            def propertyName = 'crew'
            def aliasName = 'c'
            createAlias "${propertyName}", "${aliasName}", CriteriaSpecification.LEFT_JOIN
            eq 'c.name', 'Blackbeard'
        }
        
        then:
        ships?.size() == 1
        
        when:
        ship = ships[0]
        
        then:
        "Queen Anne's Revenge" == ship.name
        ship.crew?.size() == 2
        
        when:
        crewNames = ship.crew.name
        
        then:
        'Blackbeard' in crewNames
        'Calico Jack' in crewNames
    }
}

@Entity
class Pirate {
    String name
    static belongsTo = Ship
    static hasMany = [ships: Ship]
}

@Entity
class Ship {
    String name
    static hasMany = [crew: Pirate]
}
