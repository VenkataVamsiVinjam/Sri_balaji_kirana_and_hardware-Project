#!/usr/bin/env bash
# ==========================================================================
# deploy.sh
# Run from your LOCAL machine (or CI) after server-setup.sh has already
# been run once on the VPS. Builds the jar, copies it over SSH, restarts
# the systemd service.
#
# Usage: ./deploy.sh user@your-vps-ip-or-domain
# ==========================================================================
set -euo pipefail

if [ -z "${1:-}" ]; then
    echo "Usage: $0 user@vps-host"
    exit 1
fi

VPS_TARGET="$1"
REMOTE_DIR="/opt/erp"

echo "==> Building the jar (skipping tests for speed - run 'mvn verify' separately in CI)"
mvn -B -DskipTests clean package

JAR_PATH="target/erp.jar"
if [ ! -f "$JAR_PATH" ]; then
    echo "Build failed - $JAR_PATH not found."
    exit 1
fi

echo "==> Copying jar to $VPS_TARGET:$REMOTE_DIR"
scp "$JAR_PATH" "$VPS_TARGET:/tmp/erp-new.jar"

echo "==> Installing jar and restarting service on the VPS"
ssh "$VPS_TARGET" bash -s << 'REMOTE_SCRIPT'
set -euo pipefail
sudo mv /tmp/erp-new.jar /opt/erp/erp.jar
sudo chown erp:erp /opt/erp/erp.jar
sudo systemctl restart erp
sleep 3
sudo systemctl status erp --no-pager -l | head -20
REMOTE_SCRIPT

echo "==> Done. Tail logs with: ssh $VPS_TARGET 'sudo journalctl -u erp -f'"
