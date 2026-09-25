#!/usr/bin/env bash
# Encode a keystore file to base64 so you can paste it into the
# KEYSTORE_BASE64 GitHub Secret (no newlines).
#
# Usage:
#   ./scripts/encode-keystore.sh /path/to/release.keystore
#
# Then copy the printed string into:
#   GitHub repo -> Settings -> Secrets and variables -> Actions -> New secret
#   Name:  KEYSTORE_BASE64
#   Value: <paste the base64 string>

set -euo pipefail

if [ "$#" -lt 1 ]; then
  echo "Usage: $0 <keystore-file>" >&2
  exit 1
fi

KEYSTORE="$1"
if [ ! -f "$KEYSTORE" ]; then
  echo "Error: file not found: $KEYSTORE" >&2
  exit 1
fi

# macOS `base64` needs -i and strips differently than GNU base64 (-w 0).
if [ "$(uname -s)" = "Darwin" ]; then
  base64 -i "$KEYSTORE" | tr -d '\n'
else
  base64 -w 0 "$KEYSTORE"
fi
echo
