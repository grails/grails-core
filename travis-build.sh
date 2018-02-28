#!/bin/bash
echo "Gradle command to be run: '$GRADLE_CMD'"

EXIT_STATUS=0

./gradlew --stop
./gradlew clean classes --no-daemon
./gradlew testClasses --no-daemon

if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
    echo "Tagged Release Skipping Tests for Publish"
else
    echo "Executing tests"
    ./gradlew --stop
    ./gradlew grails-test-suite-persistence:test grails-test-suite-uber:test grails-test-suite-web:test --no-daemon && {
	    ./gradlew --stop
	    ./gradlew check -x grails-test-suite-persistence:test -x grails-test-suite-uber:test -x grails-test-suite-web:test --no-daemon
    } || EXIT_STATUS=$?
    echo "Done."
fi

if [[ $EXIT_STATUS -eq 0 ]]; then
    ./gradlew --stop

	# Configure GIT
	git config --global credential.helper "store --file=~/.git-credentials"
	echo "https://$GH_TOKEN:@github.com" > ~/.git-credentials

	git config --global user.name "$GIT_NAME"
	git config --global user.email "$GIT_EMAIL"

	if [[ $TRAVIS_PULL_REQUEST == 'false' && $TRAVIS_REPO_SLUG == grails/grails-core && $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
	    # files encrypted with 'openssl aes-256-cbc -in <INPUT FILE> -out <OUTPUT_FILE> -pass pass:$SIGNING_PASSPHRASE'
	    openssl aes-256-cbc -pass pass:$SIGNING_PASSPHRASE -in secring.gpg.enc -out secring.gpg -d
	    openssl aes-256-cbc -pass pass:$SIGNING_PASSPHRASE -in pubring.gpg.enc -out pubring.gpg -d
	    openssl aes-256-cbc -pass pass:$SIGNING_PASSPHRASE -in settings.xml.enc -out settings.xml -d
	    mkdir -p ~/.m2
	    cp settings.xml ~/.m2/settings.xml

	    echo "Publishing archives"

	    gpg --keyserver keyserver.ubuntu.com --recv-key $SIGNING_KEY

	    echo "Running Gradle publish for branch $TRAVIS_BRANCH"
	    ./gradlew --stop
	    ./gradlew --no-daemon -Psigning.keyId="$SIGNING_KEY" -Psigning.password="$SIGNING_PASSPHRASE" -Psigning.secretKeyRingFile="${TRAVIS_BUILD_DIR}/secring.gpg" publish uploadArchives -x grails-bom:uploadArchives -x grails-dependencies:uploadArchives || EXIT_STATUS=$?
	    ./gradlew closeAndReleaseRepository

	    if [[ $EXIT_STATUS == 0 ]]; then
	        ./gradlew --stop
	        ./gradlew --no-daemon -Psigning.keyId="$SIGNING_KEY" -Psigning.password="$SIGNING_PASSPHRASE" -Psigning.secretKeyRingFile="${TRAVIS_BUILD_DIR}/secring.gpg" grails-dependencies:uploadArchives grails-bom:uploadArchives || EXIT_STATUS=$?
	        ./gradlew closeAndReleaseRepository
	    fi

	    if [[ $EXIT_STATUS == 0 ]]; then
	        ./gradlew --stop
	        ./gradlew --no-daemon assemble || EXIT_STATUS=$?

	        if [[ $EXIT_STATUS == 0 ]]; then
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

        	fi
	    fi
	elif [[ $TRAVIS_BRANCH =~ ^master|[23]\..\.x$ ]]; then
	    echo "Builder Leading Publishing Snapshot..."
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
fi

# done. after build, then after_success will run if EXIT_STATUS is 0
#
# This also means that the later modifications to EXIT_STATUS will not affect
# the build status: Build could now be success, even though the later steps will
# change EXIT_STATUS variable

exit $EXIT_STATUS
