#!/bin/bash
if [[ $TRAVIS_BRANCH =~ ^master|grails-shell|2\..\.x$ && $TRAVIS_REPO_SLUG == grails/*
    && $TRAVIS_PULL_REQUEST == 'false' ]]; then
echo "Install duplicity with S3 support"
sudo apt-get update
sudo apt-get install -y --force-yes duplicity python-pip
sudo pip install boto
TARGET_URL="s3://s3-eu-west-1.amazonaws.com/grails-travis-failures/$TRAVIS_REPO_SLUG/$TRAVIS_BRANCH"
echo "Uploading all files in working directory to $TARGET_URL ..."
duplicity --no-encryption --full-if-older-than=14D incr . "$TARGET_URL"
echo "Cleaning old uploads from S3 ..."
duplicity --no-encryption --force remove-older-than 30D "$TARGET_URL"
fi
