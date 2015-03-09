#!/bin/bash

grailsVersion="$(grep 'grailsVersion =' build.gradle | egrep -v ^[[:blank:]]*\/\/)"
grailsVersion="${grailsVersion#*=}"
grailsVersion="${grailsVersion//[[:blank:]\'\"]/}"

echo "Project Version: '$grailsVersion'"

EXIT_STATUS=0
./gradlew --stop


if [[ $TRAVIS_PULL_REQUEST == 'false' && $EXIT_STATUS -eq 0 ]]; then

    echo "Publishing archives"

    gpg --keyserver keyserver.ubuntu.com --recv-key $SIGNING_KEY

    if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
        ./gradlew -Dsigning.key="$SIGNING_KEY" -Dsigning.password="$SIGNING_PASSPHRASE" -Dsigning.secretKeyRingFile="$USER/.gnupg/secring.gpg uploadArchives publish || EXIT_STATUS=$?
        ./gradlew assemble || EXIT_STATUS=$?
    elif [[ $TRAVIS_BRANCH =~ ^(master|2.5.x|2.4.x)$ ]]; then

        ./gradlew -Dsigning.key="$SIGNING_KEY" -Dsigning.password="$SIGNING_PASSPHRASE" -Dsigning.secretKeyRingFile="$USER/.gnupg/secring.gpg uploadArchives publish || EXIT_STATUS=$?
    fi

fi

if [[ $EXIT_STATUS == 0 ]]; then
    ./gradlew travisciTrigger -i
fi

exit $EXIT_STATUS
