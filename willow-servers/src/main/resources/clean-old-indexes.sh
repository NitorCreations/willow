#!/bin/bash

if [ -z "$1" ]; then
  DAYS_TO_KEEP=15
else
  DAYS_TO_KEEP=$1
fi
log() {
 echo "$(date '+%Y%m%d-%H:%M:%S') $@"
}

for DAYS in $(seq $DAYS_TO_KEEP $(($DAYS_TO_KEEP + 15))); do
  SECS_AGO=$((3600 * 24 * $DAYS))
  EPOCH_AT_AGO=$(($(date +%s) - $SECS_AGO))
  INDEX=$(date +%Y-%m-%d --date="@$EPOCH_AT_AGO")

  if curl -sf -XHEAD http://localhost:9200/$INDEX; then
    curl -XDELETE http://localhost:9200/$INDEX
    log deleted index $INDEX
  else
    log index $INDEX does not exist
  fi
done
