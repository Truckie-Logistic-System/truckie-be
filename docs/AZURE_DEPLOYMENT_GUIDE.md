# Hướng Dẫn Deploy Backend & Database (Azure + Neon)

## 📋 Tổng Quan

Hướng dẫn này sẽ giúp bạn deploy **MIỄN PHÍ 100%**:
- **Backend**: Spring Boot app lên **Azure App Service (Free F1)**
- **Database**: PostgreSQL lên **Neon (Free Tier)**

---

## 🗄️ Bước 1: Tạo Database trên Neon (MIỄN PHÍ)

### 1.1 Đăng ký Neon

1. Truy cập [https://neon.tech](https://neon.tech)
2. Click **Sign Up** → Đăng nhập bằng GitHub/Google
3. Chọn **Free Plan** (0.5 GB storage, không cần thẻ tín dụng)

### 1.2 Tạo Project

1. Click **Create Project**
2. Điền thông tin:
   - **Project name**: `truckie-db`
   - **Region**: `Singapore` (ap-southeast-1) - gần Việt Nam nhất
   - **PostgreSQL version**: `15` hoặc `16`
3. Click **Create Project**

### 1.3 Lấy Connection String

Sau khi tạo xong, bạn sẽ thấy connection details:

```
Host: ep-xxx-xxx-123456.ap-southeast-1.aws.neon.tech
Database: neondb (hoặc đổi thành capstone-project)
User: neondb_owner
Password: ************ (copy password này)
```

**Connection String cho Spring Boot:**
```
jdbc:postgresql://ep-xxx-xxx-123456.ap-southeast-1.aws.neon.tech/neondb?sslmode=require
```

### 1.4 Tạo Database mới (tuỳ chọn)

1. Vào **Databases** tab trong Neon Console
2. Click **New Database**
3. Đặt tên: `capstone-project`

### 1.5 Chạy Migration (Liquibase)

Bạn có thể chạy migration từ local:

```bash
# Set environment variables
$env:DATABASE_URL = "jdbc:postgresql://ep-xxx.ap-southeast-1.aws.neon.tech/neondb?sslmode=require"
$env:DATABASE_USERNAME = "neondb_owner"
$env:DATABASE_PASSWORD = "your-password"

# Run migration
./gradlew update
```

---

## 🔧 Bước 2: Tạo Resource Group trên Azure

1. Đăng nhập vào [Azure Portal](https://portal.azure.com) với **Azure for Students**
2. Tìm kiếm "Resource groups" → Click **Create**
3. Điền thông tin:
   - **Subscription**: `Azure for Students`
   - **Resource group**: `truckie-rg`
   - **Region**: `Southeast Asia`
4. Click **Review + Create** → **Create**

---

## 🚀 Bước 3: Tạo Azure App Service (FREE F1)

### 3.1 Tạo App Service Plan

1. Tìm kiếm "App Service" → Click **Create** → **Web App**
2. Điền thông tin:

   **Basics:**
   - **Subscription**: Chọn subscription
   - **Resource group**: `truckie-rg`
   - **Name**: `truckie-be` (sẽ có URL: truckie-be.azurewebsites.net)
   - **Publish**: `Code`
   - **Runtime stack**: `Java 17`
   - **Java web server stack**: `Java SE (Embedded Web Server)`
   - **Operating System**: `Linux`
   - **Region**: `Southeast Asia`
   
   **Pricing plans:**
   - Click **Create new** App Service Plan
   - **Name**: `truckie-plan`
   - **Pricing plan**: 
     - 🎓 **Free F1** (miễn phí - khuyến nghị cho sinh viên/demo)
     - hoặc **Basic B1** (~$13/tháng - cho production)

3. Click **Review + Create** → **Create**

### 3.2 Cấu hình Environment Variables (Quan trọng!)

1. Vào App Service → **Settings** → **Environment variables**
2. Click **+ Add** để thêm các biến sau:

| Name | Value |
|------|-------|
| `SPRING_PROFILES_ACTIVE` | `azure` |
| `DATABASE_URL` | `jdbc:postgresql://ep-xxx.ap-southeast-1.aws.neon.tech/neondb?sslmode=require` |
| `DATABASE_USERNAME` | `neondb_owner` |
| `DATABASE_PASSWORD` | `(password từ Neon)` |
| `JWT_SECRET` | `(secret key của bạn - ít nhất 256 bits)` |
| `FRONTEND_URL` | `https://truckie.vercel.app` |
| `CORS_ALLOWED_ORIGINS` | `https://truckie.vercel.app` |
| `MAIL_USERNAME` | `(email của bạn)` |
| `MAIL_PASSWORD` | `(app password)` |
| `GOOGLE_CLIENT_ID` | `(nếu dùng OAuth)` |
| `GOOGLE_CLIENT_SECRET` | `(nếu dùng OAuth)` |

> ⚠️ **Lưu ý**: Thay `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` bằng thông tin từ Neon Console

3. Click **Apply** → **Confirm**

### 3.3 Cấu hình Startup Command (Quan trọng!)

1. Vào **Settings** → **Configuration** → **General settings**
2. **Startup Command**:
```bash
java -jar /home/site/wwwroot/app.jar --spring.profiles.active=azure
```

---

## 🔐 Bước 4: Thiết lập CI/CD với GitHub Actions

### 4.1 Tạo Azure Service Principal

Chạy lệnh sau trong **Azure Cloud Shell** hoặc **Azure CLI**:

```bash
az ad sp create-for-rbac --name "truckie-github-actions" --role contributor \
    --scopes /subscriptions/{subscription-id}/resourceGroups/truckie-rg \
    --sdk-auth
```

Thay `{subscription-id}` bằng ID subscription của bạn.

Lệnh sẽ trả về JSON:
```json
{
  "clientId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "clientSecret": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "subscriptionId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "tenantId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  ...
}
```

### 4.2 Thêm Secret vào GitHub Repository

1. Vào GitHub repo → **Settings** → **Secrets and variables** → **Actions**
2. Click **New repository secret**
3. **Name**: `AZURE_CREDENTIALS`
4. **Value**: Paste toàn bộ JSON ở trên
5. Click **Add secret**

### 4.3 Cập nhật Workflow (nếu cần)

File workflow đã được tạo tại `.github/workflows/azure-deploy.yml`. 
Đổi tên `AZURE_WEBAPP_NAME` nếu App Service của bạn có tên khác.

---

## 📦 Bước 5: Deploy thủ công (Cách 1 - Azure CLI)

Nếu không dùng GitHub Actions, bạn có thể deploy bằng Azure CLI:

```bash
# Build project
./gradlew clean build -x test

# Login Azure
az login

# Deploy
az webapp deploy --resource-group truckie-rg --name truckie-be \
    --src-path build/libs/app.jar --type jar
```

---

## 🐳 Bước 5 Alternative: Deploy bằng Docker (Cách 2)

### 5.1 Tạo Azure Container Registry

```bash
# Tạo Container Registry
az acr create --resource-group truckie-rg --name truckieacr --sku Basic

# Login vào registry
az acr login --name truckieacr

# Build và push image
docker build -t truckieacr.azurecr.io/truckie-be:latest .
docker push truckieacr.azurecr.io/truckie-be:latest
```

### 5.2 Tạo Web App for Containers

1. Tạo App Service với **Publish**: `Docker Container`
2. **Image Source**: `Azure Container Registry`
3. Chọn image vừa push

---

## ✅ Bước 6: Kiểm tra Deployment

### 6.1 Kiểm tra logs

1. Vào App Service → **Monitoring** → **Log stream**
2. Hoặc sử dụng Azure CLI:
```bash
az webapp log tail --resource-group truckie-rg --name truckie-be
```

### 6.2 Kiểm tra health

Truy cập các URL:
- Health check: `https://truckie-be.azurewebsites.net/actuator/health`
- Swagger UI: `https://truckie-be.azurewebsites.net/swagger-ui.html`

---

## 💰 Chi phí & Azure for Students

### 🎓 Azure for Students (KHUYẾN NGHỊ)

Khi đăng ký [Azure for Students](https://azure.microsoft.com/free/students/), bạn nhận được:
- ✅ **$100 credit** miễn phí (không cần thẻ tín dụng)
- ✅ **12 tháng** dịch vụ miễn phí
- ✅ Gia hạn mỗi năm khi còn là sinh viên

### 📊 Option 1: Tiết kiệm nhất với $100 credit

| Service | Tier | Giá/tháng | Thời gian dùng với $100 |
|---------|------|-----------|------------------------|
| App Service | **Free F1** | **$0** | ♾️ Miễn phí mãi |
| PostgreSQL Flexible | Burstable B1ms | ~$12 | ~8 tháng |
| **Tổng** | | **~$12/tháng** | **~8 tháng** |

### 📊 Option 2: Performance tốt hơn

| Service | Tier | Giá/tháng | Thời gian dùng với $100 |
|---------|------|-----------|------------------------|
| App Service | Basic B1 | ~$13 | ~4 tháng |
| PostgreSQL Flexible | Burstable B1ms | ~$12 | ~4 tháng |
| **Tổng** | | **~$25/tháng** | **~4 tháng** |

### ⚠️ Giới hạn của Free F1 App Service:
- 60 phút CPU/ngày
- 1 GB RAM, 1 GB storage
- Không có custom domain SSL
- Có thể bị "cold start" (chậm khi lâu không dùng)

**👉 Khuyến nghị**: Dùng **Free F1** cho development/demo, upgrade lên **Basic B1** khi cần production

### 🆓 Option 3: Hoàn toàn MIỄN PHÍ (Dùng dịch vụ khác cho DB)

Nếu muốn **100% miễn phí**, bạn có thể:

| Service | Platform | Giá |
|---------|----------|-----|
| Backend | Azure App Service **Free F1** | **$0** |
| Database | [Neon PostgreSQL](https://neon.tech) Free tier | **$0** (0.5GB) |
| Database | [Supabase](https://supabase.com) Free tier | **$0** (500MB) |
| Database | [Railway](https://railway.app) | **$5 credit/tháng** |

**Cách dùng Neon/Supabase**: Chỉ cần thay connection string trong environment variables

---

## 🔧 Troubleshooting

### Lỗi kết nối database
1. Kiểm tra firewall của PostgreSQL server
2. Đảm bảo đã bật "Allow Azure services" 
3. Kiểm tra connection string và credentials

### Lỗi application không start
1. Kiểm tra logs: App Service → Log stream
2. Đảm bảo SPRING_PROFILES_ACTIVE=azure
3. Kiểm tra tất cả environment variables

### Lỗi out of memory
1. Upgrade App Service plan
2. Thêm JVM options: `-Xmx512m -Xms256m`

---

## 📚 Tài liệu tham khảo

- [Azure App Service for Java](https://docs.microsoft.com/azure/app-service/quickstart-java)
- [Azure Database for PostgreSQL](https://docs.microsoft.com/azure/postgresql/flexible-server/)
- [GitHub Actions for Azure](https://docs.microsoft.com/azure/app-service/deploy-github-actions)
