#!/bin/bash

grailsVersion="$(grep 'grailsVersion =' build.gradle | egrep -v ^[[:blank:]]*\/\/)"
grailsVersion="${grailsVersion#*=}"
grailsVersion="${grailsVersion//[[:blank:]\'\"]/}"

echo "Project Version: '$grailsVersion'"

EXIT_STATUS=0
./gradlew --stop
echo "org.gradle.daemon=false" >> ~/.gradle/gradle.properties
echo "Executing tests"
./gradlew --no-daemon --stacktrace test || EXIT_STATUS=$?
echo "Done."
if [[ $EXIT_STATUS == 0 ]]; then
  echo "Executing integration tests"
  ./gradlew --no-daemon --stacktrace --info integrationTest < /dev/null || EXIT_STATUS=$?
  echo "Done."
fi

if [[ $TRAVIS_PULL_REQUEST == 'false' && $EXIT_STATUS -eq 0 ]]; then

    echo "Publishing archives"

    if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
        ./gradlew bintrayUpload || EXIT_STATUS=$?
        ./gradlew assemble || EXIT_STATUS=$?

        version="$TRAVIS_TAG"
        version=${version:1}
        zipName="grails-$version"
        export RELEASE_FILE="build/distributions/${zipName}.zip"
    else
        ./gradlew publish || EXIT_STATUS=$?
    fi

fi

if [[ $EXIT_STATUS == 0 ]]; then
    ./gradlew travisciTrigger -i
fi

exit $EXIT_STATUS
