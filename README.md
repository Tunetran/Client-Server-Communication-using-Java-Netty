# Client-Server Communication using Java & Netty

## Mô tả dự án

Dự án xây dựng hệ thống Client-Server sử dụng Java, Netty và JavaFX, cho phép xác thực thông điệp giữa client và server, đồng thời thực hiện quét subdomain theo wordlist. Hệ thống hỗ trợ mã hóa, ký số, thống kê thời gian thực và dashboard trực quan.

## Kiến trúc tổng thể

- **Client**: Ứng dụng JavaFX, nhập thông điệp, chọn khóa, gửi xác thực, nhận kết quả, hiển thị log và dashboard.
- **Server**: Ứng dụng Java sử dụng Netty (TCP) và SparkJava (REST API), xác thực dữ liệu, quét subdomain, lưu log, thống kê, cung cấp dashboard.
- **Giao tiếp**: TCP (Netty) cho xác thực, HTTP REST cho dashboard.

## Quy trình xử lý

### Phía Client
- Nhập thông điệp, chọn file private key (client) và public key (server).
- Ký số, mã hóa thông điệp, gửi lên server qua Netty.
- Nhận kết quả xác thực và kết quả quét subdomain.
- Hiển thị log, dashboard (biểu đồ thành công/thất bại, subdomain tồn tại/không tồn tại) cập nhật thời gian thực qua REST API.

### Phía Server
- Lắng nghe kết nối, nhận dữ liệu xác thực.
- Giải mã, xác thực chữ ký số.
- Nếu thành công: log, cập nhật số liệu, lưu khóa, quét subdomain, trả kết quả.
- Nếu thất bại: log, cập nhật số liệu, trả lỗi.
- Cập nhật số liệu và log cho dashboard qua REST API.

### Quét Subdomain
- Đọc wordlist, kiểm tra từng subdomain bằng DNS lookup.
- Cập nhật số lượng "Found" và "Not found" vào MetricsCollector theo thời gian thực.
- Trả kết quả về client và dashboard.

## Công nghệ sử dụng
- **Java 17+**
- **JavaFX** (giao diện client)
- **Netty** (TCP server/client)
- **SparkJava** (REST API)
- **RSA, AES, SHA256withRSA** (mã hóa, ký số)
- **SQLite** (lưu khóa, log)
- **Gson** (JSON), **SLF4J** (logging), **Maven** (quản lý phụ thuộc)
- **ExecutorService, AtomicInteger** (đa luồng, đếm số liệu an toàn)

## Hướng dẫn chạy dự án

### Build

```sh
cd Server
mvn clean package
cd ../Client
mvn clean package
```

### Chạy Server
```sh
cd Server
java -jar target/Server-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Chạy Client
```sh
cd Client
java -jar target/Client-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Dashboard
- Truy cập REST API: http://localhost:4567/stats
- Dashboard JavaFX: tab Dashboard trên client

## Tính năng nổi bật
- Xác thực, mã hóa thông điệp an toàn giữa client và server
- Quét subdomain theo wordlist, cập nhật realtime
- Dashboard trực quan: biểu đồ thành công/thất bại, subdomain tồn tại/không tồn tại
- Log realtime, lưu trữ khóa và log
- REST API cho tích hợp hệ thống khác

## Ý nghĩa các class chính

- **VerifyServerNetty**: Khởi động server, điều phối xác thực, gọi quét subdomain, cập nhật log và số liệu.
- **VerifyServerHandler**: Xử lý từng kết nối xác thực, giải mã, xác thực chữ ký số, log, trả kết quả.
- **SubdomainScan**: Quét subdomain, cập nhật số liệu và log theo thời gian thực.
- **MetricsCollector**: Quản lý số liệu thống kê (thành công/thất bại, found/not found, log).
- **StatsApi**: Cung cấp REST API trả về số liệu thống kê cho dashboard.
- **LogBroadcaster**: Quản lý và phát log cho dashboard hoặc client.
- **KeyDatabase**: Lưu trữ thông tin khóa vào database.
- **ClientUI**: Giao diện người dùng, gửi nhận dữ liệu xác thực, hiển thị log và dashboard.

## Bảo mật
- Không commit file private key, database thực lên repository public.
- Có thể mở rộng xác thực người dùng, mã hóa TLS, phân quyền truy cập.

## Đóng góp
- Fork, tạo pull request hoặc mở issue để đóng góp ý tưởng, sửa lỗi hoặc tính năng mới.

---

**Tác giả:** Tunetran