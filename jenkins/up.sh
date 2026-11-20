#!/usr/bin/env bash
# Starts the local CI stand (Jenkins + agent + Selenium Grid + app). Run from anywhere: bash jenkins/up.sh
#   - finds the git repository Jenkins should clone (for a git worktree: the main .git folder)
#   - makes an SSH key pair for controller -> agent once (jenkins/secrets/, not in git)
#   - writes both values into jenkins/.env (not in git): docker compose reads it by itself,
#     so later `docker compose -f jenkins/docker-compose.yml logs / down / restart ...` just work
#   - docker compose up --build
set -euo pipefail
cd "$(dirname "$0")"

# Jobs clone this repository (branch stage2): only COMMITTED code reaches Jenkins, like on a real CI.
REPO_GIT_DIR="$(cd .. && git rev-parse --path-format=absolute --git-common-dir)"

mkdir -p secrets
if [ ! -f secrets/agent_key ]; then
  ssh-keygen -q -t ed25519 -N "" -C "jenkins-controller" -f secrets/agent_key
  echo "[up] new SSH key pair: jenkins/secrets/agent_key(.pub)"
fi

cat > .env <<EOF
# written by jenkins/up.sh - machine-specific, not in git. No secrets: a path and a PUBLIC key.
REPO_GIT_DIR=${REPO_GIT_DIR}
JENKINS_AGENT_SSH_PUBKEY=$(cat secrets/agent_key.pub)
EOF

docker compose up -d --build "$@"
echo
echo "Jenkins        http://localhost:8081   (first start: ~1 min until the UI answers)"
echo "Grid           http://localhost:4444/ui/"
echo "Grid browser   http://localhost:7900   (noVNC)"
echo "App            http://localhost:8080"
echo "Stop:          docker compose -f jenkins/docker-compose.yml down"
