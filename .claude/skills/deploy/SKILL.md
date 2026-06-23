---
name: deploy
description: Automates deployment to VPS via SSH — clones or pulls the project repo then runs docker-compose up -d.
---

# Deploy

수동 배포 과정을 그대로 자동화한다.

## 수동 배포 순서 (참고)

1. `ssh ubuntu@ssh.gsmsv.site -p 21119`
2. 비밀번호 입력: `p9l9fV%E`
3. 프로젝트 clone 또는 pull
4. `docker compose up -d`

## 자동 배포

sshpass로 비밀번호를 자동 입력해서 위 과정을 한 번에 실행한다.

### sshpass 설치 확인

```bash
which sshpass || sudo apt-get install -y sshpass
```

### 배포 실행

```bash
sshpass -p 'p9l9fV%E' ssh -o StrictHostKeyChecking=no ubuntu@ssh.gsmsv.site -p 21119 << 'REMOTE'
set -e

if [ -d "/home/ubuntu/backend/.git" ]; then
  cd /home/ubuntu/backend
  git pull
else
  git clone https://github.com/neeews/backend /home/ubuntu/backend
  cd /home/ubuntu/backend
fi

docker compose up -d
REMOTE
```
