#!/usr/bin/env bash

JAR_DIR=target/dependency
for jar in `ls -1 $JAR_DIR`; do
  CLASSPATH=$CLASSPATH:$JAR_DIR/$jar
done
echo "$CLASSPATH:target/st-AutoComplete-1.6.6.0.1-SNAPSHOT.jar"
