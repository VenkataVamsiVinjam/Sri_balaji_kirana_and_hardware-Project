# DevOps Guide: Sri Balaji Kirana and Hardware ERP

End-to-end process to take this codebase from a laptop to a live, HTTPS-secured
application on a Linux VPS with a real domain, plus a CI/CD pipeline so future
changes deploy safely.

Everything referenced here (`erp.service`, `erp.env.example`, `erp.nginx.conf`,
`server-setup.sh`, `deploy.sh`) lives in the `deploy/` folder of this project.

---

## 0. Prerequisites

| What | Why |
|---|---|
| JDK 17+, Maven 3.8+ | Build the app locally |
| MySQL 8 (local) | Dev database |
| A Linux VPS (Ubuntu 22.04/24.04), root/sudo access | Production host |
| A domain name pointed (A record) at the VPS's public IP | HTTPS + friendly URL |
| GitHub repo for this code | Version control + CI/CD |

---

## 1. Local Development Loop

```bash
# one-time
mysql -u root -p -e "CREATE DATABASE erp_dev CHARACTER SET utf8mb4;"

# edit src/main/resources/application-dev.properties with your local MySQL creds

# run
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
Visit `http://localhost:8080`, log in as `admin` / `Admin@123`, and change the
password immediately from the Users screen.

Iterate: edit code -> Spring Boot DevTools (already a dependency) hot-reloads on
recompile. `spring.jpa.hibernate.ddl-auto=update` in dev means new entity fields
auto-migrate the schema - fine for dev, **not** how we'll run prod long-term (see §5).

---

## 2. Version Control

```bash
cd erp
git init
git add .
git commit -m "Initial commit: Sri Balaji Kirana and Hardware ERP"
git branch -M main
git remote add origin https://github.com/<you>/<repo>.git
git push -u origin main
```

The included `.gitignore` already excludes `target/`, IDE files, and anything named
`erp.env` / `application-secrets.properties` so you never accidentally commit real
DB passwords or mail credentials.

---

## 3. CI Pipeline (GitHub Actions)

Already included at `.github/workflows/build.yml`. On every push/PR to `main` it:
1. Spins up a throwaway MySQL 8 service container
2. Runs `mvn clean verify` (compiles + runs tests against that DB)
3. Packages `target/erp.jar`
4. Uploads the jar as a build artifact you can download from the Actions tab

This gives you a safety net: if a change breaks the build, you find out on the PR,
not on the VPS.

There's a commented-out `deploy` job at the bottom of that file for **automatic**
deployment on every push to `main`. To turn it on:
1. In GitHub: **Settings > Secrets and variables > Actions**, add:
   - `VPS_HOST` — your server's IP or domain
   - `VPS_USER` — the SSH user you deploy with (not `erp`, a real sudo-capable user)
   - `VPS_SSH_KEY` — the private key for that user (generate a deploy-only keypair,
     don't reuse your personal one)
2. Uncomment the `deploy:` job block.
3. Push to `main` — it'll now build and auto-deploy.

Until you're comfortable with that, deploy manually with `deploy/deploy.sh` (§6) —
same effect, but you press the button.

---

## 4. One-Time VPS Bootstrap

SSH into your fresh VPS, then:

```bash
scp deploy/server-setup.sh youruser@your-vps-ip:/tmp/
ssh youruser@your-vps-ip
sudo bash /tmp/server-setup.sh
```

This installs Java 17, MySQL, Nginx, and Certbot; creates an unprivileged `erp`
system user (the app never runs as root); and creates `/opt/erp` + `/var/log/erp`.

Then follow the printed manual steps:

**a) Secure MySQL and create the prod database + user**
```bash
sudo mysql_secure_installation
sudo mysql -u root -p
```
```sql
CREATE DATABASE erp_prod CHARACTER SET utf8mb4;
CREATE USER 'erp_user'@'localhost' IDENTIFIED BY 'REPLACE_WITH_STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON erp_prod.* TO 'erp_user'@'localhost';
FLUSH PRIVILEGES;
```

**b) Set up secrets**
```bash
scp deploy/erp.env.example youruser@your-vps-ip:/tmp/erp.env
ssh youruser@your-vps-ip
sudo mv /tmp/erp.env /opt/erp/erp.env
sudo nano /opt/erp/erp.env      # fill in real DB password, mail credentials, domain
sudo chown erp:erp /opt/erp/erp.env
sudo chmod 600 /opt/erp/erp.env # only the erp user can read secrets
```

**c) Install the systemd service**
```bash
scp deploy/erp.service youruser@your-vps-ip:/tmp/
ssh youruser@your-vps-ip
sudo mv /tmp/erp.service /etc/systemd/system/erp.service
sudo systemctl daemon-reload
sudo systemctl enable erp   # start on boot
```
(Don't `start` it yet — there's no jar at `/opt/erp/erp.jar` until §6.)

