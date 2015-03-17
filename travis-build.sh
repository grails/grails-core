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
    ./gradlew --stacktrace test || EXIT_STATUS=$?
    echo "Done."
    if [[ $EXIT_STATUS == 0 ]]; then
      echo "Executing integration tests"
      ./gradlew --stacktrace --info integrationTest || EXIT_STATUS=$?
      echo "Done."
    fi
fi


if [[ $TRAVIS_PULL_REQUEST == 'false' && $EXIT_STATUS -eq 0
    && $TRAVIS_REPO_SLUG == grails/grails-core && ( $TRAVIS_TAG =~ ^v[[:digit:]] || $TRAVIS_BRANCH =~ ^master|[23]\..\.x$ )  ]]; then
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
        ./gradlew -Psigning.keyId="$SIGNING_KEY" -Psigning.password="$SIGNING_PASSPHRASE" -Psigning.secretKeyRingFile="${TRAVIS_BUILD_DIR}/secring.gpg" uploadArchives || EXIT_STATUS=$?
        ./gradlew assemble || EXIT_STATUS=$?
    elif [[ $TRAVIS_BRANCH =~ ^master|[23]\..\.x$ ]]; then
        ./gradlew -Psigning.keyId="$SIGNING_KEY" -Psigning.password="$SIGNING_PASSPHRASE" -Psigning.secretKeyRingFile="${TRAVIS_BUILD_DIR}/secring.gpg" publish || EXIT_STATUS=$?
    fi

fi

if [[ $EXIT_STATUS == 0 ]]; then
    ./gradlew travisciTrigger -i
fi

exit $EXIT_STATUS
