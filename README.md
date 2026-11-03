2. Công nghệ sử dụng
   Hạng mục
   Lựa chọn đã chốt
   Ngôn ngữ / Framework
   Java, Spring Boot, Spring Data JPA, Spring Security
   Giao diện
   Thymeleaf + Bootstrap 5.3 + vanilla JS (không dùng jQuery nặng, không dùng framework JS)
   Cơ sở dữ liệu
   MariaDB, database LUMINOUS_DB, ddl-auto=validate (không dùng update để tự sửa schema)
   Bảo mật mật khẩu
   BCrypt
   Ảnh
   Ảnh local trong static/images/, không phụ thuộc Unsplash hay example.com
   Font
   "Be Vietnam Pro", fallback system-ui, Segoe UI, Arial
   Màu chủ đạo
   Xanh lá #456A41, xanh đậm #1D5420, xanh nhạt #E0F4E3, nền be #FFFCF4, nền kem #f7f3ec, đỏ giá khuyến mãi #d6334a
   Lưu ý quan trọng về phiên bản: Bản giao đầu viết cho Spring Boot
   3.3.5 / Java 21, nhưng classpath thật trên máy người dùng là
   Spring Boot 4.1.2 / Spring 7.0.10 / Hibernate 7.4.13 / Lombok
   1.18.48 / MariaDB 12.2.2, Java 25.0.2 trong IntelliJ. **Không cần
   đổi ngược về 3.3.5.** Mọi hướng dẫn sau này phải giữ nguyên pom
   và application.properties của người dùng, chỉ chép đè src/.
   Quyết định Thymeleaf thay JSP: Đề gốc ghi Java Servlet/JSP +
   Spring Data JPA, nhưng dự án thực tế dùng Thymeleaf vì template
   HTML thuần khớp Figma hơn, luồng Controller → Service → Repository
   → MariaDB giữ nguyên. Nếu giảng viên bắt buộc JSP thì chỉ chuyển
   template, không đổi luồng nghiệp vụ. File mẫu của người khác
   (HuynhDucPhu_22653551_SpringAI.rar, Flight Booking Assistant)
   chỉ lấy khung chatbot làm module cắm thêm sau này, không dùng làm
   khung chính, không nộp nguyên file đó.
3. Cấu trúc dự án
   src/main/java/com/luminous/
   config/SecurityConfig.java        # Phân quyền, login/logout, remember-me
   controller/
   HomeController.java             # Trang chủ, tìm kiếm, lọc danh mục
   MenuController.java             # Trang menu /menu
   AuthController.java             # Đăng nhập, đăng ký, captcha
   ... (các controller khác theo từng trang)
   service/
   ProductService.java             # Thẻ sản phẩm, ảnh local, sao, đã bán
   CustomUserDetailsService.java   # Tìm user theo email rồi số điện thoại
   model/                            # Entity khớp đúng bảng/cột trong SQL
   repository/                       # JpaRepository + method naming
   dto/ProductCard.java              # DTO thẻ sản phẩm (viết tay getter, không Lombok ở DTO này)

