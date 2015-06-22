[![Build Status](https://travis-ci.org/grails/grails-core.svg?branch=master)](https://travis-ci.org/grails/grails-core)

Grails
===

[Grails][Grails] is a framework used to build web applications with the [Groovy][Groovy] programming language. The core framework is very extensible and there are numerous [plugins][plugins] available that provide easy integration of add-on features.
[Grails]: http://grails.org/
[Groovy]: http://groovy-lang.org/
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
    
If you encounter out of memory errors when trying to run the install target, try adjusting Gradle build settings. For example:

    export GRADLE_OPTS="-Xmx2G -Xms2G -XX:NewSize=512m -XX:MaxNewSize=512m -XX:MaxPermSize=1G"

Performing a Release
---

Releases of Grails are automated by [Travis CI](https://travis-ci.org/grails/grails-core).

To create a release perform the following steps.

First check that the [tests are passing](https://github.com/grails/grails-core/wiki/Travis-CI-status) and all is well on Travis.

With that done checkout the [Profile Repository](https://github.com/grails/grails-profile-repository) and tag it for the release version. The tag should begin with the letter 'v'. For example::

    $ git clone git@github.com:grails/grails-profile-repository.git
    $ cd grails-profile-repository
    $ git tag v3.0.2
    $ git push --tags

Next, update the Grails version in `build.gradle` and `grails-core/src/test/groovy/grails/util/GrailsUtilTests.java` and then push the changes to git:

    $ git add build.gradle grails-core/src/test/groovy/grails/util/GrailsUtilTests.java
    $ git commit -m "Release Grails 3.0.2"
    $ git push

After pushing these changes to the repository you must wait for [the build](https://travis-ci.org/grails/grails-core) to complete. During this phase the JAR files will be uploaded  to the [Grails Artefactory Repository](https://repo.grails.org/grails/libs-releases-local/).

Once the build completes successfully, tag the release using Git: 

     $ git tag v3.0.2
     $ git push --tags


The [Travis CI](https://travis-ci.org/grails/grails-core) build will run again and automatically upload the tagged release to Github and be available of the [Releases page](https://github.com/grails/grails-core/releases).

Note: Although by default Grails uses Artefactory to resolve dependencies, it is useful to have them in Maven Central too. To ensure they go to Maven Central login to [Sonatype OSS Nexus](https://oss.sonatype.org) with your account details then "Close" and "Release" the staged JAR files.

At this point it is a good idea to release [the documentation](https://github.com/grails/grails-doc):

    $ git clone git@github.com:grails/grails-doc.git
    $ cd grails-doc
    $ echo "grails.version=3.0.2" > gradle.properties
    $ git add gradle.properties
    $ git commit -m "Release 3.0.2 docs"
    $ git tag v3.0.2
    $ git push --tags
    
The [Travis CI build](https://travis-ci.org/grails/grails-doc) for the documentation will automatically publish the documentation and make it available on the website at: http://grails.org/doc/3.0.2

Finally to update the website's download page you should [edit the sitemap](https://github.com/grails/grails-static-website/blob/39c84b93e08ec111a7860075b89082c46083fe34/site/src/site/sitemap.groovy#L108)


License
---

Grails and Groovy are licensed under the terms of the [Apache License, Version 2.0][Apache License, Version 2.0].
[Apache License, Version 2.0]: http://www.apache.org/licenses/LICENSE-2.0.html


***

YourKit is kindly supporting Grails open source projects with its full-featured Java Profiler.
YourKit, LLC is the creator of innovative and intelligent tools for profiling
Java and .NET applications. Take a look at YourKit's leading software products:
[YourKit Java Profiler](http://www.yourkit.com/java/profiler/index.jsp) and
[YourKit .NET Profiler](http://www.yourkit.com/.net/profiler/index.jsp).
