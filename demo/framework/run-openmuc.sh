#!/bin/sh

DIR_NAME=`dirname $0`
cd $DIR_NAME

if [ "$1" = "-b" ]
then
    java -Dgosh.args=--nointeractive -jar felix/felix.jar >openmuc.out 2>&1 &
else
    java -jar felix/felix.jar
fi
