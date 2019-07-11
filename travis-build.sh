#!/bin/bash
echo "Gradle command to be run: '$GRADLE_CMD'"

EXIT_STATUS=0

./gradlew sdkDefaultVersion

exit $EXIT_STATUS