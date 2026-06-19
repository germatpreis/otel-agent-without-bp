#!/bin/bash

# Directory to search for JSON files
DIRECTORY="."

# Find all JSON files and validate them with jq
find "$DIRECTORY" -name "*.avsc" | while read -r file; do
  if ! jq empty "$file" 2>/dev/null; then
    echo "Invalid JSON in file: $file"
    jq empty "$file" 2>&1 | grep -v 'parse error'
  fi
done