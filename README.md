Quy định và cấu trúc thư mục
1. Cấu trúc thư mục
   src/main/java/com/luminous/
   controller/            # điều khiển request, trả về tên template
       admin/               # trang quản trị: Admin*Controller (10 class)
       auth/                # đăng nhập, đăng ký, captcha
       home/                # trang chủ
       menu/                # trang menu
       product/             # trang sản phẩm của shop
       shop/                # trang phụ của shop
       skinquiz/            # trắc nghiệm da, chu trình
   service/               # xử lý nghiệp vụ, gọi repository
       admin/               # Admin*Service (7 class)
       auth/                # đăng nhập, captcha
       product/             # sản phẩm của shop
       skinquiz/            # trắc nghiệm da, chu trình
       FileStorageService   # lưu file ảnh (dùng chung, để ở package cha)
       ProductImage         # đường dẫn ảnh sản phẩm (dùng chung)
   repository/            # truy vấn database (JPA)
       category/            # loại sản phẩm
       order/               # đơn hàng, chi tiết đơn, thanh toán, tích điểm
       product/             # sản phẩm, biến thể, ảnh sản phẩm
       review/              # đánh giá
       skinquiz/            # kết quả trắc nghiệm, chu trình
       voucher/             # khuyến mãi và phạm vi áp dụng
       UserRepository       # tài khoản (dùng chung, để ở package cha)
       RoleRepository       # vai trò (dùng chung, để ở package cha)
   model/                 # entity JPA + enum
       Orders, User, Product, Category, Voucher, Payment, ...
       enums/
            CategoryStatus, DiscountType, OrderStatus, SalesChannel, PaymentMethod,    
            PaymentStatus, VoucherScope, VoucherApplicableScope...
      dto/                   # form nhận dữ liệu và dòng hiển thị, không chứa
      config/                # cấu hình web, bảo mật, định dạng
      security/              # đăng nhập, phân quyền
      src/main/resources/
      templates/
          admin/               # giao diện trang quản trị
          homepage/            # giao diện trang chủ (template home.html)
          ...                  # giao diện các trang shop khác
      static/
          css/                 # admin.css, style.css
          js/                  # admin.js
          images/products/     # ảnh sản phẩm, chia theo thư mục tên loại
      database/
          migrations/            # 003_admin.sql ... 008_order_walkin.sql
          seed_orders_reviews.sql  # dữ liệu đơn hàng, đánh giá mẫu (chạy 1 lần)
      bill/                    # hóa đơn đã lưu, dạng bill/<mã đơn>.html
2. Quy định
   Đặt class đúng thư mục: class có tên chứa Admin thì để trong thư mục admin/ của tầng đó; class khác để trong thư mục theo tên tính năng (ví dụ product/, skinquiz/); class dùng chung cho nhiều tính năng thì để ở package cha. dto/,
   model/, security/, config/ giữ nguyên, không chia thư mục con. Khi đổi thư mục của class thì xóa file ở đường dẫn cũ, nếu không sẽ trùng bean.
   Enum nằm trong model/: không tạo thư mục enums/ riêng. Enum lưu xuống database dạng chuỗi (@Enumerated(EnumType.STRING), đổi enum không cần migration. Form và tham số request giữ kiểu chuỗi, service chuyển sang enum bằng hàm from() và dùng giá trị mặc định khi chuỗi không hợp lệ (không trả lỗi 400).
   Database thay đổi bằng migration: mọi thay đổi cột, bảng đều viết thành file migration mới trong database/migrations/, đặt tên số tiếp theo (ví dụ 009_...), chạy theo thứ tự, mỗi file chỉ chạy một lần. Không sửa các migration đã chạy.
   Entity phải khớp database vì ddl-auto=validate.
   Tiền tệ: hiển thị tiền bằng Vnd.format(); ô nhập tiền dùng lass money-input (kiểu text, không đặt min/step).
   Bảo mật form: mọi form POST đều có token CSRF, viết kèm th:if="${_csrf != null}".
   Ảnh sản phẩm: lưu vào static/images/products/<tên-loại>/ (tên thư mục là dạng slug của tên loại hiện tại). Đổi tên loại không dời thư mục cũ, xóa loại không xóa thư mục. Ảnh tối đa 5MB, chỉ nhận jpg / jpeg / png / webp / gif. 
   Đơn hàng: trạng thái chỉ chuyển theo luồng PENDING → CONFIRMED → SHIPPING → DELIVERED → COMPLETED, đơn hủy (CANCELLED) phải có lý do và có thể mở lại thành PENDING. Doanh thu chỉ tính đơn COMPLETED. Đơn hoàn thành cộng 1 điểm cho mỗi 1.000đ, cộng đúng một lần. Đơn khách vãng lai lưu userID = NULL, không tạo tài khoản, không tích điểm.
   Hóa đơn: nút Lưu hóa đơn ghi file bill/<mã đơn>.html vào thư mục gốc project (lưu lại thì ghi đè). In hóa đơn dùng hết chiều rộng trang.
   Giao file: chỉ gửi file đã thay đổi; khi giao lại file trùng tên với project thì giữ nguyên mọi hàm và logic cũ, chỉ thêm mới.
   Không ghi đè bản gốc, chỉ sửa đúng chỗ đã gọi tên.
   Code: code ngắn gọn, dễ đọc, dễ hiểu.