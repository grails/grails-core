#!/bin/bash
set -x

grailsVersion="$(grep 'grailsVersion =' build.gradle | egrep -v ^[[:blank:]]*\/\/)"
grailsVersion="${grailsVersion#*=}"
grailsVersion="${grailsVersion//[[:blank:]\'\"]/}"

echo "Project Version: '$grailsVersion'"
echo "Gradle command to be run: '$GRADLE_CMD'"

EXIT_STATUS=0

if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
    echo "Tagged Release Skipping Tests for Publish"
else
    echo "Executing tests"
    ./gradlew --stop
    ./gradlew compileTestGroovy --no-daemon
    killall -9 java
    ./gradlew check --no-daemon || EXIT_STATUS=$?
    echo "Done."
fi

if [[ $EXIT_STATUS -eq 0 ]]; then
    ./gradlew --stop
    ./travis-publish-archives.sh || EXIT_STATUS=$?
fi

# done. after build, then after_success will run if EXIT_STATUS is 0
#
# This also means that the later modifications to EXIT_STATUS will not affect
# the build status: Build could now be success, even though the later steps will
# change EXIT_STATUS variable

exit $EXIT_STATUS
