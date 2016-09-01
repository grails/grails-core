#!/bin/bash
# This will run after a successful build
#
# This also means that the later modifications to EXIT_STATUS will not affect
# the build status: Build could now be success, even though the later steps here will
# change EXIT_STATUS variable
set -x

echo "Project Version: '$grailsVersion'"
echo "EXIT STATUS of build: '$EXIT_STATUS'"


if [[ $TRAVIS_PULL_REQUEST == 'false'
    && $TRAVIS_REPO_SLUG == grails/grails-core
    && ( $TRAVIS_TAG =~ ^v[[:digit:]] || $TRAVIS_BRANCH =~ ^master|[23]\..\.x$ )  ]]; then
    # files encrypted with 'openssl aes-256-cbc -in <INPUT FILE> -out <OUTPUT_FILE> -pass pass:$SIGNING_PASSPHRASE'
    openssl aes-256-cbc -pass pass:$SIGNING_PASSPHRASE -in secring.gpg.enc -out secring.gpg -d
    openssl aes-256-cbc -pass pass:$SIGNING_PASSPHRASE -in pubring.gpg.enc -out pubring.gpg -d
    openssl aes-256-cbc -pass pass:$SIGNING_PASSPHRASE -in settings.xml.enc -out settings.xml -d
    mkdir -p ~/.m2
    cp settings.xml ~/.m2/settings.xml

    mv ~/.gradle/gradle.properties{,.orig}
    echo "org.gradle.jvmargs=-XX\:MaxPermSize\=1024m -Xmx1500m -Dfile.encoding\=UTF-8 -Duser.country\=US -Duser.language\=en -Duser.variant" >> ~/.gradle/gradle.properties
    echo "org.gradle.daemon=true" >> ~/.gradle/gradle.properties
    ./gradlew --stop
    #./gradlew groovydoc
    mv ~/.gradle/gradle.properties{.orig,}

    echo "Publishing archives"

    gpg --keyserver keyserver.ubuntu.com --recv-key $SIGNING_KEY

    echo "Running Gradle publish for branch $TRAVIS_BRANCH"

    if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
        ./gradlew -Psigning.keyId="$SIGNING_KEY" -Psigning.password="$SIGNING_PASSPHRASE" -Psigning.secretKeyRingFile="${TRAVIS_BUILD_DIR}/secring.gpg" grails-dependencies:uploadArchives grails-bom:uploadArchives || EXIT_STATUS=$?
        ./gradlew closeAndPromoteRepository
        ./gradlew assemble || EXIT_STATUS=$?
    fi

fi

exit $EXIT_STATUS
