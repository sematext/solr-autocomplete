#!/usr/bin/env sh

#
# Usage: sh file-loader.sh <input file> <solr URL>
#
# Example:
# sh file-loader.sh /tmp/suggestions.txt http://localhost:8983/solr
#

IN_FILE=$1
SOLR_URL=$2
JAR_DIR=target/dependency

echo
echo Loading $IN_FILE into AutoComplete backend at $SOLR_URL
echo
sleep 5

cat $IN_FILE | java -cp target/st-AutoComplete-1.6.6.0.1-SNAPSHOT.jar:$JAR_DIR/solr-core-1.3.0.jar:$JAR_DIR/solr-common-1.3.0.jar:$JAR_DIR/commons-httpclient-4.4.1.jar:$JAR_DIR/commons-codec-1.10.jar com.sematext.autocomplete.loader.FileLoader $SOLR_URL

echo
wc -l $IN_FILE
echo done
echo
