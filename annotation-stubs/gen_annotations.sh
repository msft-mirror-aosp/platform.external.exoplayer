#!/bin/bash

# Creates stubs for the checker framework annotations.

ANNOTATIONS=(
    org.checkerframework.checker.initialization.qual.UnknownInitialization
    org.checkerframework.checker.nullness.compatqual.NullableType
    org.checkerframework.checker.nullness.qual.EnsuresNonNull
    org.checkerframework.checker.nullness.qual.EnsuresNonNullIf
    org.checkerframework.checker.nullness.qual.MonotonicNonNull
    org.checkerframework.checker.nullness.qual.PolyNull
    org.checkerframework.checker.nullness.qual.RequiresNonNull
)

rm -r $(dirname $0)/src/*
for a in ${ANNOTATIONS[@]}; do
    package=${a%.*}
    class=${a##*.}
    dir=$(dirname $0)/src/${package//.//}
    file=${class}.java
    mkdir -p ${dir}
    sed -e"s/__PACKAGE__/${package}/" -e"s/__CLASS__/${class}/" template.java > ${dir}/${file}
done
