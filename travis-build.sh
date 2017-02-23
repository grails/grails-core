#!/bin/bash
set -x

# Configure GIT
git config --global user.name "$GIT_NAME"
git config --global user.email "$GIT_EMAIL"
git config --global credential.helper "store --file=~/.git-credentials"

# Set Gradle daemon JVM args
mkdir ~/.gradle
echo "org.gradle.jvmargs=-XX\:MaxPermSize\=512m -Xmx1024m -Dfile.encoding\=UTF-8 -Duser.country\=US -Duser.language\=en -Duser.variant" >> ~/.gradle/gradle.properties
echo "org.gradle.daemon=true" >> ~/.gradle/gradle.properties

grailsVersion="$(grep 'grailsVersion =' build.gradle | egrep -v ^[[:blank:]]*\/\/)"
grailsVersion="${grailsVersion#*=}"
grailsVersion="${grailsVersion//[[:blank:]\'\"]/}"

echo "Project Version: '$grailsVersion'"
echo "Gradle command to be run: '$GRADLE_CMD'"

EXIT_STATUS=0
./gradlew --stop

if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
    echo "Tagged Release Skipping Tests for Publish"
    ./travis-publish-archives.sh
else
    echo "Executing tests"
    ./gradlew --stacktrace test  || EXIT_STATUS=$?
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
# change EXIT_STATUS variable>>>>>>> parent of 19b4737... Restore previous build since matrix changes broke release process

exit $EXIT_STATUS
