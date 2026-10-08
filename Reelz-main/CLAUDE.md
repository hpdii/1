# Donk — app mạng xã hội video

Bài tập lớn môn học. Hạn nộp **dưới 2 tuần** tính từ 2026-09-28. Huy sẽ **deploy online** sau khi xong.

Giao tiếp bằng **tiếng Việt**. Huy tự build và test trên điện thoại thật — không có emulator.

## Phạm vi — CHỐT CỨNG, không tự ý thêm

Đúng 5 tính năng: (1) đăng ký/đăng nhập, (2) đăng video kèm tiêu đề/mô tả,
(3) feed dọc phát video từ server, (4) like, (5) trang cá nhân.

**Đã cắt hẳn:** theo dõi, tìm kiếm, thông báo, nhắn tin. Comment chỉ làm nếu còn ngày đệm.

Đề bài bắt buộc: **CSDL quan hệ + framework cụ thể + REST API**. Vì vậy chọn
Spring Boot + MySQL, không được đổi sang Firebase hay giải pháp khác.

## Lệnh

Maven **không có trên PATH**, phải dùng đường dẫn đầy đủ:

```
cd D:\Donk\server
C:\apache-maven-3.9.11\bin\mvn.cmd spring-boot:run
```

Build kiểm tra nhanh (dùng cái này thay vì tin chẩn đoán của VS Code):

```
C:\apache-maven-3.9.11\bin\mvn.cmd -B -q compile
```

> Chẩn đoán "package org.springframework.boot does not exist" trong VS Code là
> **nhiễu classpath**, không phải lỗi thật. Maven mới là thẩm quyền.

## Bẫy đã phát hiện trên máy này

**Cổng 8080 đã bị chiếm** bởi `PEMHTTPD-x64` (PostgreSQL Enterprise Manager,
service tự khởi động). Server chạy **cổng 8081**. Không đụng vào service kia.

**Không đặt `server.address`** trong `application.properties`. Mặc định Spring
Boot bind `0.0.0.0` — đó là thứ điện thoại cần. Đặt `127.0.0.1` là nguyên nhân
số 1 của lỗi "localhost chạy được nhưng điện thoại không kết nối được".

**Wi-Fi đã ở profile `Private`**, firewall cổng 8081 đã mở (`Donk 8081`).
IP LAN hiện tại của máy: `192.168.0.104`.

## Trạng thái hạ tầng (đã hoàn thành 2026-09-28)

1. **CSDL `donk` đã tạo thành công** từ `schema.sql` (6 bảng: `users`, `videos`, `video_likes`, `video_bookmarks`, `user_follows`, `video_comments`).
2. **`application.properties` đã điền mật khẩu MySQL và Cloudinary** (`qloyy5c1`, API Key: `113475237214213`).
3. **Mạng & Firewall đã sẵn sàng**: Wi-Fi `Private`, firewall rule `Donk 8081` mở TCP 8081 Inbound.
4. **Đã test API chạy thật**: Ping, Auth, Video, Like, Bookmark, Follow, Following Feed, Search, Comments, Avatar, Cloudinary CDN... chạy và trả đúng định dạng JSON chuẩn.
5. **Đổi tên app thành Reelz**, logo chữ R neon gradient hiện đại, bảng màu TikTok/Reels siêu đẹp.
6. **Màn hình Login/Register đồng bộ Dark Mode**, icon Home & Play indicator hiển thị chuẩn xác, thanh tìm kiếm có nút quay lại `←` và tự động focus bàn phím.

## Quy ước code

- **Java 17 + Spring Boot 3.5.16**, Maven. Client Android viết bằng **Java**
  (không Kotlin), XML layout + `findViewById`.
- **Không dùng quan hệ JPA** (`@ManyToOne`…) — chỉ giữ cột `Long userId` trần.
  Lý do: tránh `LazyInitializationException`, Jackson đệ quy vô hạn
  (`User → videos → user`), và N+1 query. DTO dựng tường minh trong service.
- **Mọi lỗi trả cùng một dạng JSON** qua `GlobalExceptionHandler`. Bắt buộc:
  Gson bên client vỡ ngay khi gặp trang HTML lỗi hoặc body rỗng.
- **`createdAt` trong DTO là `String`** ISO-8601, không phải `LocalDateTime`.
- **`videoPath` là đường dẫn tương đối**, không có `/` đầu. Client ghép
  `BASE_URL + videoPath`. Không bao giờ để server nhúng URL tuyệt đối gắn cứng
  IP — IP LAN đổi là hỏng toàn bộ feed.
- **Bọc page trong object** (`PageResponse`), không serialize thẳng `Page<T>`.
- Comment trong code viết **không dấu** (theo lối cũ của Huy).

## Quyết định kiến trúc — đừng đảo ngược mà không hỏi

**`VideoStorageService` là nơi DUY NHẤT ghi/đọc file video.** Cố ý tách riêng
để khi deploy lên cloud chỉ đổi phần ruột lớp này sang S3/R2, không phải sửa
controller hay CSDL. Nền tảng deploy giá rẻ (Render, Railway) có ổ đĩa tạm —
file sẽ mất mỗi lần deploy.

**Video phục vụ qua static resource handler của Spring** tại `/media/**`, không
tự viết controller streaming. `ResourceHttpRequestHandler` tự cài HTTP
byte-range nên ExoPlayer mới tua được. Trả `ResponseEntity<Resource>` từ
`@GetMapping` chỉ cho ra 200 thường và **không có Range** — đó là bản hay hỏng.

**`/media/**` để công khai, không yêu cầu đăng nhập** — ExoPlayer khó gắn header
`Authorization` vào request media. Bù lại tên file là UUID. Đánh đổi có chủ ý,
cần ghi rõ trong báo cáo.

**Client sẽ dùng ExoPlayer/Media3**, không dùng `VideoView` như app Reelz cũ —
video giờ đến qua mạng.

## Git

- Danh tính: `hbl306 <daiquoc306@gmail.com>`. **Không được set vào git config** —
  truyền bằng `git -c user.name=... -c user.email=...` cho từng lệnh commit.
- Kết thúc commit message bằng:
  `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>`

## Tài liệu tham khảo

Kế hoạch đầy đủ (thiết kế CSDL, hợp đồng API, thứ tự thi công theo ngày, danh
sách rủi ro): `C:\Users\Huy\.claude\plans\stateless-zooming-wilkes.md`

App cũ **Reelz** ở `D:\VideoApp` là app cá nhân đang chạy tốt — **không sửa vào
đó**. Chỉ copy sang những file dùng lại được (theme, drawable, `BackgroundExecutor`,
`VideoGestureHandler`) rồi đổi package sang `com.donk.app`.
