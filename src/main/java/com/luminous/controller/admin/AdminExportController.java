package com.luminous.controller.admin;

import com.luminous.service.admin.AdminDashboardService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import java.io.IOException;
import java.time.LocalDate;

@Controller
public class AdminExportController {

    private final AdminDashboardService dashboard;

    public AdminExportController(AdminDashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/admin/export/revenue")
    public ResponseEntity<byte[]> exportRevenue(@RequestParam(defaultValue = "day") String range,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day,
                                                      @RequestParam(required = false) String month,
                                                      @RequestParam(required = false) Integer year) throws IOException {
        byte[] body = dashboard.exportExcel(range, day, month, year);
        String filename = "bao-cao-doanh-thu-" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }
}
