# Lumina: Manual Camera (Android)

Ứng dụng máy ảnh chụp thủ công chuẩn DSLR tối giản, tập trung vào chất lượng ảnh gốc, độ ổn định và trải nghiệm điều khiển xúc giác trực quan trên Android.

---

## 1. Báo cáo khảo sát công nghệ (Phase 0)

### 1.1 Khảo sát Skip (SwiftUI on Android) & Swift SDK for Android
- **Yêu cầu hệ thống của Skip (`skip.tools`)**: Skip yêu cầu bắt buộc môi trường **macOS và Xcode 16+** để biên dịch và đóng gói ứng dụng Android (`skip init --app-name`). Trên Windows và Linux, Skip chỉ hỗ trợ thư viện/framework ở mức thử nghiệm và **không thể build ứng dụng Android hoàn chỉnh**.
- **Camera2 & NDK / JNI**: Các tính năng điều khiển cảm biến sâu (`SENSOR_INFO_EXPOSURE_TIME_RANGE`, `DngCreator`, `StreamConfigurationMap`) là các API Java/Android SDK.
- **Quyết định kiến trúc**: Ứng dụng được triển khai với **100% Native Kotlin + Jetpack Compose + CameraX & Camera2 Interop**. Kiến trúc được phân tách rõ ràng (Clean MVVM), tách riêng toàn bộ logic tính toán thuần (Kelvin, EV, Shutter formatting, Gyroscope shake analysis, HDR Bracketing) để đảm bảo tính module hóa và độ tin cậy tối đa.

---

## 2. Tech Stack & Kiến trúc

- **Ngôn ngữ**: 100% Kotlin
- **Giao diện**: Jetpack Compose, Material 3 (Theme tối giản DSLR chuyên nghiệp, OLED Black `#000000`, Amber `#FFB300`, Green `#00E676`)
- **Camera Engine**: CameraX kết hợp `Camera2CameraControl` & `Camera2Interop`
- **Xử lý ảnh RAW/DNG**: `DngCreator`, `ExifInterface`
- **Cảm biến & ML**:
  - `SensorManager` (Gyroscope) phục vụ chế độ chống rung Enhanced RAW
  - Google ML Kit Face Detection nhận diện khuôn mặt, nụ cười và mắt mở cho Smart Selfie
- **Kiến trúc**: MVVM + StateFlow + Coroutines + Clean Architecture

---

## 3. Danh sách tính năng chi tiết

### 3.1 Điều khiển thủ công (Manual Controls)
1. **Shutter Speed (Tốc độ màn trập)**: Dải tốc độ từ rất nhanh (1/8000s, 1/4000s...) đến phơi sáng lâu (1s, 2s). Hiển thị dạng phân số hoặc giây. Chuyển đổi linh hoạt giữa Auto và Manual.
2. **ISO (Độ nhạy sáng)**: Điều chỉnh theo dải nhạy sáng của cảm biến (50, 100, 200, 400, 800, 1600, 3200, 6400).
3. **Manual Focus (Lấy nét thủ công)**: Thanh trượt mượt mà từ vô cực ($\infty$) đến cận cảnh (Macro). Nút AF chuyển lại lấy nét tự động tức thì.
4. **White Balance (Cân bằng trắng)**: Chỉnh nhiệt độ màu theo Kelvin (2000K – 10000K) hoặc chọn nhanh Preset (Daylight 5500K, Cloudy 6500K, Tungsten 3200K, Fluorescent 4000K, Shade 7500K, AWB).
5. **Exposure Bias (Bù trừ sáng EV)**: $\pm 3\text{ EV}$ với bước nhảy $1/3\text{ EV}$.
6. **Thanh thông số & Dial cuộn ngang**: Hiển thị dải thông số trên nút chụp, chạm để mở thanh trượt/thước đo mượt mà.