**d) Nginx reverse proxy + free SSL**
```bash
scp deploy/erp.nginx.conf youruser@your-vps-ip:/tmp/
ssh youruser@your-vps-ip
sudo mv /tmp/erp.nginx.conf /etc/nginx/sites-available/erp
# edit it first: replace "yourdomain.com" with your real domain
sudo ln -s /etc/nginx/sites-available/erp /etc/nginx/sites-enabled/erp
sudo nginx -t && sudo systemctl reload nginx

sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com
# certbot edits the Nginx config to redirect HTTP->HTTPS and auto-renews via a systemd timer
```

**e) Firewall**
```bash
sudo ufw allow OpenSSH
sudo ufw allow 'Nginx Full'
sudo ufw enable
```

At this point: port 8443 (the app) is only reachable from `127.0.0.1` via Nginx's
reverse proxy — never exposed directly to the internet. Only 22 (SSH), 80, and 443
are open.

---

## 5. First Deploy

The very first deploy needs `DDL_AUTO=update` (already the default in
`erp.env.example`) so Hibernate creates all tables in the empty `erp_prod` database.

```bash
./deploy/deploy.sh youruser@your-vps-ip
```

This builds the jar locally, copies it to the VPS, and restarts the `erp` service.
Check it came up clean:
```bash
ssh youruser@your-vps-ip 'sudo systemctl status erp --no-pager -l'
ssh youruser@your-vps-ip 'sudo journalctl -u erp -n 100 --no-pager'
```

Visit `https://yourdomain.com` and log in with the default `admin` / `Admin@123`
account — **change the password immediately** from the Users screen.

**After confirming the schema looks right** (all tables present, app functions),
edit `/opt/erp/erp.env` on the VPS and switch:
```
DDL_AUTO=validate
```
then `sudo systemctl restart erp`. From now on, Hibernate will only *validate* that
entities match the schema and refuse to start if they don't — it will never
silently alter your production schema again. Future schema changes should go through
a proper migration (Flyway/Liquibase) rather than `ddl-auto`; this project ships with
`update` for zero-friction setup as requested, but `validate` is the safer long-term
posture for a live system with real financial data.

---

## 6. Every Deploy After That

```bash
git pull                       # get latest code
./deploy/deploy.sh youruser@your-vps-ip
```
Or just push to `main` if you enabled the GitHub Actions auto-deploy job (§3).

The systemd `Restart=on-failure` policy means if the new jar crashes on boot, it
retries a few times and then stops rather than looping forever — check
`sudo journalctl -u erp -f` if a deploy doesn't come back up.

---

## 7. Backups (Critical — this app holds real financial data)

Set up a nightly MySQL dump with rotation:

```bash
sudo mkdir -p /opt/erp/backups
sudo tee /opt/erp/backup-db.sh > /dev/null << 'EOF'
#!/usr/bin/env bash
set -euo pipefail
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
mysqldump -u erp_user -p"$(grep DB_PASSWORD /opt/erp/erp.env | cut -d= -f2)" erp_prod \
    | gzip > /opt/erp/backups/erp_prod_$TIMESTAMP.sql.gz
# keep last 30 days
find /opt/erp/backups -name "*.sql.gz" -mtime +30 -delete
EOF
sudo chmod +x /opt/erp/backup-db.sh
sudo chown erp:erp /opt/erp/backup-db.sh /opt/erp/backups

# schedule nightly at 2 AM
(sudo crontab -u erp -l 2>/dev/null; echo "0 2 * * * /opt/erp/backup-db.sh") | sudo crontab -u erp -
```

**Strongly recommended**: also copy these `.sql.gz` files off the VPS regularly
(e.g. `rsync` to another machine, or upload to cloud storage) — a backup that lives
only on the same disk as the database doesn't protect you if that disk fails.

---

## 8. Day-2 Operations Cheat Sheet

| Task | Command |
|---|---|
| Tail live logs | `ssh you@vps 'sudo journalctl -u erp -f'` |
| Restart app | `ssh you@vps 'sudo systemctl restart erp'` |
| Check app status | `ssh you@vps 'sudo systemctl status erp'` |
| Check Nginx config | `ssh you@vps 'sudo nginx -t'` |
| Manual DB backup | `ssh you@vps 'sudo -u erp /opt/erp/backup-db.sh'` |
| Restore a backup | `gunzip < backup.sql.gz \| mysql -u erp_user -p erp_prod` |
| SSL renewal (automatic, but to force) | `ssh you@vps 'sudo certbot renew'` |
| Rollback a bad deploy | Keep the previous `erp.jar` as `erp.jar.bak` before each `deploy.sh` run; `sudo cp erp.jar.bak erp.jar && sudo systemctl restart erp` |

---

## 9. Suggested Improvements As You Grow

- **Flyway or Liquibase** for versioned schema migrations instead of `ddl-auto`.
- **Prometheus + Grafana or a simple uptime monitor** (e.g. UptimeRobot) pinging
  `https://yourdomain.com/login` so you know immediately if the app goes down.
- **A staging environment** (a second, cheaper VPS or a separate systemd instance
  on port 8444 with its own DB) to test deploys before they hit production.
- **Log aggregation** if you outgrow `journalctl`/local files (e.g. ship
  `/var/log/erp/erp.log` to a log service).
