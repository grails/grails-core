[![Java CI](https://github.com/grails/grails-core/actions/workflows/gradle.yml/badge.svg?branch=2.5.x)](https://github.com/grails/grails-core/actions/workflows/gradle.yml)

Grails
===

[Grails][Grails] is a framework used to build web applications with the [Groovy][Groovy] programming language. The core framework is very extensible and there are numerous [plugins][plugins] available that provide easy integration of add-on features.
[Grails]: http://grails.org/
[Groovy]: http://groovy.codehaus.org/
[plugins]: http://grails.org/plugins/

Getting Started
---

You need a Java Development Kit (JDK) installed, but it is not necessary to install Groovy because it's bundled with the Grails distribution.

To install Grails, visit http://grails.org/Download and download the version you would like to use. Set a `GRAILS_HOME` environment variable to point to the root of the extracted download and add `GRAILS_HOME/bin` to your executable `PATH`. Then in a shell, type the following:
	
	grails create-app sampleapp
	cd sampleapp
	grails run-app
	
To build Grails, clone this GitHub repository and execute the install Gradle target:

    git clone https://github.com/grails/grails-core.git
    cd grails-core
    ./gradlew install
	
License
---

Grails and Groovy are licensed under the terms of the [Apache License, Version 2.0][Apache License, Version 2.0].
[Apache License, Version 2.0]: http://www.apache.org/licenses/LICENSE-2.0.html
