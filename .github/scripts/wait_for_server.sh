#!/bin/bash
# License: BSD-3-Clause
# Wait until the Josh server is accepting connections on port 8085, then exit 0.
# If the server has not started within 30 seconds, print the log and exit 1.
#
# Usage: wait_for_server.sh <log_file>

set -e

LOG_FILE="$1"

if echo "$LOG_FILE" | grep -q "profiler"; then
  LABEL="Profiler server"
else
  LABEL="Server"
fi

for i in $(seq 1 30); do
  if curl -s --max-time 1 --connect-timeout 1 http://localhost:8085 -o /dev/null 2>&1; then
    echo "$LABEL ready after $i seconds"
    exit 0
  fi
  echo "Waiting for $LABEL... ($i/30)"
  sleep 1
  if [ "$i" -eq 30 ]; then
    echo "$LABEL failed to start. Log:"
    cat "$LOG_FILE"
    exit 1
  fi
done
