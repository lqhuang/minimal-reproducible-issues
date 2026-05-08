#!/bin/bash

set -eu -o pipefail

MACHINE_NAME="demo"
#CURR_REPO="/path/tor/your/minimal-reproducible-issues"
# or you can use current dir as working dir directly
CURR_REPO=$(dirname "$(realpath "$0")")
SCALA_NATIVE_REPO="/absolute/path/to/your/src/of/scala-native"

TESTS=(
  Loops1Test
  Loops2Test
  Loops3Test
  Loops4Test
)

pushd "${SCALA_NATIVE_REPO}"
git reset --hard
git checkout c69e04ce8e75d3994fad8010f14a608288d629f0
popd

# By default, the Scala Native repo contains the libc-stdatomic implementation

pushd "${SCALA_NATIVE_REPO}"
for test_script in "${TESTS[@]}"; do
  echo "Running ${test_script}..."

  log_dir="${CURR_REPO}/logs/${MACHINE_NAME}/standalone/libc-stdatomic/${test_script}"
  mkdir -p "${log_dir}"

  cat "${CURR_REPO}/standalone/${test_script}.scala" > "${SCALA_NATIVE_REPO}/sandbox/src/main/scala/Test.scala"

  for i in {0..49}; do
    sbt "++3.8.3; \
          set ThisBuild/nativeConfig ~= { _.withMultithreading(true).withMode(scala.scalanative.build.Mode.releaseFast) }; \
          show sandbox3/nativeConfig; \
          sandbox3/run" \
      | tee "${log_dir}/$(printf %02d "${i}").log"
  done

  echo "Finished ${test_script} for libc-stdatomic"
done
popd

# Copy the JUC Atomic implementation to the Scala Native repo
# and repeat the tests with it

pushd "${SCALA_NATIVE_REPO}"
git reset --hard
git checkout c69e04ce8e75d3994fad8010f14a608288d629f0
cat "${CURR_REPO}/impl/SubmissionPublisherImplJUCAtomic.scala" > "${SCALA_NATIVE_REPO}/javalib/src/main/scala/java/util/concurrent/SubmissionPublisher.scala"
popd

pushd "${SCALA_NATIVE_REPO}"
for test_script in "${TESTS[@]}"; do
  echo "Running ${test_script}..."

  log_dir="${CURR_REPO}/logs/${MACHINE_NAME}/standalone/juc-atomic/${test_script}"
  mkdir -p "${log_dir}"

  cat "${CURR_REPO}/standalone/${test_script}.scala" > "${SCALA_NATIVE_REPO}/sandbox/src/main/scala/Test.scala"

  for i in {0..49}; do
    sbt "++3.8.3; \
          set ThisBuild/nativeConfig ~= { _.withMultithreading(true).withMode(scala.scalanative.build.Mode.releaseFast) }; \
          show sandbox3/nativeConfig; \
          sandbox3/run" \
      | tee "${log_dir}/$(printf %02d "${i}").log"
  done

  echo "Finished ${test_script} for JUC Atomic"
done
popd