### 3.2 Định dạng lưu trữ & Cảm biến
- **Định dạng**: JPEG, RAW (DNG) và HEIC.
- **Hậu kỳ cảm biến (Post-processing)**: Off / Low / Standard (điều khiển `NOISE_REDUCTION_MODE` và `EDGE_MODE`).
- **Không gian màu**: sRGB và Display P3.
- **Ghi EXIF**: Tự động lưu đầy đủ ISO, Shutter Speed, Aperture, Focal Length, Camera Model vào ảnh qua MediaStore.

### 3.3 Công cụ hỗ trợ ngắm chụp (Aids & Overlays)
- **Focus Peaking**: Phân tích frame preview thời gian thực bằng bộ lọc cạnh Sobel, đánh dấu các vùng nét bằng viền xanh lá Neon rực rỡ.
- **Exposure Clipping (Zebra)**: Cảnh báo vùng cháy sáng (Highlights) bằng sọc đỏ và vùng bết đen (Shadows) bằng sọc xanh dương.
- **Biểu đồ Histogram thời gian thực**: Hỗ trợ chế độ Luma và RGB, cập nhật 60 FPS mà không làm giật preview.
- **Lưới ngắm (Grid)**: Tắt / 3x3 (Rule of Thirds) / 4x4 / Tỷ lệ vàng (Golden Ratio).

### 3.4 Chế độ chụp nâng cao
- **Bracketing (HDR)**: Chụp liên tiếp 3 tấm ở 3 mức EV ($-2\text{ EV}$, $0\text{ EV}$, $+2\text{ EV}$), **khóa cố định ISO** và thay đổi Shutter.
- **Enhanced RAW**: Sử dụng con quay hồi chuyển (Gyroscope) đo độ rung của tay theo thời gian thực để tính toán tốc độ màn trập an toàn, hạ ISO xuống thấp nhất có thể.
- **Smart Selfie**: Sử dụng camera trước kết hợp ML Kit Face Detection, đánh giá nụ cười (`smilingProbability`) và độ mở mắt (`leftEyeOpenProbability`, `rightEyeOpenProbability`) để lưu ảnh tối ưu.

### 3.5 Thư viện ảnh tích hợp (Gallery & EXIF)
- Xem lại ảnh dạng lưới Thumbnail và xem Full màn hình.
- Hiển thị bảng chi tiết EXIF (ISO, Tốc độ, Khẩu độ, Tiêu cự, Định dạng, Độ phân giải, Dung lượng file).
- Chia sẻ và xóa ảnh trực tiếp trong ứng dụng.

---

## 4. Hướng dẫn Build và Chạy ứng dụng

### 4.1 Yêu cầu môi trường
- JDK 17 (đã kiểm tra tương thích Microsoft OpenJDK 17)
- Android SDK (API Level 26 đến 34)

### 4.2 Lệnh build APK
Mở terminal trong thư mục dự án và chạy:

```bash
# Build APK Debug
.\gradlew.bat assembleDebug

# Chạy toàn bộ Unit Test logic thuần
.\gradlew.bat testDebugUnitTest
```

File APK sau khi build nằm tại:
`app/build/outputs/apk/debug/app-debug.apk`

### 4.3 Cài đặt lên thiết bị thật
Kết nối điện thoại Android qua cổng USB và bật USB Debugging:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 5. Kết quả kiểm thử (Unit Tests)

Đã hoàn thiện bộ Unit Test JUnit cho toàn bộ logic thuần tại `LuminaPureLogicTest.kt`:
1. `testShutterSpeedFormatting`: Định dạng tốc độ màn trập phân số và giây (`1/1000s`, `1/4s`, `1s`, `2s`, `AUTO`).
2. `testKelvinToRggbVector`: Quy đổi nhiệt độ Kelvin sang tỷ lệ kênh màu RGB (ánh sáng ấm 2800K thì Red > Blue, ánh sáng lạnh 8000K thì Blue > Red).
3. `testExposureBiasCalculation`: Tính toán các bước phơi sáng EV.
4. `testEnhancedRawAlgorithm`: Thuật toán chọn Shutter/ISO dựa trên độ rung con quay hồi chuyển.
5. `testBracketingManager`: Thuật toán chụp 3 tấm phơi sáng khóa ISO.

**Kết quả: 100% tests PASS, Build xanh hoàn toàn!**
