package com.luminous.service.admin;

import com.luminous.dto.DashboardStats;
import com.luminous.dto.RevenuePoint;
import com.luminous.dto.TopProductRow;
import com.luminous.model.enums.OrderStatus;
import com.luminous.model.Product;
import com.luminous.repository.UserRepository;
import com.luminous.repository.order.OrderDetailRepository;
import com.luminous.repository.order.OrderRepository;
import com.luminous.repository.product.ProductRepository;
import com.luminous.service.ProductImage;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final OrderRepository orders;
    private final OrderDetailRepository orderDetails;
    private final ProductRepository products;
    private final UserRepository users;

    public AdminDashboardService(OrderRepository orders,
                                 OrderDetailRepository orderDetails,
                                 ProductRepository products,
                                 UserRepository users) {
        this.orders = orders;
        this.orderDetails = orderDetails;
        this.products = products;
        this.users = users;
    }

    public DashboardStats stats(BigDecimal periodRevenue) {
        long newOrders = orders.countByStatus(OrderStatus.PENDING);
        long newCustomers = users.countByCreatedAtAfter(LocalDateTime.now().minusDays(30));
        long totalProducts = products.count();
        return new DashboardStats(periodRevenue == null ? BigDecimal.ZERO : periodRevenue,
                newOrders, newCustomers, totalProducts);
    }

    public String periodLabel(String range, LocalDate day, String month, Integer year) {
        return switch (range == null ? "day" : range) {
            case "month" -> "Tháng " + monthOrNow(month).format(DateTimeFormatter.ofPattern("MM/yyyy"));
            case "year" -> "Năm " + (year == null ? LocalDate.now().getYear() : year);
            default -> "30 ngày đến " + (day == null ? LocalDate.now() : day)
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        };
    }

    public List<RevenuePoint> revenueSeries(String range, LocalDate day, String month, Integer year) {
        return switch (range == null ? "day" : range) {
            case "month" -> monthDailySeries(monthOrNow(month));
            case "year" -> yearMonthlySeries(year == null ? LocalDate.now().getYear() : year);
            default -> dailySeries(day == null ? LocalDate.now() : day);
        };
    }

    public static YearMonth monthOrNow(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException e) {
            return YearMonth.now();
        }
    }

    private Map<LocalDate, BigDecimal> revenueByDay(LocalDateTime from, LocalDateTime to) {
        Map<LocalDate, BigDecimal> byDate = new HashMap<>();
        for (Object[] row : orders.dailyRevenue(OrderStatus.COMPLETED, from, to)) {
            Object raw = row[0];
            LocalDate date = raw instanceof java.sql.Date sqlDate ? sqlDate.toLocalDate()
                    : raw instanceof LocalDate localDate ? localDate
                    : LocalDate.parse(raw.toString());
            byDate.put(date, (BigDecimal) row[1]);
        }
        return byDate;
    }

    private List<RevenuePoint> dailySeries(LocalDate end) {
        Map<LocalDate, BigDecimal> byDate = revenueByDay(
                end.minusDays(29).atStartOfDay(), end.plusDays(1).atStartOfDay());
        List<RevenuePoint> points = new ArrayList<>();
        DateTimeFormatter label = DateTimeFormatter.ofPattern("dd/MM");
        for (int i = 29; i >= 0; i--) {
            LocalDate date = end.minusDays(i);
            points.add(new RevenuePoint(date.format(label), byDate.getOrDefault(date, BigDecimal.ZERO)));
        }
        return points;
    }

    private List<RevenuePoint> monthDailySeries(YearMonth ym) {
        Map<LocalDate, BigDecimal> byDate = revenueByDay(
                ym.atDay(1).atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay());
        List<RevenuePoint> points = new ArrayList<>();
        DateTimeFormatter label = DateTimeFormatter.ofPattern("dd/MM");
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            LocalDate date = ym.atDay(d);
            points.add(new RevenuePoint(date.format(label), byDate.getOrDefault(date, BigDecimal.ZERO)));
        }
        return points;
    }

    private List<RevenuePoint> yearMonthlySeries(int year) {
        LocalDateTime from = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime to = LocalDate.of(year + 1, 1, 1).atStartOfDay();
        Map<String, BigDecimal> byMonth = new HashMap<>();
        for (Object[] row : orders.monthlyRevenue(OrderStatus.COMPLETED, from, to)) {
            String key = row[0] + "-" + String.format("%02d", (Integer) row[1]);
            byMonth.put(key, (BigDecimal) row[2]);
        }
        List<RevenuePoint> points = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            LocalDate month = LocalDate.of(year, m, 1);
            String key = year + "-" + String.format("%02d", m);
            points.add(new RevenuePoint(month.format(DateTimeFormatter.ofPattern("MM/yyyy")),
                    byMonth.getOrDefault(key, BigDecimal.ZERO)));
        }
        return points;
    }

    public List<TopProductRow> topSellers(int limit) {
        Map<Integer, Long> sold = soldMap();
        Map<Integer, BigDecimal> revenue = revenueMap();
        List<Object[]> rows = orderDetails.topSellerProductIds(OrderStatus.CANCELLED);
        List<TopProductRow> result = new ArrayList<>();
        for (Object[] row : rows) {
            if (result.size() >= limit) {
                break;
            }
            Integer productId = (Integer) row[0];
            Product product = products.findById(productId).orElse(null);
            if (product == null || !Boolean.TRUE.equals(product.getIsActive())) {
                continue;
            }
            result.add(new TopProductRow(productId, product.getName(), ProductImage.imageFor(product),
                    sold.getOrDefault(productId, 0L), revenue.getOrDefault(productId, BigDecimal.ZERO)));
        }
        return result;
    }

    public List<TopProductRow> slowestSellers(int limit) {
        Map<Integer, Long> sold = soldMap();
        Map<Integer, BigDecimal> revenue = revenueMap();
        List<Product> all = products.findByIsActiveTrueOrderByCreatedAtDesc();
        all.sort((a, b) -> {
            int bySold = Long.compare(sold.getOrDefault(a.getId(), 0L), sold.getOrDefault(b.getId(), 0L));
            if (bySold != 0) {
                return bySold;
            }
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        List<TopProductRow> result = new ArrayList<>();
        for (Product product : all) {
            if (result.size() >= limit) {
                break;
            }
            long soldCount = sold.getOrDefault(product.getId(), 0L);
            if (soldCount == 0) {
                continue;
            }
            result.add(new TopProductRow(product.getId(), product.getName(), ProductImage.imageFor(product),
                    soldCount, revenue.getOrDefault(product.getId(), BigDecimal.ZERO)));
        }
        return result;
    }

    public String exportCsv(String range, LocalDate day, String month, Integer year) {
        StringBuilder out = new StringBuilder();
        out.append("Bao cao thong ke Luminous - ").append(periodLabel(range, day, month, year))
                .append(" - ngay xuat ").append(LocalDate.now()).append("\n\n");
        out.append("Doanh thu theo ky\n");
        out.append("Ky,Doanh thu (VND)\n");
        for (RevenuePoint point : revenueSeries(range, day, month, year)) {
            out.append(csv(point.label())).append(",").append(point.value()).append("\n");
        }
        out.append("\nTop 10 san pham ban chay\n");
        out.append("Hang,Ten san pham,Da ban,Doanh thu (VND)\n");
        int rank = 1;
        for (TopProductRow row : topSellers(10)) {
            out.append(rank++).append(",").append(csv(row.name())).append(",")
                    .append(row.soldCount()).append(",").append(row.revenue()).append("\n");
        }
        out.append("\nTop 10 san pham ban cham\n");
        out.append("Hang,Ten san pham,Da ban,Doanh thu (VND)\n");
        rank = 1;
        for (TopProductRow row : slowestSellers(10)) {
            out.append(rank++).append(",").append(csv(row.name())).append(",")
                    .append(row.soldCount()).append(",").append(row.revenue()).append("\n");
        }
        return out.toString();
    }

    public byte[] exportExcel(String range, LocalDate day, String month, Integer year) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeRevenueSheet(workbook, range, day, month, year);
            writeTopSheet(workbook, "Top bán chạy", "Top 10 sản phẩm bán chạy nhất", topSellers(10));
            writeTopSheet(workbook, "Top bán chậm", "Top 10 sản phẩm bán chậm nhất", slowestSellers(10));
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static final XSSFColor GREEN = new XSSFColor(new java.awt.Color(69, 106, 65), null);
    private static final XSSFColor GREEN_DARK = new XSSFColor(new java.awt.Color(47, 82, 51), null);
    private static final XSSFColor LIGHT_GREEN = new XSSFColor(new java.awt.Color(234, 243, 236), null);
    private static final XSSFColor LIGHT_GREY = new XSSFColor(new java.awt.Color(247, 250, 247), null);
    private static final XSSFColor GREY_TEXT = new XSSFColor(new java.awt.Color(107, 114, 128), null);
    private static final XSSFColor WHITE = new XSSFColor(new java.awt.Color(255, 255, 255), null);

    private CellStyle titleStyle(XSSFWorkbook workbook) {
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(GREEN_DARK);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private CellStyle subtitleStyle(XSSFWorkbook workbook) {
        XSSFFont font = workbook.createFont();
        font.setItalic(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(GREY_TEXT);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private CellStyle headerStyle(XSSFWorkbook workbook) {
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(WHITE);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(GREEN);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setThinBorder(style);
        return style;
    }

    private CellStyle cellStyle(XSSFWorkbook workbook, boolean alt, boolean bold) {
        XSSFFont font = workbook.createFont();
        font.setBold(bold);
        font.setFontHeightInPoints((short) 11);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        if (alt) {
            style.setFillForegroundColor(LIGHT_GREY);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        setThinBorder(style);
        return style;
    }

    private CellStyle moneyStyle(XSSFWorkbook workbook, boolean alt, boolean bold) {
        CellStyle style = cellStyle(workbook, alt, bold);
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0\"đ\""));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle intStyle(XSSFWorkbook workbook, boolean alt, boolean bold) {
        CellStyle style = cellStyle(workbook, alt, bold);
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle totalTextStyle(XSSFWorkbook workbook) {
        CellStyle style = cellStyle(workbook, false, true);
        style.setFillForegroundColor(LIGHT_GREEN);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle totalMoneyStyle(XSSFWorkbook workbook) {
        CellStyle style = moneyStyle(workbook, false, true);
        style.setFillForegroundColor(LIGHT_GREEN);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle totalIntStyle(XSSFWorkbook workbook) {
        CellStyle style = intStyle(workbook, false, true);
        style.setFillForegroundColor(LIGHT_GREEN);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void setThinBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void writeRevenueSheet(XSSFWorkbook workbook, String range,
                               LocalDate day, String month, Integer year) {
        XSSFSheet sheet = workbook.createSheet("Doanh thu");
        String rangeLabel = periodLabel(range, day, month, year);
        List<RevenuePoint> points = revenueSeries(range, day, month, year);
        CellStyle header = headerStyle(workbook);
        int rowIndex = 0;
        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("BÁO CÁO DOANH THU — LUMINOUS");
        titleCell.setCellStyle(titleStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));
        Row subRow = sheet.createRow(rowIndex++);
        Cell subCell = subRow.createCell(0);
        subCell.setCellValue("Kỳ báo cáo: " + rangeLabel + "  •  Xuất ngày "
                + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        subCell.setCellStyle(subtitleStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 1));
        rowIndex++;
        int headerRow = rowIndex;
        Row head = sheet.createRow(rowIndex++);
        Cell head0 = head.createCell(0);
        head0.setCellValue("Kỳ");
        head0.setCellStyle(header);
        Cell head1 = head.createCell(1);
        head1.setCellValue("Doanh thu (VND)");
        head1.setCellStyle(header);
        int firstDataRow = rowIndex;
        for (int i = 0; i < points.size(); i++) {
            RevenuePoint point = points.get(i);
            Row row = sheet.createRow(rowIndex++);
            boolean alt = i % 2 == 1;
            Cell label = row.createCell(0);
            label.setCellValue(point.label());
            label.setCellStyle(cellStyle(workbook, alt, false));
            Cell value = row.createCell(1);
            value.setCellValue(point.value() == null ? 0 : point.value().doubleValue());
            value.setCellStyle(moneyStyle(workbook, alt, false));
        }
        int lastDataRow = rowIndex - 1;
        Row total = sheet.createRow(rowIndex++);
        Cell totalLabel = total.createCell(0);
        totalLabel.setCellValue("Tổng cộng");
        totalLabel.setCellStyle(totalTextStyle(workbook));
        Cell totalValue = total.createCell(1);
        totalValue.setCellFormula("SUM(B" + (firstDataRow + 1) + ":B" + (lastDataRow + 1) + ")");
        totalValue.setCellStyle(totalMoneyStyle(workbook));
        sheet.setColumnWidth(0, 22 * 256);
        sheet.setColumnWidth(1, 26 * 256);
        sheet.createFreezePane(0, headerRow + 1);
        sheet.setAutoFilter(new CellRangeAddress(headerRow, lastDataRow, 0, 1));
        sheet.getPrintSetup().setLandscape(true);
        sheet.setFitToPage(true);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);
    }

    private void writeTopSheet(XSSFWorkbook workbook, String sheetName, String title,
                               List<TopProductRow> rows) {
        XSSFSheet sheet = workbook.createSheet(sheetName);
        CellStyle header = headerStyle(workbook);
        int rowIndex = 0;
        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title.toUpperCase());
        titleCell.setCellStyle(titleStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
        Row subRow = sheet.createRow(rowIndex++);
        Cell subCell = subRow.createCell(0);
        subCell.setCellValue("Chỉ tính đơn đã hoàn thành  •  Xuất ngày "
                + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        subCell.setCellStyle(subtitleStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 3));
        rowIndex++;
        int headerRow = rowIndex;
        Row head = sheet.createRow(rowIndex++);
        String[] headers = {"Hạng", "Tên sản phẩm", "Đã bán", "Doanh thu (VND)"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = head.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(header);
        }
        int firstDataRow = rowIndex;
        for (int i = 0; i < rows.size(); i++) {
            TopProductRow item = rows.get(i);
            Row row = sheet.createRow(rowIndex++);
            boolean alt = i % 2 == 1;
            Cell rank = row.createCell(0);
            rank.setCellValue(i + 1);
            rank.setCellStyle(intStyle(workbook, alt, false));
            Cell name = row.createCell(1);
            name.setCellValue(item.name());
            name.setCellStyle(cellStyle(workbook, alt, false));
            Cell sold = row.createCell(2);
            sold.setCellValue(item.soldCount());
            sold.setCellStyle(intStyle(workbook, alt, false));
            Cell revenue = row.createCell(3);
            revenue.setCellValue(item.revenue() == null ? 0 : item.revenue().doubleValue());
            revenue.setCellStyle(moneyStyle(workbook, alt, false));
        }
        int lastDataRow = rowIndex - 1;
        Row total = sheet.createRow(rowIndex++);
        Cell totalLabel = total.createCell(0);
        totalLabel.setCellValue("Tổng cộng");
        totalLabel.setCellStyle(totalTextStyle(workbook));
        Cell totalName = total.createCell(1);
        totalName.setCellStyle(totalTextStyle(workbook));
        Cell totalSold = total.createCell(2);
        if (rows.isEmpty()) {
            totalSold.setCellValue(0);
        } else {
            totalSold.setCellFormula("SUM(C" + (firstDataRow + 1) + ":C" + (lastDataRow + 1) + ")");
        }
        totalSold.setCellStyle(totalIntStyle(workbook));
        Cell totalRevenue = total.createCell(3);
        if (rows.isEmpty()) {
            totalRevenue.setCellValue(0);
        } else {
            totalRevenue.setCellFormula("SUM(D" + (firstDataRow + 1) + ":D" + (lastDataRow + 1) + ")");
        }
        totalRevenue.setCellStyle(totalMoneyStyle(workbook));
        sheet.setColumnWidth(0, 10 * 256);
        sheet.setColumnWidth(1, 55 * 256);
        sheet.setColumnWidth(2, 14 * 256);
        sheet.setColumnWidth(3, 26 * 256);
        sheet.createFreezePane(0, headerRow + 1);
        if (!rows.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(headerRow, lastDataRow, 0, 3));
        }
        sheet.getPrintSetup().setLandscape(true);
        sheet.setFitToPage(true);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String text = value.replace("\"", "\"\"");
        return text.contains(",") || text.contains("\"") || text.contains("\n") ? "\"" + text + "\"" : text;
    }

    private Map<Integer, Long> soldMap() {
        Map<Integer, Long> sold = new HashMap<>();
        for (Object[] row : orderDetails.soldByProduct(OrderStatus.CANCELLED)) {
            sold.put((Integer) row[0], ((Number) row[1]).longValue());
        }
        return sold;
    }

    private Map<Integer, BigDecimal> revenueMap() {
        Map<Integer, BigDecimal> revenue = new HashMap<>();
        for (Object[] row : orderDetails.revenueByProduct(OrderStatus.CANCELLED)) {
            revenue.put((Integer) row[0], (BigDecimal) row[1]);
        }
        return revenue;
    }
}
