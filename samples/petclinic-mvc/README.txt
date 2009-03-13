Pet Clinic Spring MVC Sample
----------------------------

This example example application is based on the original Spring Pet Clinic sample from the Spring framework's samples directory. It demonstrates using GORM (Grails' ORM technology built on Hibernate) as the persistence engine rather than Hibernate, whilst the controllers have been written in Groovy.

Usage
-----

The build for this samples must run from the GRAILS_HOME/samples/petclinic-mvc directory as it depends on relative paths to obtain dependencies. To get started simply run:

$ ant war

This will produce a target/petclinic.war file which you can deploy on Tomcat 6 or any Servlet 2.4 compliant container.
