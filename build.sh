#!/usr/bin/env sh
javac --release 21 --enable-preview Reproducer.java
java -cp . --enable-preview --enable-native-access=ALL-UNNAMED Reproducer
