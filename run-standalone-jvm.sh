#!/bin/bash

set -eu -o pipefail

MACHINE_NAME="demo"
CURR_REPO=$(dirname "$(realpath "$0")")

SCALA_CLI=scala

if [[ ! -x $(command -v "${SCALA_CLI}") ]]; then
  echo "Error: '${SCALA_CLI}' command not found. Please install Scala CLI or update SCALA_CLI." >&2
  exit 1
fi

TESTS=(
  Loops1Test
  Loops2Test
  Loops3Test
  Loops4Test
)

for test_script in "${TESTS[@]}"; do
  echo "Running ${test_script}..."

  log_dir="${CURR_REPO}/logs/${MACHINE_NAME}/standalone/jvm/${test_script}"
  mkdir -p "${log_dir}"

  for i in {0..49}; do
    scala run --platform jvm --scala-version 3.8.3 \
      "${CURR_REPO}/standalone/${test_script}.scala" 2>&1 \
      | tee "${log_dir}/$(printf %02d "${i}").log"
  done

  echo "Finished ${test_script} for libc-stdatomic"
done
popd
