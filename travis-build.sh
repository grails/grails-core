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
echo "Executing integration tests"
./gradlew --no-daemon --stacktrace --info integrationTest < /dev/null || EXIT_STATUS=$?
echo "Done."

if [[ $TRAVIS_BRANCH =~ ^master|2\.[34]\.x$ && $TRAVIS_REPO_SLUG == "grails/grails-core" 
	&& $TRAVIS_PULL_REQUEST == 'false' 
    && $EXIT_STATUS -eq 0 && $grailsVersion == *-SNAPSHOT* 
    && -n "$ARTIFACTORY_PASSWORD" ]]; then
    echo "Publishing archives"
    upload_skip_tasks="-x :grails-dependencies:assemble -x :grails-dependencies:install -x :grails-dependencies:uploadPublished -x :grails-bom:assemble -x :grails-bom:install -x :grails-bom:uploadPublished"
    ./gradlew -PartifactoryPublishUsername=travis-grails-core $upload_skip_tasks upload || EXIT_STATUS=$?
    echo "Done."
fi

exit $EXIT_STATUS
