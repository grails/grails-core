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

if [ "${TRAVIS_JDK_VERSION}" == "openjdk11" ] ; then
  exit $EXIT_STATUS
fi

echo "Publishing for branch $TRAVIS_BRANCH JDK: $TRAVIS_JDK_VERSION"

if [[ $EXIT_STATUS -eq 0 ]]; then
    ./gradlew --stop

	# Configure GIT
	git config --global credential.helper "store --file=~/.git-credentials"
	echo "https://$GH_TOKEN:@github.com" > ~/.git-credentials

	git config --global user.name "$GIT_NAME"
	git config --global user.email "$GIT_EMAIL"

	if [[ $TRAVIS_PULL_REQUEST == 'false' && $TRAVIS_REPO_SLUG == grails/grails-core && $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then

	    echo "Publishing archives"

	    echo "Running Gradle publish for branch $TRAVIS_BRANCH"
	    ./gradlew --stop
	    ./gradlew --no-daemon bintrayUpload

	    if [[ $EXIT_STATUS == 0 ]]; then
	        ./gradlew --stop
	        ./gradlew --no-daemon assemble || EXIT_STATUS=$?

	        if [[ $EXIT_STATUS == 0 ]]; then
			    # Tag and release the docs
			    # version="$TRAVIS_TAG"
       #                      version=${version:1}
       #                      majorVersion=${version:0:4}
       #                      majorVersion="${majorVersion}x"
                git clone https://${GH_TOKEN}@github.com/grails/grails-doc.git -b master grails-doc --single-branch > /dev/null
			    cd grails-doc

			    echo "grails.version=${TRAVIS_TAG:1}" > gradle.properties
			    git add gradle.properties
			    git commit -m "Release $TRAVIS_TAG docs"
			    git tag $TRAVIS_TAG
			    git push --tags
			    git push
			    cd ..

			    # Update the website
                            echo "set released version in static website"      
			    git clone https://${GH_TOKEN}@github.com/grails/grails-static-website.git
			    cd grails-static-website
			    version="$TRAVIS_TAG"
                            version=${version:1}
			    ./release.sh $version
	                    git commit -a -m "Updating grails version at static website for Travis build: https://travis-ci.org/$TRAVIS_REPO_SLUG/builds/$TRAVIS_BUILD_ID" && {
                                git push origin HEAD || true
                            }			    
			    cd ..
			    rm -rf grails-static-website

			    # Rebuild Artifactory index
			    curl -H "X-Api-Key:$ARTIFACTORY_API_KEY" -X POST "http://repo.grails.org/grails/api/maven?repos=libs-releases-local,plugins-releases-local,plugins3-releases-local,core&force=1"

        	fi
	    fi
	elif [[ $TRAVIS_PULL_REQUEST == 'false' && $TRAVIS_BRANCH =~ ^master|[23]\..\.x$ ]]; then
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
