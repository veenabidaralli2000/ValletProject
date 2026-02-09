#!/usr/bin/env bash
# Simple end-to-end smoke test against a running wallet-service.
# Usage:
#   ./scripts/smoke.sh                # uses http://localhost:8080
#   BASE=http://host:8080 ./scripts/smoke.sh
set -euo pipefail

BASE="${BASE:-http://localhost:8080}"
WALLET="${WALLET:-11111111-1111-1111-1111-111111111111}"

hr() { printf '\n\033[1;34m== %s ==\033[0m\n' "$*"; }
req() {
  local method="$1"; shift
  local path="$1"; shift
  local body="${1:-}"
  if [[ -n "$body" ]]; then
    curl -sS -X "$method" "$BASE$path" -H 'Content-Type: application/json' -d "$body" -w '\nHTTP %{http_code}\n'
  else
    curl -sS -X "$method" "$BASE$path" -w '\nHTTP %{http_code}\n'
  fi
}

hr "GET seeded wallet"
req GET "/api/v1/wallets/$WALLET"

hr "DEPOSIT 500"
req POST "/api/v1/wallet" "{\"walletId\":\"$WALLET\",\"operationType\":\"DEPOSIT\",\"amount\":500}"

hr "WITHDRAW 200"
req POST "/api/v1/wallet" "{\"walletId\":\"$WALLET\",\"operationType\":\"WITHDRAW\",\"amount\":200}"

hr "Error: wallet not found"
req GET "/api/v1/wallets/00000000-0000-0000-0000-000000000000"

hr "Error: insufficient funds"
req POST "/api/v1/wallet" "{\"walletId\":\"$WALLET\",\"operationType\":\"WITHDRAW\",\"amount\":99999999}"

hr "Error: invalid JSON"
curl -sS -X POST "$BASE/api/v1/wallet" -H 'Content-Type: application/json' -d '{broken json' -w '\nHTTP %{http_code}\n'

hr "Error: invalid operationType"
req POST "/api/v1/wallet" "{\"walletId\":\"$WALLET\",\"operationType\":\"TRANSFER\",\"amount\":1}"
