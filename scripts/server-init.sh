#!/bin/bash
# DigitalOcean 서버 초기화 스크립트 (1회만 실행)
set -e

echo "=== [1/6] 패키지 업데이트 ==="
apt-get update -y && apt-get upgrade -y

echo "=== [2/6] Docker 설치 ==="
apt-get install -y ca-certificates curl gnupg
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  > /etc/apt/sources.list.d/docker.list
apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

echo "=== [3/6] Docker Compose (standalone) 설치 ==="
curl -SL "https://github.com/docker/compose/releases/download/v2.24.6/docker-compose-linux-x86_64" \
  -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
docker-compose --version

echo "=== [4/6] 프로젝트 디렉토리 생성 ==="
mkdir -p /root/auto-trading

echo "=== [5/6] 방화벽 설정 ==="
ufw allow OpenSSH
ufw allow 8080/tcp   # 앱 포트 (필요시 특정 IP로 제한 가능)
ufw --force enable
ufw status

echo "=== [6/6] Docker 서비스 자동시작 설정 ==="
systemctl enable docker
systemctl start docker

echo ""
echo "✅ 서버 초기화 완료!"
echo "다음 단계: /root/auto-trading/.env 파일을 생성하세요"
echo "  scp .env root@143.198.207.81:/root/auto-trading/.env"