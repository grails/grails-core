#!/bin/bash
IFS='
'
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
sudo apt-get install -y --force-yes xml-twig-tools xsltproc
ROOTDIR="$1"
if [ -z "$ROOTDIR" ]; then
	ROOTDIR="."
fi
FILES=$(find "$ROOTDIR" -path '*/build/test-results/*.xml' | xargs --no-run-if-empty xml_grep --files --cond 'testsuite[@failures > 0 or @errors > 0]')
if [ -n "$FILES" ]; then
	for file in "$FILES"; do
		if [ -f "$file" ]; then
			echo '====================================================='
			xsltproc "$DIR/junit-xml-format-errors.xsl" "$file"
		fi
	done
	echo '====================================================='
fi
