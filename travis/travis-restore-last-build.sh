#!/bin/bash
set -o pipefail
TRAVIS_REPO_SLUG=$(git config remote.origin.url | awk -F'[/:]' '{ print $(NF-1) "/" $(NF) }' | sed s/\\.git$//)
TRAVIS_BRANCH=$(git rev-parse --abbrev-ref HEAD)
TARGET_URL="s3://s3-eu-west-1.amazonaws.com/grails-travis-failures/$TRAVIS_REPO_SLUG/$TRAVIS_BRANCH"
RESTORE_DIR="/tmp/grails-travis-failures-$$"
if [[ -z "$AWS_ACCESS_KEY_ID" || -z "$AWS_SECRET_ACCESS_KEY" ]]; then
    echo "pass AWS credentials in AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY environment variables."
    exit 1
fi
echo "Restoring $TARGET_URL to $RESTORE_DIR"
( duplicity --progress --no-encryption restore "$TARGET_URL" $RESTORE_DIR 2>&1 | grep -v 'Operation not permitted' ) || echo "make sure you have installed duplicity with brew/apt-get and boto with pip"
