#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Launch db emulator
current_dir="$(dirname "$0")"
source "$current_dir/envs_and_functions.sh"

PID=$(lsof -t -i :"${DATASTORE_PORT}" -s tcp:LISTEN)

if [ -z "$PID" ]; then
  echo "Starting Datastore emulator"
  nohup gcloud beta emulators datastore start \
    --host-port="${DATASTORE_FULL_ADDRESS}" \
    --project="${TEST_PROJECT_ID}" \
    --consistency=1 \
    --no-store-on-disk \
    > /tmp/mock-db-logs &
    waitport "$DATASTORE_PORT"
else
  echo "There is an instance of Datastore emulator already running"
fi