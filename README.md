Grails
===

[Grails][Grails] is a web application framework that uses the [Groovy][Groovy] programming language. 
[Grails]: http://www.grails.org/
[Groovy]: http://groovy.codehaus.org/

Getting Started
---

### Setup

You need a Java Development Kit (JDK) installed, but it is not necessary to install Groovy because it's bundled with the Grails distribution.

To install Grails, visit http://www.grails.org and download the version you would like to use. Set a GRAILS_HOME environment variable to point to the root of the extracted download and add GRAILS_HOME/bin to your executable PATH. Then in a shell, type the following:
	
	grails create-app sampleapp
	
To build Grails, clone this GitHub repository and then in a shell, type the following:
	
	./gradlew install