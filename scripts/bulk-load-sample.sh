#!/usr/bin/env bash
set -euo pipefail

ES_URL="${ES_URL:-http://localhost:9200}"
ES_USER="${ES_USER:-elastic}"
ES_PASSWORD="${ES_PASSWORD:-elastic}"
INDEX="${INDEX:-sample-index}"
TOTAL_DOCS="${1:-10000}"
BATCH_SIZE="${BATCH_SIZE:-1000}"

if ! [[ "$TOTAL_DOCS" =~ ^[0-9]+$ ]] || [ "$TOTAL_DOCS" -lt 1 ]; then
  echo "TOTAL_DOCS must be a positive integer"
  exit 1
fi

tmpfile="$(mktemp)"
trap 'rm -f "$tmpfile"' EXIT

echo "Loading $TOTAL_DOCS documents into index '$INDEX' at $ES_URL"

counter=1
while [ "$counter" -le "$TOTAL_DOCS" ]; do
  : > "$tmpfile"
  upper_limit=$((counter + BATCH_SIZE - 1))
  if [ "$upper_limit" -gt "$TOTAL_DOCS" ]; then
    upper_limit=$TOTAL_DOCS
  fi

  i=$counter
  while [ "$i" -le "$upper_limit" ]; do
    category_id=$((i % 5))
    price=$((10 + (i % 200)))
    quantity=$((1 + (i % 50)))
    cat="category-${category_id}"
    echo '{"index":{"_index":"'"$INDEX"'","_id":"doc-'"$i"'"}}' >> "$tmpfile"
    echo '{"name":"product-'"$i"'","category":"'"$cat"'","price":'"$price"',"quantity":'"$quantity"',"tags":["bulk","generated","'"$cat"'"],"createdAt":"'"$(date -u +"%Y-%m-%dT%H:%M:%SZ")"'"}' >> "$tmpfile"
    i=$((i + 1))
  done

  curl -sS -u "${ES_USER}:${ES_PASSWORD}" \
    -H "Content-Type: application/x-ndjson" \
    -X POST "${ES_URL}/_bulk?refresh=true" \
    --data-binary "@${tmpfile}" > /dev/null

  echo "Inserted documents $counter..$upper_limit"
  counter=$((upper_limit + 1))
done

echo "Done."
