#### Build Status
- [![Build Status](https://travis-ci.org/grails/grails-core.svg?branch=master)](https://travis-ci.org/grails/grails-core)

#### Slack Signup
- [Slack Signup](http://slack-signup.grails.org)

Grails
===

[Grails](http://grails.org/) is a framework used to build web applications with the [Groovy](http://groovy-lang.org/) programming language. The core framework is very extensible and there are numerous [plugins](http://grails.org/plugins/) available that provide easy integration of add-on features.

Grails is sponsored by [Object Computing Inc.](http://www.ociweb.com) in St. Louis Missouri.  Please contact <info@ociweb.com> for support inquiries.

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

#### Steps ####

- [ ] Check all of the profiles [here](https://github.com/grails-profiles) to see if any changes need released and update Grails BOM
 for the Grails branch you plan to release [here](https://github.com/grails/grails-core/blob/master/grails-bom/profiles.properties).
- [ ] Release Grails plugins and update Grails BOM for the Grails branch you plan to release [here](https://github.com/grails/grails-core/blob/master/grails-bom/plugins.properties).
- [ ] Edit `grails-bom/plugins.properties`
    - [ ] hibernate4 https://github.com/grails/gorm-hibernate4
    - [ ] hibernate5 https://github.com/grails/gorm-hibernate5
    - [ ] mongodb https://github.com/grails/gorm-mongodb
    - [ ] rx-gorm-rest-client https://github.com/grails/gorm-rest-client
    - [ ] rx-mongodb https://github.com/grails/gorm-mongodb
    - [ ] neo4j https://github.com/grails/gorm-neo4j
    - [ ] cache https://github.com/grails-plugins/grails-cache
    - [ ] scaffolding https://github.com/grails3-plugins/scaffolding
    - [ ] fields https://github.com/grails-fields-plugin/grails-fields
    - [ ] geb https://github.com/grails3-plugins/geb
    - [ ] rxjava https://github.com/grails-plugins/grails-rxjava
    - [ ] views-json https://github.com/grails/grails-views
    - [ ] views-json-templates https://github.com/grails/grails-views
    - [ ] views-markup https://bintray.com/grails/plugins/grails-views
    - [ ] grails-java8 https://github.com/grails-plugins/grails-java8
- [ ] Release Grails dependencies and update `build.gradle` [here](https://github.com/grails/grails-core/blob/master/build.gradle).
Typically you want to release
  - [ ] async https://github.com/grails/grails-async
  - [ ] gsp https://github.com/grails/grails-gsp
  - [ ] testing-support https://github.com/grails/grails-testing-support
  - [ ] data-store https://github.com/grails/grails-data-mapping
  - [ ] And any other third party dependency such as Groovy, Spring,...
- [ ] Merge the branch for `grails-doc`. If you're releasing `3.2.x` then merge into `3.3.x`
- [ ] Check release of a Github branch of `grails-core` in `grails-doc` repository is correct so that the API docs generate correctly.

Next, update the Grails version in `build.gradle` and `grails-core/src/test/groovy/grails/util/GrailsUtilTests.java` and then push the changes to git:

    $ git add build.gradle grails-core/src/test/groovy/grails/util/GrailsUtilTests.java
    $ git commit -m "Release Grails 3.0.1"
    $ git tag v3.0.1
    $ git push --tags
    $ git push

By tagging the release Travis will perform all the necessary steps to release a new version of Grails, just wait for [the build](https://travis-ci.org/grails/grails-core) to complete.
This process will also trigger a release of Grails docs.

- [ ] Update Grails website to display latest version [here](https://github.com/grails/grails-static-website/blob/master/main/src/main/groovy/org/grails/main/SiteMap.groovy).
- [ ] Update profile-versions-repositories https://github.com/grails-profiles-versions
- [ ] Write the release notes
- [ ] Tweet about it in Social Networks

The [Travis CI](https://travis-ci.org/grails/grails-core) build will automatically upload the tagged release to Github and be available on the [Releases page](https://github.com/grails/grails-core/releases).

License
---

Grails and Groovy are licensed under the terms of the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).


***

YourKit is kindly supporting Grails open source projects with its full-featured Java Profiler.
YourKit, LLC is the creator of innovative and intelligent tools for profiling
Java and .NET applications. Take a look at YourKit's leading software products:
[YourKit Java Profiler](http://www.yourkit.com/java/profiler/index.jsp) and
[YourKit .NET Profiler](http://www.yourkit.com/.net/profiler/index.jsp).
