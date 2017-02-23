#!/bin/bash
# This will run after a successful build
#
# This also means that the later modifications to EXIT_STATUS will not affect
# the build status: Build could now be success, even though the later steps here will
# change EXIT_STATUS variable
set -x

echo "Project Version: '$grailsVersion'"
echo "EXIT STATUS of build: '$EXIT_STATUS'"
echo "Builder Leader: '$BUILD_LEADER'"


if [[ $TRAVIS_PULL_REQUEST == 'false' && $BUILD_LEADER='YES' && $TRAVIS_REPO_SLUG == grails/grails-core && ( $TRAVIS_TAG =~ ^v[[:digit:]] || $TRAVIS_BRANCH =~ ^master|[23]\..\.x$ )  ]]; then
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
        ./gradlew -Psigning.keyId="$SIGNING_KEY" -Psigning.password="$SIGNING_PASSPHRASE" -Psigning.secretKeyRingFile="${TRAVIS_BUILD_DIR}/secring.gpg" publish uploadArchives -x grails-bom:uploadArchives -x grails-dependencies:uploadArchives || EXIT_STATUS=$?
        ./gradlew closeAndPromoteRepository

        if [[ $EXIT_STATUS == 0 ]]; then
            ./gradlew --stop
            # wait 30 seconds to ensure the previous promotion completes
            sleep 30
            ./gradlew -Psigning.keyId="$SIGNING_KEY" -Psigning.password="$SIGNING_PASSPHRASE" -Psigning.secretKeyRingFile="${TRAVIS_BUILD_DIR}/secring.gpg" grails-dependencies:uploadArchives grails-bom:uploadArchives || EXIT_STATUS=$?
            ./gradlew closeAndPromoteRepository
        fi

        if [[ $EXIT_STATUS == 0 ]]; then
            ./gradlew assemble || EXIT_STATUS=$?
        fi

        # Configure GIT
        git config --global credential.helper "store --file=~/.git-credentials"
        echo "https://$GH_TOKEN:@github.com" > ~/.git-credentials

        git config --global user.name "$GIT_NAME"
        git config --global user.email "$GIT_EMAIL"

        # Tag and release the docs
        git clone https://${GH_TOKEN}@github.com/grails/grails-doc.git grails-doc
        cd grails-doc

        echo "grails.version=${TRAVIS_TAG:1}" > gradle.properties
        git add gradle.properties
        git commit -m "Release $TRAVIS_TAG docs"
        git tag $TRAVIS_TAG
        git push --tags
        git push
        cd ..

        # Update the website
        git clone https://${GH_TOKEN}@github.com/grails/grails-static-website.git
        cd grails-static-website
        echo -e "${TRAVIS_TAG:1}" >> generator/src/main/resources/versions
        git add generator/src/main/resources/versions
        git commit -m "Release Grails $TRAVIS_TAG"
        git push
        cd ..

        # Rebuild Artifactory index
        curl -H "X-Api-Key:$ARTIFACTORY_API_KEY" -X POST "http://repo.grails.org/grails/api/maven?repos=libs-releases-local,plugins-releases-local,plugins3-releases-local,core&force=1"

    elif [[ $TRAVIS_BRANCH =~ ^master|[23]\..\.x$ ]]; then
        ./gradlew -Psigning.keyId="$SIGNING_KEY" -Psigning.password="$SIGNING_PASSPHRASE" -Psigning.secretKeyRingFile="${TRAVIS_BUILD_DIR}/secring.gpg" publish || EXIT_STATUS=$?
    fi

elif [[ $TRAVIS_BRANCH =~ ^master|[23]\..\.x$ && $BUILD_LEADER='YES' ]]; then
    ./gradlew -Psigning.keyId="$SIGNING_KEY" -Psigning.password="$SIGNING_PASSPHRASE" -Psigning.secretKeyRingFile="${TRAVIS_BUILD_DIR}/secring.gpg" publish || EXIT_STATUS=$?
    cd ..
    # Trigger the functional tests
    git clone -b master https://${GH_TOKEN}@github.com/grails/grails3-functional-tests.git functional-tests
    cd functional-tests
    echo "$(date)" > .snapshot
    git add .snapshot
    git commit -m "New Core Snapshot: $(date)"
    git push
fi

exit $EXIT_STATUS
