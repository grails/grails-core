#!/bin/bash

grailsVersion="$(grep 'grailsVersion =' build.gradle | egrep -v ^[[:blank:]]*\/\/)"
grailsVersion="${grailsVersion#*=}"
grailsVersion="${grailsVersion//[[:blank:]\'\"]/}"

echo "Project Version: '$grailsVersion'"

EXIT_STATUS=0
./gradlew --stop
./gradlew --no-daemon --stacktrace test || EXIT_STATUS=$?


if [[ ( $TRAVIS_BRANCH == 'master' ) && $TRAVIS_REPO_SLUG == "grails/grails-core" && $TRAVIS_PULL_REQUEST == 'false' && $EXIT_STATUS -eq 0 ]]; then

    echo "Publishing archives"

    if [[ -n $TRAVIS_TAG ]]; then
        ./gradlew bintrayUpload || EXIT_STATUS=$?
    else
        ./gradlew publish || EXIT_STATUS=$?
    fi

fi

exit $EXIT_STATUS
