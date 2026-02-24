#!/bin/bash
# =============================================================================
# run-api-tests.sh
# Starts the PetClinic stack, runs API tests, then shuts everything down.
# Usage:  ./run-api-tests.sh
# =============================================================================

set -euo pipefail

# ── Config ────────────────────────────────────────────────────────────────────
BASE_URL="${BASE_URL:-http://localhost:8080}"
HEALTH_ENDPOINT="${BASE_URL}/api/customer/owners"
MAX_WAIT_SECONDS=120
POLL_INTERVAL=5
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ── Colours ───────────────────────────────────────────────────────────────────
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Colour

log()    { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()   { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()  { echo -e "${RED}[ERROR]${NC} $*"; }

# ── Teardown (always runs, even on failure) ───────────────────────────────────
teardown() {
  log "Shutting down containers..."
  docker compose down --remove-orphans
  log "Containers stopped."
}
trap teardown EXIT

# ── Step 1: Start containers ──────────────────────────────────────────────────
log "Starting PetClinic stack..."
docker compose up -d
log "Containers started."

# ── Step 2: Wait for API Gateway to be healthy ────────────────────────────────
log "Waiting for API Gateway to be ready at ${HEALTH_ENDPOINT}..."
elapsed=0
until curl --silent --fail "${HEALTH_ENDPOINT}" > /dev/null 2>&1; do
  if [ "${elapsed}" -ge "${MAX_WAIT_SECONDS}" ]; then
    error "API Gateway did not become ready within ${MAX_WAIT_SECONDS}s. Aborting."
    exit 1
  fi
  echo -n "."
  sleep "${POLL_INTERVAL}"
  elapsed=$((elapsed + POLL_INTERVAL))
done
echo ""
log "API Gateway is ready (after ${elapsed}s)."

# ── Step 3: Run API tests ─────────────────────────────────────────────────────
log "Running API tests..."
cd "${SCRIPT_DIR}"
BASE_URL="${BASE_URL}" ./mvnw verify -pl api-tests --no-transfer-progress
TEST_EXIT_CODE=$?

# ── Step 4: Report ────────────────────────────────────────────────────────────
echo ""
if [ "${TEST_EXIT_CODE}" -eq 0 ]; then
  log "✅  All tests passed."
else
  error "❌  Tests failed. See above for details."
  error "    Reports: api-tests/target/failsafe-reports/"
fi

exit "${TEST_EXIT_CODE}"
