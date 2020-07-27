#!/usr/bin/env bash

# Shell script to create the OSX installer package using JDK14 jpackage
# Source is in test/java/ReadYubikey.java
JDK14=/Library/Java/JavaVirtualMachines/jdk-14.0.2.jdk/Contents/Home

$JDK14/bin/jar cvef ReadYubikey ReadYubikey.jar -C target/test-classes ReadYubikey.class
$JDK14/bin/jpackage --name ReadYubikey --input . --main-jar ReadYubikey.jar --main-class ReadYubikey