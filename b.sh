#!/bin/bash

pushd ./java
ant clean
ant
popd

pushd ./ppsspp
./b.sh --debug
#./b.sh --release
popd
