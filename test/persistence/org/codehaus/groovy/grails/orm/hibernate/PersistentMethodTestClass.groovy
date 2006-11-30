package org.codehaus.groovy.grails.orm.hibernate;

public class PersistentMethodTestClass {

	 List optionals = [ "age" ];
	
	 Long id;
	 Long version;
	
	 String firstName;
	 String lastName;
	 Integer age;
	 boolean active = true

	 static constraints = {
	    firstName(length:4..15)
     }
}