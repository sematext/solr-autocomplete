#!/bin/env bash

version=`grep '<version>' pom.xml | head -1 | grep version | cut -d\> -f2 | cut -d\< -f1`

rm -v st-AutoComplete-$version.zip

mvn clean compile jar:jar dependency:copy-dependencies
rm -v ./target/dependency/*test*jar
rm -v ./target/dependency/*junit*jar
rm -v ./target/dependency/*jetty*jar
rm -v ./target/dependency/*morfologik*jar
rm -v ./target/dependency/*servlet*jar
rm -v ./target/dependency/*spatial*jar
rm -v ./target/dependency/*ant*jar
rm -v ./target/dependency/*zookeeper*jar

cat bin/prep-classpath.tmpl | sed -e "s/__VERSION__/$version/g" > bin/prep-classpath.sh
zip -r st-AutoComplete-$version.zip doc/*pdf LICENSE.pdf apache/httpd.conf example/solr/collection1/conf/schema.xml example/solr/collection1/conf/solrconfig.xml web/auto-complete.html web/css web/js target/st-AutoComplete-$version.jar target/dependency/*jar bin/prep-classpath.sh example/exampledocs -x "*.svn*"

echo
unzip -l st-AutoComplete-$version.zip | egrep -v '.html$|/$'

echo
egrep -i "error|fail|cannot|can't" pack.log
echo
ls -al *zip
