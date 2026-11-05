#!/usr/bin/env bash
set -euo pipefail

# Two independent Maven builds: the consumer resolves the installed production JAR.
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CONSUMER="$ROOT/acceptance/consumer"
: "${JAVA_HOME:?Set JAVA_HOME to a JDK 11 installation}"
MVN="${MVN:-mvn}"

"$MVN" -B -ntp "$@" -f "$ROOT/pom.xml" clean install
engine_jars=("$ROOT"/target/reform-engine-core-*.jar)
if [[ ${#engine_jars[@]} -ne 1 || ! -f "${engine_jars[0]}" ]]; then
    echo 'Expected exactly one production engine JAR after clean install' >&2
    exit 1
fi
"$MVN" -B -ntp "$@" -f "$CONSUMER/pom.xml" \
    "-Dacceptance.engineJar=${engine_jars[0]}" \
    clean verify dependency:build-classpath \
    -Dmdep.outputFile=target/runtime-classpath.txt -DincludeScope=runtime

# Deliberately exclude test-classes and test-scoped dependencies from this process.
"$JAVA_HOME/bin/java" -cp "$CONSUMER/target/text-batch-consumer-1.0-SNAPSHOT.jar:$(cat "$CONSUMER/target/runtime-classpath.txt")" \
    acceptance.TextBatchApplication
