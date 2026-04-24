#!/bin/sh
set -e

STORAGE_URL="${STORAGE_URL:-http://storage:5000}"

if [ -z "$SERVICE_ROLE_KEY" ]; then
  echo "ERROR: SERVICE_ROLE_KEY is not set"
  exit 1
fi

if [ -z "$BUCKETS" ]; then
  echo "ERROR: BUCKETS is not set"
  exit 1
fi

create_bucket() {
  local bucket_id="$1"
  local public="${2:-false}"

  local http_code
  http_code=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "${STORAGE_URL}/bucket" \
    -H "Authorization: Bearer ${SERVICE_ROLE_KEY}" \
    -H "Content-Type: application/json" \
    -d "{\"id\":\"${bucket_id}\",\"name\":\"${bucket_id}\",\"public\":${public}}")

  if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
    echo "bucket created: ${bucket_id} (public=${public})"
  elif [ "$http_code" = "409" ]; then
    echo "bucket already exists: ${bucket_id}"
  else
    echo "ERROR: failed to create bucket '${bucket_id}' - HTTP ${http_code}"
    exit 1
  fi
}

for bucket in $BUCKETS; do
  create_bucket "$bucket" "${BUCKET_PUBLIC:-false}"
done

echo "bucket initialization complete"
