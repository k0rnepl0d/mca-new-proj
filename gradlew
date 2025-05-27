#!/usr/bin/env sh
# Gradle wrapper bootstrap
DIR="$( cd "$( dirname "$0" )" && pwd )"
exec "$DIR/gradle/wrapper/gradle-wrapper.jar" "$@"