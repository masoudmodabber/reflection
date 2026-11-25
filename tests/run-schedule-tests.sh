#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
javac -d "$tmp" app/src/main/java/com/personal/reflection/Schedule.java tests/ScheduleTest.java
java -cp "$tmp" com.personal.reflection.ScheduleTest
