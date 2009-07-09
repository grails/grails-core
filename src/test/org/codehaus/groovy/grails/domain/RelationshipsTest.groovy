package org.codehaus.groovy.grails.domain;

class RelationshipsTest {
   def hasMany = [ 	"ones" : OneToManyTest2.class,
  								"manys" : ManyToManyTest.class,
  								"uniones" : UniOneToManyTest.class ];

    Long id;
    Long version;

    Set manys; // many-to-many relationship
    OneToOneTest one; // uni-directional one-to-one
    Set ones; // bi-directional one-to-many relationship    
    Set uniones; // uni-directional one-to-many relationship       
}


