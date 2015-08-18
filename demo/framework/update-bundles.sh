#!/bin/sh

DIR_NAME=`dirname $0`
cd $DIR_NAME

gradle -p ../../ build
gradle -p ../projects/simpledemoapp build
gradle -p ../projects/dummydriver build
gradle syncBundles copyFelixMain
