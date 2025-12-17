# 📊 Hướng dẫn Import Database

Có 2 cách để import database vào PostgreSQL container.

## Cách 1: Tự động import khi khởi tạo (Khuyến nghị)

Docker Compose đã được cấu hình để tự động import file SQL khi tạo database lần đầu.

### Bước 1: Đảm bảo file SQL tồn tại

```bash
# Kiểm tra file SQL
ls -la "Cloud_SQL_Export_2025-12-16 (08_49_10).sql"
```

### Bước 2: Khởi động services

```bash
# Lần đầu tiên chạy, database sẽ tự động import
docker-compose up -d
```

**Lưu ý quan trọng:**
- Import tự động chỉ chạy khi database được tạo **LẦN ĐẦU TIÊN**
- Nếu volume `postgres_data` đã tồn tại, script sẽ KHÔNG chạy
- Để import lại, cần xóa volume cũ (xem bước dưới)

### Xóa volume và import lại

```bash
# Stop và xóa volumes
docker-compose down -v

# Start lại (sẽ tự động import)
docker-compose up -d

# Xem logs để kiểm tra import
docker-compose logs -f postgres
```

## Cách 2: Import thủ công sau khi khởi động

Nếu database đã chạy và bạn muốn import lại:

### Option A: Import trực tiếp từ host

```bash
# Import SQL file
docker exec -i cookshare-postgres psql -U cookshare_user -d cookshare_db < "Cloud_SQL_Export_2025-12-16 (08_49_10).sql"

# Trên Windows (PowerShell)
Get-Content "Cloud_SQL_Export_2025-12-16 (08_49_10).sql" | docker exec -i cookshare-postgres psql -U cookshare_user -d cookshare_db

# Trên Windows (CMD)
type "Cloud_SQL_Export_2025-12-16 (08_49_10).sql" | docker exec -i cookshare-postgres psql -U cookshare_user -d cookshare_db
```

### Option B: Copy file vào container rồi import

```bash
# Copy file vào container
docker cp "Cloud_SQL_Export_2025-12-16 (08_49_10).sql" cookshare-postgres:/tmp/init.sql

# Import từ trong container
docker exec -it cookshare-postgres psql -U cookshare_user -d cookshare_db -f /tmp/init.sql

# Xóa file tạm
docker exec cookshare-postgres rm /tmp/init.sql
```

### Option C: Sử dụng pgAdmin

1. Truy cập pgAdmin: http://localhost:5050
2. Đăng nhập với credentials trong `.env`
3. Kết nối đến database
4. Right-click vào database > **Restore**
5. Chọn file SQL và restore

## Kiểm tra import thành công

```bash
# Kết nối vào database
docker exec -it cookshare-postgres psql -U cookshare_user -d cookshare_db

# Liệt kê các tables
\dt

# Đếm số records trong một table (ví dụ: users)
SELECT COUNT(*) FROM users;

# Thoát
\q
```
