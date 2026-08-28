#!/usr/bin/env bash
# ==========================================================================
# server-setup.sh
# Run ONCE on a fresh Ubuntu 22.04/24.04 VPS as root (or with sudo) to
# prepare it to host the ERP app. Safe to re-run (idempotent-ish).
#
# Usage: sudo bash server-setup.sh
# ==========================================================================
set -euo pipefail

echo "==> Updating package lists"
apt-get update -y

echo "==> Installing Java 17, MySQL, Nginx, Certbot, unzip"
apt-get install -y openjdk-17-jre-headless mysql-server nginx certbot python3-certbot-nginx unzip

echo "==> Creating dedicated 'erp' system user (no login shell, no home dir)"
if ! id "erp" &>/dev/null; then
    useradd --system --no-create-home --shell /usr/sbin/nologin erp
fi

echo "==> Creating /opt/erp and log directory"
mkdir -p /opt/erp
mkdir -p /var/log/erp
chown -R erp:erp /opt/erp /var/log/erp

echo "=================================================================="
echo " Next manual steps:"
echo "  1. Secure MySQL:            sudo mysql_secure_installation"
echo "  2. Create the DB + user:"
echo "       sudo mysql -u root -p"
echo "       CREATE DATABASE erp_prod CHARACTER SET utf8mb4;"
echo "       CREATE USER 'erp_user'@'localhost' IDENTIFIED BY 'STRONG_PASSWORD';"
echo "       GRANT ALL PRIVILEGES ON erp_prod.* TO 'erp_user'@'localhost';"
echo "       FLUSH PRIVILEGES;"
echo "  3. Copy erp.jar to /opt/erp/erp.jar (see deploy.sh)"
echo "  4. Copy erp.env.example to /opt/erp/erp.env, fill in real secrets,"
echo "       then: chmod 600 /opt/erp/erp.env && chown erp:erp /opt/erp/erp.env"
echo "  5. Copy erp.service to /etc/systemd/system/erp.service, then:"
echo "       sudo systemctl daemon-reload"
echo "       sudo systemctl enable erp"
echo "       sudo systemctl start erp"
echo "  6. Set up Nginx (see erp.nginx.conf) + certbot for SSL"
echo "  7. Open the firewall:"
echo "       sudo ufw allow OpenSSH"
echo "       sudo ufw allow 'Nginx Full'"
echo "       sudo ufw enable"
echo "=================================================================="
