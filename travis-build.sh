#!/bin/bash

grailsVersion="$(grep 'grailsVersion =' build.gradle | egrep -v ^[[:blank:]]*\/\/)"
grailsVersion="${grailsVersion#*=}"
grailsVersion="${grailsVersion//[[:blank:]\'\"]/}"

echo "Project Version: '$grailsVersion'"

EXIT_STATUS=0
./gradlew test || EXIT_STATUS=$?

if [[ $TRAVIS_BRANCH =~ ^master|2\.[34]\.x$ && $TRAVIS_REPO_SLUG == "grails/grails-core" 
	&& $TRAVIS_PULL_REQUEST == 'false' 
    && $EXIT_STATUS -eq 0 && $grailsVersion == *-SNAPSHOT* 
    && -n "$ARTIFACTORY_PASSWORD" ]]; then
    echo "Publishing archives"
    ./gradlew -PartifactoryPublishUsername=travis-grails-core upload
fi

exit $EXIT_STATUS