#!/bin/bash

# Set Gradle daemon JVM args
mkdir ~/.gradle
echo "org.gradle.jvmargs=-XX\:MaxPermSize\=512m -Xmx1024m -Dfile.encoding\=UTF-8 -Duser.country\=US -Duser.language\=en -Duser.variant" >> ~/.gradle/gradle.properties
echo "org.gradle.daemon=true" >> ~/.gradle/gradle.properties

grailsVersion="$(grep 'grailsVersion =' build.gradle | egrep -v ^[[:blank:]]*\/\/)"
grailsVersion="${grailsVersion#*=}"
grailsVersion="${grailsVersion//[[:blank:]\'\"]/}"

echo "Project Version: '$grailsVersion'"

EXIT_STATUS=0
./gradlew --stop

if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
    echo "Tagged Release Skipping Tests for Publish"
else
    echo "Executing tests"
    ./gradlew --stacktrace test -x grails-test-suite-web:test || EXIT_STATUS=$?
    if [[ $EXIT_STATUS == 0 ]]; then
        ./gradlew --stop
        ./gradlew --stacktrace grails-test-suite-web:test || EXIT_STATUS=$?
    fi
    echo "Done."
    if [[ $EXIT_STATUS == 0 ]]; then
      echo "Executing integration tests"
      ./gradlew --stacktrace --info integrationTest || EXIT_STATUS=$?
      echo "Done."
    fi
fi

export EXIT_STATUS
export grailsVersion
# done. after build, then after_success will run if EXIT_STATUS is 0
#
# This also means that the later modifications to EXIT_STATUS will not affect
# the build status: Build could now be success, even though the later steps will
# change EXIT_STATUS variable

exit $EXIT_STATUS
