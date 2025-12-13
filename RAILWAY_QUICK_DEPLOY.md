# 🚀 Quick Deploy Guide - Railway CLI

## Bước 1: Cài Railway CLI

**PowerShell (Run as Administrator):**
```powershell
# Install
iwr https://railway.app/install.ps1 | iex

# Verify
railway --version
```

**Nếu lỗi, dùng NPM:**
```powershell
npm install -g @railway/cli
railway --version
```

---

## Bước 2: Deploy lên Railway

```powershell
# Di chuyển vào folder backend
cd d:\capstone\capstone-be

# Login Railway (mở browser để authorize)
railway login

# Link vào project có sẵn hoặc tạo mới
railway link

# Deploy ngay (Railway tự build Dockerfile)
railway up

# Xem logs real-time
railway logs -f
```

**Railway CLI sẽ:**
1. ✅ Upload code lên Railway
2. ✅ Tự động detect Dockerfile
3. ✅ Build Docker image với Gradle
4. ✅ Deploy container
5. ✅ Cấp domain tự động

---

## Bước 3: Cấu hình Environment Variables

**Cách 1: Qua CLI**
```powershell
railway variables set DATABASE_URL="jdbc:postgresql://..."
railway variables set JWT_SECRET="your-secret-min-32-chars"
railway variables set FRONTEND_URL="https://your-frontend.vercel.app"
```

**Cách 2: Qua Dashboard** (Khuyên dùng)
1. Vào https://railway.app
2. Chọn Project → Service → **Variables**
3. Add các biến cần thiết (xem list dưới)

---

## Environment Variables cần thiết

```bash
# Database (Railway tự inject nếu có PostgreSQL service)
DATABASE_URL=${{Postgres.DATABASE_URL}}
PGUSER=${{Postgres.PGUSER}}
PGPASSWORD=${{Postgres.PGPASSWORD}}

# App
APP_URL=https://your-app.up.railway.app
FRONTEND_URL=https://your-frontend.vercel.app
PORT=8080

# Google OAuth
GOOGLE_CLIENT_ID=358401289952-t9k9oelrg00tcb3a63jiv9pcaumb1dia.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=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

# Email
MAIL_USERNAME=sonnnse182328@fpt.edu.vn
MAIL_PASSWORD=rvna efft nmog mlbi

# JWT (QUAN TRỌNG: Tạo secret mới!)
JWT_SECRET=m9fP0TyWJ1tF3z2q8rB7tG6+KoW8I8sLK8JiwUEaUO8=

# Redis (Azure)
REDIS_HOST=truckie2025.redis.cache.windows.net
REDIS_PORT=6380
REDIS_USERNAME=default
REDIS_PASSWORD=OEWn02c76Z8qSdQBuCH38qw5OWCSHeasjAzCaCm937g=
REDIS_SSL=true

# FCM
FCM_SENDER_ID=1074005794111
```

---

## Bước 4: Provision PostgreSQL

**Nếu chưa có database:**
```powershell
# Add PostgreSQL service vào project
railway add postgresql

# Hoặc qua Dashboard:
# Project → New → Database → PostgreSQL
```

**Import schema:**
```powershell
# Connect vào PostgreSQL
railway connect postgres

# Trong psql shell, import file SQL
\i path/to/your-schema.sql

# Exit
\q
```

---

## Commands hữu ích

```powershell
# Deploy
railway up

# Xem logs
railway logs
railway logs -f  # Follow mode

# Restart service
railway restart

# Xem status
railway status

# Get domain
railway domain

# Connect database
railway connect postgres

# Run command in Railway environment
railway run ./gradlew bootRun

# Unlink project
railway unlink

# Delete service
railway delete
```

---

## Troubleshooting

### 1. Build quá lâu (>10 phút)

**Nguyên nhân:** Gradle download dependencies lần đầu

**Fix:** Chờ, lần build sau sẽ nhanh hơn (Railway cache)

### 2. Lỗi "Out of Memory"

**Nguyên nhân:** Free tier chỉ 512MB RAM

**Fix:** Đã optimize trong Dockerfile:
```dockerfile
ENV JAVA_OPTS="-Xmx400m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
```

### 3. Lỗi "Database connection failed"

**Fix:**
```powershell
# Check database service đang chạy
railway status

# Check environment variables
railway variables

# Restart cả 2 services
railway restart
```

### 4. Lỗi "jar file not found"

**Fix:** Đã fix bằng Dockerfile builder, không cần startCommand

### 5. Build fail do network

**Fix:**
```powershell
# Retry
railway up --detach

# Hoặc push lên GitHub, Railway auto deploy
git push origin master
```

---

## Auto Deploy từ GitHub

**Setup:**
1. Railway Dashboard → Service → **Settings**
2. **Source** → Connect GitHub repository
3. Chọn branch `master`
4. ✅ Enable "Auto Deploy"

**Từ giờ:** Mỗi lần push code lên GitHub, Railway tự động deploy!

---

## Check Deployment Success

```powershell
# Get app URL
railway domain

# Test health endpoint
curl https://your-app.up.railway.app/actuator/health

# Test Swagger
Start-Process "https://your-app.up.railway.app/swagger-ui.html"
```

---

## Cost Management (Free Tier)

**Railway Free Tier:**
- 💰 $5 credit/month
- ⏱️ ~500 giờ runtime
- 💾 512MB RAM
- 📦 1GB storage

**Ước tính:** Đủ chạy 24/7 trong ~20 ngày/tháng

**Xem usage:**
```powershell
railway status
```

---

## Next Steps

1. ✅ Deploy backend thành công
2. ✅ Setup database schema
3. ✅ Test API endpoints
4. ✅ Update frontend API URL
5. ✅ Setup monitoring & alerts
6. ✅ Configure custom domain (optional)

---

**Thành công! Backend đã chạy trên Railway 🎉**

URL: `https://capstone-be-production.up.railway.app`
