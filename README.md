Grails
===

[Grails][Grails] is a framework used to build web applications with the [Groovy][Groovy] programming language. The core framework is very extensible and there are numerous [plugins][plugins] available.
[Grails]: http://grails.org/
[Groovy]: http://groovy.codehaus.org/
[plugins]: http://grails.org/plugins/

Getting Started
---

You need a Java Development Kit (JDK) installed, but it is not necessary to install Groovy because it's bundled with the Grails distribution.

To install Grails, visit http://www.grails.org and download the version you would like to use. Set a GRAILS_HOME environment variable to point to the root of the extracted download and add GRAILS_HOME/bin to your executable PATH. Then in a shell, type the following:
	
	grails create-app sampleapp
	
To build Grails, clone this GitHub repository and then in a shell, type the following:
	
	./gradlew install