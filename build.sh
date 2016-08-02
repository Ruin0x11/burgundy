#!/bin/bash

pushd ./java
ant
popd

pushd ./ppsspp
./b.sh
popd