src/main/resources/
application.properties            # GIỮ NGUYÊN bản của người dùng (xem mục 4)
templates/
layout/header.html              # Fragment head, header, productCard
layout/footer.html              # Fragment footer + script
homepage/home.html              # Trang chủ
menu/menu.html                  # Trang menu riêng /menu
auth/login.html                 # Đăng nhập
auth/register.html              # Đăng ký
placeholder/                    # Các trang chờ (giỏ, blog, chính sách...)
static/
css/style.css                   # CSS CHUNG cho toàn hệ thống
css/home.css                    # CSS riêng trang chủ
css/menu.css                    # CSS riêng trang menu
css/auth.css                    # CSS riêng đăng nhập/đăng ký
js/main.js                      # JS chung: chuyển trang, menu, mật khẩu, modal
images/
logo.png                      # Logo (PNG, nền trong suốt)
banner/hero-01-ke-my-pham.jpg # Banner trang chủ (1 ảnh tĩnh)
banner/menu-visual.jpg        # Ảnh phải của trang menu
categories/category-*.jpg     # Ảnh danh mục hot
products/*.jpg                # Ảnh sản phẩm (xem mục 7)
4. Cấu hình chạy
   Database: MariaDB, database LUMINOUS_DB.
   Cổng DB trên máy người dùng: jdbc:mariadb://localhost:3307/LUMINOUS_DB
   (người dùng đã tự đổi sang 3307). Mọi hướng dẫn phải giữ nguyên
   cổng này, không bảo chép đè application.properties, không bảo
   đổi lại 3306.
   Các cấu hình khác trong application.properties: giữ nguyên
   bản của người dùng, gồm naming strategy Standard/LegacyJpa
   (để sửa lỗi missing column [avt_url] do đổi camelCase thành
   snake_case), ddl-auto=validate, Thymeleaf cache false khi dev.
   Chạy thử:
   mvn clean compile
   mvn spring-boot:run
   Sau đó mở trang chủ, bấm Ctrl+F5 nếu trình duyệt còn cache CSS/JS cũ.
5. Cơ sở dữ liệu
   File SQL gốc người dùng gửi: luminous_db.sql (39 KB, 602 dòng),
   database LUMINOUS_DB, MariaDB. Các bảng chính:
   Role, User, Address, Category, Product, ProductMedia,
   ProductVariant, Cart, CartItem, Orders, Payment,
   OrderDetail, VoucherType, Voucher, OrderVoucher,
   PointTransaction, Review, ReviewMedia, ReturnRequest,
   Notification, SkinQuiz, SkinQuizResult.
   5.1. Vai trò và tài khoản mẫu
   Role
   id
   Ghi chú
   ROLE_ADMIN
   1
   Quản trị
   ROLE_CUSTOMER
   2
   Khách hàng
   Tài khoản mẫu trong SQL (mật khẩu chung ghi trong header SQL là
   Luminous@123, hash BCrypt). **Chỉ dùng để test, không hiển thị
   dòng tài khoản mẫu trên giao diện đăng nhập** (dòng đó đã được
   xóa khỏi login.html theo yêu cầu):
   admin@luminous.vn — Bùi Ngọc Hiến, Admin
   thanhtruc@gmail.com — Ứng Thị Thanh Trúc, Khách hàng
   Ngoài ra còn: Nguyễn Trần Hải Đăng, Trần Gia Hân (Khách hàng)
   5.2. Danh mục và sản phẩm mẫu (dữ liệu thật, không tự bịa)
   Danh mục trong DB chỉ có 5:
   id
   Tên danh mục
   1
   Tẩy trang
   2
   Sữa rửa mặt
   3
   Serum & Dưỡng chất
   4
   Kem chống nắng
   5
   Kem dưỡng ẩm
   Sản phẩm trong DB có 8 sản phẩm, ProductVariant có 11 dòng:
   id
   Sản phẩm
   Thương hiệu
   1
   Nước tẩy trang Cocoon bí đao làm sạch dịu nhẹ
   COCOON
   2
   Sữa Rửa Mặt CeraVe Sạch Sâu Dành Cho Da Dầu
   CERAVE
   3
   Sữa Rửa Mặt Cetaphil Dịu Nhẹ Cho Da Nhạy Cảm
   CETAPHIL
   4
   Serum The Ordinary Niacinamide 10% + Zinc 1%
   THE ORDINARY
   5
   Serum Klairs Freshly Juiced Vitamin Drop
   KLAIRS
   6
   Kem Chống Nắng La Roche-Posay Anthelios UVMune 400
   LA ROCHE-POSAY
   7
   Gel Chống Nắng Skin Aqua UV Super Moisture Gel
   SKIN AQUA
   8
   Kem Dưỡng Ẩm CeraVe Moisturising Cream
   CERAVE
   Voucher thật trong DB: SUMMER2026, FREESHIP30, WELCOME10.
   5.3. Lệch giữa Figma và SQL — cách đã xử lý, không tự bịa
   Bảng User thiếu ngày sinh/giới tính: File migration
   001_add_profile_fields.sql tạo 2 cột nullable dateOfBirth,
   gender, bắt buộc chạy sau khi import SQL gốc thì 2 trường
   này mới được lưu. Dòng lưu ý về migration đã được xóa khỏi
   giao diện trang đăng ký, nhưng file migration vẫn giữ nguyên
   và logic lưu vẫn giữ nguyên.
   Đăng ký bằng số điện thoại: Email trong DB là NOT NULL
   UNIQUE nên khi đăng ký bằng phone, hệ thống tạo email nội bộ
   dạng phone_<số>@luminous.vn.
   Danh mục hot Figma có mục chưa có trong DB: Son môi,
   Phấn mắt, Cushion, Nước hoa hồng, Mặt nạ — nhấn vào hiện
   thông báo rõ "chưa có sản phẩm", không giả vờ có hàng.
   URL ảnh trong DB đều là example.com: Không tải được nên
   ProductService đổi sang ảnh local (xem mục 7). URL thật
   (không phải example.com) thì giữ nguyên.
   Bảng Review.rating: Kiểu trong DB là TINYINT nên entity
   Review.java dùng Byte + columnDefinition = "TINYINT",
   không dùng Integer (sẽ lỗi validate wrong column type).
6. Chức năng theo vai trò
   6.1. Guest (chưa đăng nhập)
   Xem danh sách và chi tiết sản phẩm, tìm kiếm, lọc danh mục.
   Mua vào giỏ session, sửa số lượng giỏ — không cập nhật CSDL
   ở bước này.
   Đăng ký bằng email không trùng (lưu CSDL + gửi email + thông báo
   khi có email thật; hiện tại email thật chưa cấu hình nên phần
   này là bước tiếp theo, không giả vờ đã gửi).
   6.2. Customer (đã đăng nhập)
   Thanh toán khi giỏ có sản phẩm và đã đăng nhập — cập nhật CSDL
   gửi email + thông báo, session giỏ về null sau khi thanh toán.
   Xem đơn hàng, tài khoản, đổi mật khẩu (theo luồng đã có).
   Giỏ hàng thật, checkout, trang chi tiết sản phẩm: **chưa làm
   xong**, hiện các link tương ứng là trang chờ hoặc neo về khối
   trên trang chủ/footer. Không được ghi trong báo cáo là đã làm
   xong các phần này.
   6.3. Admin
   Tìm kiếm back-end và CRUD có ràng buộc (đây là yêu cầu gốc,
   phần admin CRUD đầy đủ vẫn là việc chưa làm xong, hiện admin
   mới có trang tổng quan đếm số liệu thật):
   Sản phẩm chưa có trong đơn mới xóa được.
   Loại (danh mục) chưa có sản phẩm mới xóa được.
   Tài khoản xóa được nếu chưa từng đặt hàng online.
   Không xem được password của tài khoản.
   Đơn hàng sắp xếp theo ngày mua.
   Sửa được số lượng mặt hàng trong đơn.
   Validate bằng JavaScript/jQuery phía client hoặc model phía
   server. **Không dùng Functions, Check constraints, Stored
   Procedures trong CSDL** để validate.
   6.4. Đã làm thật (tính đến bản mới nhất)
   Đăng nhập/đăng ký: captcha tự vẽ bằng AWT ở /captcha.png
   (5 ký tự, xóa sau mỗi submit), mật khẩu tối thiểu 8 ký tự,
   BCrypt, role mặc định ROLE_CUSTOMER, điểm 0, khóa khi
   isActive=false, tìm user theo email rồi phone.
   Phân quyền: public gồm /, /menu, /login, /register,
   /captcha.png, /blog, /forgot-password, /error, static,
   /products/**; /admin/** chỉ ROLE_ADMIN; /cart, /orders,
   /account, /checkout cho ROLE_CUSTOMER + ROLE_ADMIN;
   form login dùng tham số username cho email/phone;
   remember-me 7 ngày, key luminous-remember-me; sau đăng nhập
   ưu tiên saved request, không có thì ADMIN vào /admin,
   Customer về /.
   Đăng xuất: POST /logout → về /?logout, hiện modal Bootstrap
   "Bạn đã đăng xuất thành công." ở trang chủ (và ở trang đăng nhập
   phòng khi vào thẳng /login?logout); modal tự mở bằng
   showLogoutModal() trong main.js, rồi xóa tham số logout
   khỏi URL bằng history.replaceState để F5 không mở lại.
   Trang chủ lấy số liệu thật: ưu đãi từ variant có salePrice,
   mới nhất theo createdAt, bán chạy theo tổng số lượng trong
   OrderDetail (chưa có đơn thì fallback sản phẩm mới nhất),
   sao theo trung bình Review, giá qua DTO ProductCard.
   Header/footer dùng chung cho tất cả các trang (kể cả
   login/register/admin) nhờ fragment Thymeleaf.
   Menu riêng /menu, tìm kiếm, danh mục hot, slider, cam kết,
   blog, footer (xem mục 8, 9).
   6.5. Chưa làm (không được ghi là đã xong)
   Giỏ hàng thật (session/DB), thanh toán/checkout, trang chi tiết
   sản phẩm, admin CRUD + tìm kiếm back-end, gửi email thật, đăng nhập
   Facebook/Google thật (hiện chỉ là nút giao diện theo Figma, bấm
   vào hiện thông báo sẽ cấu hình OAuth ở bước sau), blog thật,
   trang chính sách riêng, trang Giới thiệu/Liên hệ/Hỗ trợ riêng,
   mở rộng AI Gemini/Clarifai, tìm kiếm bằng ảnh, barcode, VNPay/MoMo,
   tích điểm, dashboard realtime/WebSocket (các mục mở rộng trong đề
   gốc, làm sau khi phần lõi chạy ổn).
7. Quy định về ảnh
   Đây là phần hay bị làm lệch nhất, nên ghi rõ quy tắc hiện tại
   (là quy tắc cuối cùng đã chốt, các quy tắc cũ hơn đã bị thay thế
   thì không dùng nữa):
   Quy tắc hiện tại — áp dụng cho mọi ảnh product từ nay:
   [slug-ten-san-pham]-[productID].jpg
   Ví dụ:
   product_id
   Tên file
   1
   nuoc-tay-trang-cocoon-bi-dao-1.jpg
   2
   sua-rua-mat-cerave-sach-sau-2.jpg
   3
   sua-rua-mat-cetaphil-diu-nhe-3.jpg
   4
   serum-the-ordinary-niacinamide-4.jpg
   5
   serum-klairs-vitamin-c-5.jpg
   6
   kem-chong-nang-la-roche-posay-anthelios-6.jpg
   7
   gel-chong-nang-skin-aqua-uv-7.jpg
   8
   kem-duong-am-cerave-moisturising-8.jpg
   Slug: viết thường, không dấu, nối bằng -, chỉ giữ phần lõi
   nhận diện sản phẩm, không copy nguyên câu mô tả trong DB.
   productID: đúng id trong bảng Product, không tự nghĩ.
   Không thêm số thứ tự ở đuôi tên (bản trước có -1 ở đuôi
   đã bỏ). Không dùng cột is_featured: mỗi sản phẩm hiện
   chỉ có 1 ảnh nên slug-id.jpg đã đủ duy nhất. Nếu sau này mới
   thêm ảnh thứ 2 thì lúc đó mới thêm hậu tố, ví dụ slug-9-2.jpg,
   và thêm dòng ProductMedia với displayOrder = 2.
   Không thêm phần kích thước vào tên ảnh chung của sản phẩm.
   Phần kích thước chỉ dùng khi ảnh thật theo từng dung tích,
   ví dụ ảnh riêng cho bản 500ml thì đặt ...-500ml.jpg.
   File nằm phẳng trong static/images/products/, không chia
   thư mục theo danh mục nữa, vì tên file đã đủ thông tin và tên
   cũ 01.jpg/02.jpg trùng nhau giữa các thư mục, nhìn không
   biết là ảnh nào.
   Tất cả file .jpg phải là JPEG thật, kiểm tra bằng lệnh
   file. Đã từng xảy ra trường hợp ảnh người dùng gửi thực chất
   là dữ liệu PNG nhưng đặt tên .jpg (có thể bị chặn vì sai
   MIME/nosniff) — khi đó phải chuyển sang JPEG thật, giữ nguyên
   tên file. Riêng logo giữ PNG vì có nền trong suốt
   (alpha 0–255) nên tên file là logo.png, và 3 template
   header/footer/menu phải tham chiếu sang logo.png.
   Bảng tra cứu trong DB là ProductMedia đã có sẵn, không tạo
   bảng product_images mới trùng chức năng: mediaID là id,
   productID là product_id, phần tên file trong mediaURL /
   thumbnailUrl là image_name. File update_product_images.sql
   cập nhật 8 dòng ảnh đầu tiên (displayOrder = 1,
   mediaType = 'IMAGE') từ example.com sang đường dẫn local mới,
   không sửa dòng video của CeraVe, không sửa dòng texture thứ 2
   của Cocoon, không sửa ProductVariant.imgUrl.
   ProductService.java: bảng LOCAL_IMAGES là bảng tra
   file → sản phẩm duy nhất, ảnh dự phòng là ảnh của sản phẩm 1.
   Thêm sản phẩm mới thì đặt tên file đúng quy tắc, bỏ vào
   products/, thêm 1 dòng vào LOCAL_IMAGES, thêm 1 dòng
   ProductMedia với displayOrder = 1.
   Các ảnh khác: banner trang chủ là 1 ảnh tĩnh
   banner/hero-01-ke-my-pham.jpg; ảnh menu là
   banner/menu-visual.jpg; ảnh danh mục hot đặt trong
   categories/ theo tên danh mục; ảnh Figma thật muốn dùng thì
   ghi đè đúng tên file, không đổi tên file trong code.
8. Quy định về giao diện
   8.1. Bố cục chung
   Header: hamburger trái — logo ảnh giữa — icon phải (tài khoản,
   giỏ hàng). Đã bỏ thanh tìm kiếm ở header: không còn nút
   kính lúp và form ẩn mobileSearchBox trong header; tìm kiếm
   chỉ còn ở banner trang chủ. Hàm toggleMobileSearch() trong
   main.js giữ lại cho khỏi vỡ nếu template cũ còn gọi, nhưng
   không còn nút nào gọi nó.
   Footer: nền ảnh mờ 4 cột, địa chỉ Nguyễn Văn Bảo, Gò Vấp,
   line-height 2, mỗi li cách 12px. Icon footer chỉ có icon trong
   vòng tròn 38px, không có chữ bên trong (trước đây chữ "Zalo"
   trong vòng 38px bị tràn), thêm overflow: hidden +
   flex-shrink: 0.
   Logo: header 64px (mobile 44px), trang menu 120px (mobile 80px).
   Chữ menu: mục menu 26px, icon 28px, link bộ lọc 22px,
   mobile 22px.
   Thẻ sản phẩm: các thẻ bằng nhau, chỉ khối "Đại tiệc ưu đãi"
   hiện giá cũ + giá mới (fragment productCard(card, showSale),
   true/false), khối mới/bán chạy chỉ hiện giá gốc, đã bỏ dòng
   "Đã bán" và dòng dung tích (sizeLabel).
   Thương hiệu nổi bật căn giữa, màu #456A41, bỏ dòng phụ.
   Đại tiệc ưu đãi: nền be #FFFCF4, 3 ảnh promo 1 dòng (không
   phải banner carousel; 3 ảnh promo là tài sản riêng, muốn đúng
   ảnh thì ghi đè đúng 3 tên file promo).
   Cam kết: 3 thẻ nền #E0F4E3, chữ #1D5420, dòng cam kết màu
   #756134, icon 76px căn giữa, 3 màu riêng
   (#4B8244, #E67E22, #2E86C1).
   Side-banner mới/bán chạy: bằng đúng 1 thẻ sản phẩm, dùng hàng
   flex 5 phần .new-best-row (banner calc((100% - 64px) / 5)),
   ảnh phủ kín, chữ + nút "Xem thêm" căn giữa cả dọc lẫn ngang
   trên ảnh, không lớp phủ mờ, cao đúng bằng thẻ nhờ
   height: calc(100% - 16px) + margin-top: 4px.
   Ảnh model ở danh mục hot: height: 100%, min-height: 0,
   cột display: flex, cao đúng bằng dòng cuối lưới danh mục.
   Nút slider: 2 nút ở 2 rìa, căn giữa dọc.
   8.2. Trang chủ — hero và tìm kiếm
   Hero chỉ còn 1 ảnh tĩnh, không carousel 3 ảnh, không hiệu
   ứng Ken Burns (đã xóa @keyframes heroZoom và thuộc tính
   animation của .hero-single-img).
   Tiêu đề hero là mỹ phẩm, **không copy nguyên chữ của ảnh mẫu
   shop cây**: "Vẻ đẹp từ thiên nhiên / CHO LÀN DA RẠNG RỠ",
   gợi ý dưới ô tìm kiếm là 4 danh mục thật trong DB, link về
   /?categoryId=1|2|3|4. Muốn đổi tiêu đề thì chỉ sửa 2 dòng
   h1/p và 4 link trong home.html.
   Tìm kiếm là thật: form GET / với q, đúng luồng
   HomeController, lọc theo tên/thương hiệu, kết hợp
   categoryId / categoryName.
   Ô tìm kiếm khi được chọn (:focus-within trong home.css):
   nền trắng #fff, viền xanh lá var(--lum-green), bóng mờ
   xanh nhạt, icon đổi sang xanh đậm, chữ gõ #333, placeholder
   xám; rời khỏi ô thì trở lại nền trong suốt, chữ trắng.
   Nút mật khẩu ở đăng nhập/đăng ký: chỉ dùng icon mắt,
   không dùng chữ "Ẩn"/"Hiện". Nguyên nhân chữ từng xuất hiện là
   hàm togglePassword() cũ set button.textContent, xóa hết
   nội dung nút kể cả thẻ <i> icon. Hàm mới không động vào
   textContent, chỉ đổi class icon giữa bi-eye và
   bi-eye-slash và đổi aria-label/title.
   8.3. Trang menu /menu
   Trang menu là trang riêng, không có header bar, không có
   footer, full 100vh, script Bootstrap + main.js nạp thủ công
   cuối trang (vì trước đây script nằm trong fragment footer).
   Bố cục: khối trái (menu + bộ lọc) 50%, ảnh phải cố định 50%
   (flex: 0 0 50%), bộ lọc chỉ nở trong 50% trái, ảnh phải
   không bị đẩy hay thu lại khi bộ lọc mở.
   Menu chỉ còn 6 mục: Trang chủ, Sản phẩm, Giỏ hàng, Giới thiệu,
   Liên hệ, Hỗ trợ. Đã bỏ khối "Danh mục từ cơ sở dữ liệu" và nút
   Đăng nhập/Đăng ký trong menu (MenuController vẫn trả categories
   nhưng template không dùng). Gạch chân chỉ ở mục vừa được nhấn
   (không mục nào có sẵn active, main.js thêm/gỡ khi nhấn).
   Bộ lọc gồm: Xem tất cả / Mới / Bán chạy / Chăm sóc da / Makeup
   / Dưỡng môi. 3 mục sau chưa có trong DB nên dẫn về trang chủ
   với categoryName + thông báo, không giả vờ có hàng.
   Bộ lọc — quy định hiện tại (đã chốt sau nhiều lần chỉnh):
   Rộng 480px khi mở (ở cả 3 chỗ: quy tắc hiện bộ lọc,
   .menu-page-filter-inner, quy tắc hover dự phòng bằng CSS);
   mobile thì rộng 100%.
   Khi mở, padding của nav giảm xuống 8px mỗi bên, padding của
   mục menu giảm xuống 12px, để nav không bị bóp quá hẹp.
   Mở bộ lọc: chờ 0.12s rồi nở trong 0.30s (tổng 0.42s).
   Mức này là mức giữa đã chốt: bản đầu mở ngay + 0.25s thì quá
   nhanh, lướt qua cũng hiện; bản giữa chờ 0.30s + nở 0.45s
   (0.75s) thì quá chậm, nhìn ì. Lướt qua dưới 0.12s thì chưa
   kịp mở đã bị tắt. Chiều đóng không có delay nên vẫn nhanh.
   Link trong bộ lọc hiện so le, delay 0.15s → 0.35s.
   Hover không dùng CSS :has() làm chính nữa, vì bộ lọc hiện
   làm nút dịch khỏi dưới chuột → mất hover → ẩn → lặp liên
   tục, nhấp nháy. Bộ lọc chỉ mở bằng class
   .menu-page--show-filter do JS thêm: rê vào nút thì mở
   (sau delay trên), chỉ ẩn khi chuột rời khỏi toàn bộ khối
   trái (nav + filter) và đợi thêm 250ms, bấm nút thì khóa mở
   (window.__menuFilterLocked), bấm lại thì mở khóa và ẩn.
   Quy tắc :has() chỉ giữ lại làm dự phòng khi JS không chạy.
   Chuyển trang và hiện menu — quy định hiện tại:
   Trang chủ → menu phải nhanh, nhưng các thành phần menu
   phải hiện so le, từ từ, không hiện ồ ạt cả trang một lúc
   (kiểu gocxanh.app). Lưu ý thật: menu của gocxanh là lớp phủ
   trên cùng một trang (không tải lại), còn menu Luminous là
   trang riêng /menu nên bắt buộc tải lại — không thể nhanh
   tuyệt đối như overlay, chỉ có thể làm cho cảm giác gần như vậy.
   Mờ trang cũ: body.page-fade-out trong style.css
   (không phải menu.css, vì menu.css chỉ nạp ở trang menu
   nên ở trang chủ bấm hamburger sẽ không có CSS áp dụng),
   0.10s, easing cubic-bezier(0.22, 1, 0.36, 1),
   mờ dần + trượt lên 8px.
   Vào trang menu: **không dùng 1 animation pageFadeIn cho
   cả .menu-page** (đó là nguyên nhân cũ làm mọi thứ hiện
   cùng lúc, phẳng và thô). Thay bằng từng thành phần bay lên
   mờ dần (menuItemIn, bay lên 12px, 0.25s, easing như trên):
   mục menu delay 0.02s → 0.17s (6 mục), ảnh phải scale
   1.03 → 1 trong 0.4s, logo delay 0.08s, icon social delay
   0.12s → 0.21s, nút ✕ delay 0.18s. Tổng cộng menu hiện xong
   trong khoảng 0.42s.
   Có prefers-reduced-motion thì tắt hết hiệu ứng.
   Cảnh báo về số liệu: thời gian mờ trong CSS và thời gian
   chờ trong main.js phải khớp nhau, nếu không sẽ bị đứng ở
   màn trắng hoặc bị cắt hiệu ứng giữa chừng. Bản hiện tại:
   mờ CSS 0.10s thì main.js phải đợi 100ms. Nếu chỉ sửa CSS
   mà không sửa số trong main.js thì tổng thời gian vẫn bị
   số trong main.js giữ lại. Cảm giác "đứng 3s" từng gặp không
   phải do CSS 0.2s, mà do khoảng trắng tải lại trang /menu
   (server, DB, mạng) — muốn hết hẳn thì phải bỏ thời gian chờ
   trong main.js hoặc chuyển menu thành lớp phủ không tải lại
   trang, đó là bước riêng, không sửa được bằng CSS thuần.
9. Quy định về code và file
   Chỉ giao các file liên quan, không giao nguyên project.
   Khi giao, đóng zip chứa thư mục src/ (cộng README và file
   SQL nếu có), người dùng chép đè vào project thật. Không bao
   giờ bảo chép đè cả project hay application.properties.
   Chép đè không tự xóa file cũ. Sau khi chép đè phải xóa tay
   các file/thư mục cũ đã bị đổi tên hoặc bỏ đi (ví dụ: thư mục
   product cũ, file ảnh cũ ...-1.jpg, file logo.jpg cũ,
   banner/hero-02|03, products/product-01..08 phẳng,
   categories/category-phan-nuoc.jpg). Nếu không xóa, file cũ
   vẫn nằm đó và gây nhầm.
   Tách CSS theo trang, không viết quá nhiều vào 1 file chung.
   style.css chỉ chứa CSS chung cho toàn hệ thống (biến màu,
   font, header, footer, nút chung, CSS chuyển trang). CSS riêng
   từng trang để ở home.css, menu.css, auth.css, nạp qua
   th:with="pageCss='ten.css'" trên thẻ <html>, fragment
   head luôn nạp style.css rồi nạp thêm pageCss nếu có.
   Trang nào không đặt pageCss thì chỉ nạp CSS chung.
   **Giữ nguyên tên biến Thymeleaf, tên fragment, tên bảng/cột,
   endpoint và số liệu mẫu**, trừ khi người dùng gọi tên chỗ đổi.
   Chỉ sửa đúng chỗ được gọi tên, sau khi đối chiếu file thật.
   Không tự ý đổi logic, đổi stack, ghi đè bản gốc, hay làm bản
   cải tiến tách riêng rồi thay bản gốc.
   **Dữ liệu phải lấy từ database thật, không tự sinh số mẫu
   trong code.** Chưa có dữ liệu thì hiện thông báo rõ, không bịa
   số, không bịa sản phẩm, không bịa danh mục. AI (nếu có sau này)
   chỉ là ước lượng cần xác nhận.
   **Không tự bịa kết quả kiểm thử hay biên dịch khi không chạy
   được.** Để trống và nói rõ giới hạn, không ghi kết quả giả.
   **Tên file, tên biến, tên hàm giữ nguyên nghĩa tiếng Việt
   không dấu trong code** (comment và tên file ảnh dùng không
   dấu), giao diện hiển thị tiếng Việt có dấu đầy đủ.
   Khi thêm tính năng mới: viết code ngắn gọn, dễ hiểu, ưu
   tiên chạy trơn tru và chính xác hơn là viết dài, cầu kỳ.
   Thêm 1 tính năng thì kiểm tra lại các trang dùng chung
   header/footer, vì header/footer mới tự áp cho mọi trang.
10. Quy định gỡ lỗi (rất quan trọng, đã tốn nhiều thời gian)
    Không chẩn đoán lỗi biên dịch từ một dòng lỗi vắn tắt.
    Phải xin toàn bộ khối lỗi gồm symbol/file/dòng và hành động
    gây lỗi. Ví dụ dòng cannot find symbol một mình thì không
    đủ để chẩn đoán.
    Phân biệt lỗi IntelliJ với lỗi thật bằng terminal:
    chạy mvn clean compile ở terminal tại thư mục project.
    Terminal BUILD SUCCESS → lỗi đỏ trong IntelliJ chỉ là lỗi
    đồng bộ/index/classpath của IntelliJ, **không sửa code theo
    dòng đỏ**. Cách khắc phục phía IntelliJ: kiểm tra Settings
    Maven (Bundled Maven, local repo, không tick Work offline),
    Invalidate Caches / Reload project, không đổi code.
    Terminal cũng lỗi → khối lỗi terminal mới là lỗi thật, sửa
    theo khối đó.
    Đã có mốc kiểm chứng: mvn clean compile BUILD SUCCESS,
    29 source file, chỉ warning deprecated ở ProductCard.java.
    Lỗi Thymeleaf render dở chừng: Thymeleaf render từ trên
    xuống, lỗi ở thẻ sản phẩm đầu tiên (ví dụ đánh giá SpringEL
    "card.imageUrl") sẽ làm server dừng giữa chừng, trang bị
    cụt, console có ERR_INCOMPLETE_CHUNKED_ENCODING + 404.
    Khi đó phải lấy đoạn Caused by cuối cùng của log, không
    đoán từ dòng đầu.
    Các lỗi schema đã gặp và cách sửa (không làm lại sai):
    missing column [avt_url] → thêm naming strategy
    Standard/LegacyJpa vào properties, bỏ khai dialect thủ công,
    không đổi ddl-auto thành update.
    `wrong column type [rating] found [tinyint], expecting
         [integer] → Review.rating đổi sang Byte` +
    columnDefinition = "TINYINT".
    Lỗi CSRF 403 sau khi tắt CSRF ở web chain: các input _csrf
    trong template đánh giá vô điều kiện sẽ lỗi 500 EL1007E
    (_csrf.parameterName null) — sửa bằng
    th:if="${_csrf != null}" cho đủ các input, để chạy được
    cả 2 chế độ bật/tắt CSRF.
    Khi giao file: đính kèm file zip ngay trong cùng tin nhắn
    thông báo bản giao, không để người dùng phải hỏi "file đâu?".
    Khi giao code trực tiếp trong chat (theo yêu cầu "chỉ sửa file
    này trực tiếp ở đây"), thì ghi đè đúng file đó, không gửi zip
    chồng lên và không sửa file khác ngoài file được gọi tên,
    trừ khi có ghi chú rõ ràng về dòng ngoài file đó (ví dụ dòng
    mờ trang chủ nằm ở style.css, không nằm ở menu.css).
11. Thêm mới sau này — checklist
    Khi thêm 1 danh mục, 1 sản phẩm, hoặc 1 tính năng, làm theo thứ
    tự này và kiểm tra từng bước:
    Thêm dữ liệu vào DB trước (Category/Product/ProductVariant/
    ProductMedia), đúng id, đúng cột, không đổi tên bảng/cột cũ.
    Thêm ảnh đúng quy tắc mục 7, bỏ vào products/, kiểm tra
    bằng lệnh file là JPEG thật, cập nhật LOCAL_IMAGES và
    update_product_images.sql (nếu cần).
    Nếu là danh mục chưa có sản phẩm: chưa tạo thư mục product
    riêng, chỉ tạo khi có sản phẩm đầu tiên; link danh mục hot
    phải hiện thông báo, không giả vờ có hàng.
    Nếu là trang mới: tạo template riêng, nạp CSS riêng qua
    pageCss (không nhét hết vào style.css), dùng chung
    fragment header/footer.
    Nếu là hiệu ứng mới: ghi rõ 3 số phải khớp nhau — thời gian
    mờ CSS, thời gian chờ trong main.js, thời gian vào trang
    trong CSS của trang đó — và kiểm tra prefers-reduced-motion.
    Chạy mvn clean compile, rồi mvn spring-boot:run,
    kiểm tra bằng Ctrl+F5, kiểm tra cả trang chủ, menu, đăng
    nhập/đăng ký, và trang vừa sửa (không chỉ kiểm tra 1 trang).
    Xóa tay các file/thư mục cũ đã bị thay thế, rồi mới coi là
    xong. Chép đè không tự xóa.
12. Các bản đã giao (để đối chiếu, bản sau thay bản trước)
    Bản
    Nội dung chính
    Luminous_stage1.zip
    Project đủ thư mục, đăng nhập/đăng ký/trang chủ/admin tổng quan. Chưa có giỏ thật, checkout, chi tiết sản phẩm, admin CRUD, email thật, OAuth thật, blog thật.
    Luminous_home_redesign.zip
    Trang chủ theo Figma: header hamburger-logo-icon, hero 3 banner, thương hiệu, đại tiệc, danh mục hot, cam kết, footer ảnh mờ.
    Luminous_layout_homepage_fix.zip
    Sửa trang bị cụt do lỗi card.imageUrl, tách th:each khỏi th:replace, ProductCard bỏ Lombok, kèm lại logo/placeholder.
    Luminous_home_adjustments.zip
    Đại tiệc 3 ảnh 1 dòng, thẻ bằng nhau, bỏ "Đã bán", footer line-height 2, logo logo.jpg 64px, bỏ nút tin nhắn header.
    Luminous_header_footer_menu.zip
    Tách header/footer, menu riêng /menu, bộ lọc Sản phẩm, slider 2 rìa, banner đứng, bỏ placeholder.
    Luminous_menu_product_fix.zip
    Ảnh sản phẩm về local, banner đứng phủ kín, menu full 100vh không header/footer, model cao bằng lưới.
    Luminous_split_css_menu.zip
    Tách CSS theo trang, menu nhanh hơn, logo menu 96px, bộ lọc chỉ trong 50% trái, side-banner bằng thẻ.
    Luminous_hero_search_products.zip
    Hero 1 ảnh + tìm kiếm + gợi ý, ảnh product chia thư mục theo id DB, ảnh PNG đặt tên .jpg chuyển sang JPEG thật, logo thành logo.png.
    Luminous_menu_smooth_products.zip
    Product đổi sang thư mục tên thường (makeup-remover...), CSS chuyển trang về style.css, hover không dùng :has(), chữ menu 26px, logo menu 120px.
    Luminous_menu_fix_search.zip
    Sửa bộ lọc bị trong suốt do thiếu opacity: 1, bỏ tìm kiếm header, mở menu nhanh hơn, bỏ ảnh động banner, ô tìm kiếm :focus-within trắng viền xanh.
    Luminous_logout_modal_auth.zip
    Modal "Bạn đã đăng xuất thành công.", bỏ dòng tài khoản mẫu ở login và dòng lưu ý migration ở register, nút mật khẩu chỉ icon mắt.
    Luminous_product_images_slug.zip
    Ảnh đổi sang [slug]-[id]-1.jpg, phẳng trong products/, kèm SQL đồng bộ ProductMedia.
    Luminous_product_images_slug_id.zip
    Bản zip mới nhất: ảnh đổi sang [slug]-[id].jpg, bỏ số 1 đuôi và cột is_featured.
    (Giao trực tiếp trong chat, sau bản zip trên)
    Menu: bộ lọc 480px, hover chờ 0.12s + nở 0.30s, mở menu nhanh (fade-out 0.10s, vào trang 0.25s, so le 0.02s–0.17s). Khi chép vào project, ưu tiên bản CSS trong chat này hơn bản CSS trong zip cũ, vì zip cũ vẫn còn số cũ (bộ lọc 280px, mờ 0.2s).
