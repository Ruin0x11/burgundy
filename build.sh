#!/bin/bash

pushd ./java
ant clean
ant
popd

pushd ./ppsspp
./b.sh --system-ffmpeg
popd
