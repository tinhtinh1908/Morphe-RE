#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
: "${JAVA_HOME:?Use JDK 21 and set JAVA_HOME}"
mkdir -p "$root/build/policy-test"
"$JAVA_HOME/bin/javac" -d "$root/build/policy-test" "$root/extension/vn/dtinh/messenger/RegistrationPolicy.java" "$root/tests/RegistrationPolicyTest.java"
"$JAVA_HOME/bin/java" -cp "$root/build/policy-test" RegistrationPolicyTest
