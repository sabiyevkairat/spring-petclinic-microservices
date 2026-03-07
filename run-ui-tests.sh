#!/usr/bin/env bash
# run-ui-tests.sh — Start the MySQL stack and run the full UI test suite.
#
# Usage:
#   ./run-ui-tests.sh                    # headless Chrome (default)
#   HEADLESS=false ./run-ui-tests.sh     # watch tests run in browser

set -euo pipefail

COMPOSE_FILES="-f docker-compose.yml -f docker-compose.test.yml"
BASE_URL="${BASE_URL:-http://localhost:8080}"
HEADLESS="${HEADLESS:-true}"

# Load credentials from .env if present
if [ -f .env ]; then
  set -a && source .env && set +a
fi

echo "========================================"
echo " PetClinic UI Tests"
echo " Base URL : $BASE_URL"
echo " Headless : $HEADLESS"
echo "========================================"

# ── Start stack ───────────────────────────────────────────────────────────────
echo ""
echo "Starting PetClinic stack..."
docker compose $COMPOSE_FILES up -d > /dev/null 2>&1
echo "Containers started"

# ── Wait for UI to be reachable ───────────────────────────────────────────────
echo ""
echo "Waiting for frontend to be ready..."
READY=false
for i in $(seq 1 24); do
  if curl --silent --fail "$BASE_URL" > /dev/null 2>&1; then
    READY=true
    break
  fi
  sleep 5
done

if [ "$READY" = false ]; then
  echo "Frontend did not become ready within 120s — dumping logs"
  docker compose $COMPOSE_FILES logs --no-color
  docker compose $COMPOSE_FILES down --remove-orphans > /dev/null 2>&1
  exit 1
fi

echo "Frontend is ready"

# ── Run tests ─────────────────────────────────────────────────────────────────
echo ""
echo "Running UI tests..."
set +e
BASE_URL="$BASE_URL" HEADLESS="$HEADLESS" \
  ./mvnw verify -pl ui-tests --no-transfer-progress
TEST_EXIT_CODE=$?
set -e

# ── Tear down ────────────────────────────────────────────────────────────────
echo ""
echo "Stopping containers..."
docker compose $COMPOSE_FILES down --remove-orphans > /dev/null 2>&1
echo "All containers stopped"

# ── Result ────────────────────────────────────────────────────────────────────
echo ""
if [ $TEST_EXIT_CODE -eq 0 ]; then
  echo "UI tests passed"
else
  echo "UI tests failed - screenshots in ui-tests/target/screenshots/"
fi

exit $TEST_EXIT_CODE
