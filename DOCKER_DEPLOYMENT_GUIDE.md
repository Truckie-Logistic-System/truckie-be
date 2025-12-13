# 🐳 Hướng dẫn Build và Deploy Docker lên Railway

## Phương án 1: Deploy qua Railway CLI (Khuyên dùng)

### Bước 1: Cài Railway CLI

```powershell
# Windows - dùng PowerShell as Administrator
iwr https://railway.app/install.ps1 | iex

# Verify installation
railway --version
```

### Bước 2: Login Railway

```powershell
cd d:\capstone\capstone-be

# Login
railway login
# Trình duyệt sẽ mở để authorize

# Link project
railway link
# Chọn project "capstone-be" hoặc tạo mới
```

### Bước 3: Deploy Docker Image

```powershell
# Railway sẽ tự build Dockerfile và deploy
railway up

# Xem logs
railway logs
```

**Railway sẽ tự động:**
1. Đọc Dockerfile
2. Build image với Gradle
3. Tạo jar file
4. Deploy container

---

## Phương án 2: Build Local rồi Push (Nếu Railway fail)

### Bước 1: Build Docker Image Local

```powershell
cd d:\capstone\capstone-be

# Build image
docker build -t capstone-be:latest .

# Test local (optional)
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=railway capstone-be:latest
```

### Bước 2: Push lên Railway Registry

```powershell
# Login Railway Docker registry
railway login --docker

# Tag image
docker tag capstone-be:latest registry.railway.app/capstone-be:latest

# Push
docker push registry.railway.app/capstone-be:latest
```

---

## Phương án 3: Dùng GitHub Container Registry

### Bước 1: Tạo Personal Access Token

1. Vào GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Generate new token với quyền: `write:packages`, `read:packages`
3. Copy token

### Bước 2: Login GitHub Registry

```powershell
# Login (thay YOUR_TOKEN)
echo YOUR_TOKEN | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin
```

### Bước 3: Build và Push

```powershell
cd d:\capstone\capstone-be

# Build (thay YOUR_USERNAME)
docker build -t ghcr.io/YOUR_USERNAME/capstone-be:latest .

# Push
docker push ghcr.io/YOUR_USERNAME/capstone-be:latest
```

### Bước 4: Deploy từ Registry vào Railway

1. Vào Railway Dashboard → Project → Service
2. Click **Settings** → **Deploy**
3. Chọn **"Deploy from Image"**
4. Nhập: `ghcr.io/YOUR_USERNAME/capstone-be:latest`

---

## Troubleshooting

### Lỗi: "Unable to access jarfile"

**Nguyên nhân:** Gradle build không tạo jar hoặc đường dẫn sai

**Fix:**

```dockerfile
# Kiểm tra file được tạo trong builder stage
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle clean bootJar -x test && \
    ls -la build/libs/  # Debug: list files

# Copy với tên chính xác
COPY --from=builder /app/build/libs/app.jar truckie.jar
```

### Lỗi: Docker build chậm

**Fix:** Thêm `.dockerignore`

```bash
# Tạo file .dockerignore
echo ".git
.gradle
build
bin
.idea
*.log
*.md" > .dockerignore
```

### Lỗi: Out of memory khi build

**Fix:** Tăng Docker memory
- Docker Desktop → Settings → Resources → Memory: 4GB+

---

## Test Local trước khi Deploy

### 1. Build Image

```powershell
docker build -t capstone-be-test .
```

### 2. Run với Environment Variables

```powershell
docker run -p 8080:8080 `
  -e DATABASE_URL="jdbc:postgresql://host.docker.internal:5432/capstone-project" `
  -e PGUSER="postgres" `
  -e PGPASSWORD="postgres" `
  -e JWT_SECRET="your-secret" `
  -e SPRING_PROFILES_ACTIVE="railway" `
  capstone-be-test
```

### 3. Test API

```powershell
# Health check
curl http://localhost:8080/actuator/health

# Swagger
Start-Process "http://localhost:8080/swagger-ui.html"
```

---

## Deploy với GitHub Actions (CI/CD)

### Tạo file `.github/workflows/deploy-railway.yml`

```yaml
name: Deploy to Railway

on:
  push:
    branches: [ master ]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Install Railway
        run: npm install -g @railway/cli
      
      - name: Deploy to Railway
        run: railway up --service backend
        env:
          RAILWAY_TOKEN: ${{ secrets.RAILWAY_TOKEN }}
```

**Setup:**
1. Railway → Project → Settings → Tokens → Create Token
2. GitHub → Repo → Settings → Secrets → New secret
   - Name: `RAILWAY_TOKEN`
   - Value: token từ Railway

---

## Kiểm tra Deployment

```powershell
# Check Railway logs
railway logs -f

# Check service status
railway status

# Get URL
railway domain
```

---

## Environment Variables cần thiết

Vào Railway Dashboard → Service → Variables, thêm:

```
DATABASE_URL=${{Postgres.DATABASE_URL}}
PGUSER=${{Postgres.PGUSER}}
PGPASSWORD=${{Postgres.PGPASSWORD}}

APP_URL=https://your-app.up.railway.app
FRONTEND_URL=https://your-frontend.vercel.app
PORT=8080

GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

MAIL_USERNAME=your-email
MAIL_PASSWORD=your-app-password

JWT_SECRET=your-jwt-secret-min-32-chars

REDIS_HOST=your-redis-host
REDIS_PORT=6380
REDIS_USERNAME=default
REDIS_PASSWORD=your-redis-password
REDIS_SSL=true

FCM_SENDER_ID=1074005794111
```

---

## Quick Commands

```powershell
# Build local
docker build -t capstone-be .

# Run local
docker run -p 8080:8080 capstone-be

# Deploy Railway
railway up

# View logs
railway logs -f

# Restart service
railway restart

# Connect to database
railway connect postgres
```

---

## Lưu ý

1. **Gradle cache:** Build lần đầu có thể mất 5-10 phút
2. **Railway limits:** Free tier 512MB RAM, cấu hình JVM đã optimize
3. **Database:** Nhớ provision PostgreSQL trên Railway trước
4. **Secrets:** KHÔNG commit file chứa secrets vào Git

---

**Deployment thành công! 🎉**
