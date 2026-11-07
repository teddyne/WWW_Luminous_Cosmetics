package com.luminous.controller.admin;

import com.luminous.dto.DashboardStats;
import com.luminous.dto.RevenuePoint;
import com.luminous.service.admin.AdminDashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class AdminController {

    private final AdminDashboardService dashboard;

    public AdminController(AdminDashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/admin")
    public String dashboard(@RequestParam(defaultValue = "day") String range,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day,
                            @RequestParam(required = false) String month,
                            @RequestParam(required = false) Integer year,
                            Model model) {
        List<RevenuePoint> series = dashboard.revenueSeries(range, day, month, year);
        BigDecimal periodRevenue = series.stream().map(RevenuePoint::value)
                .filter(value -> value != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        DashboardStats stats = dashboard.stats(periodRevenue);
        var best = dashboard.topSellers(10);
        var slowest = dashboard.slowestSellers(10);
        model.addAttribute("stats", stats);
        model.addAttribute("range", range);
        model.addAttribute("day", day == null ? LocalDate.now() : day);
        model.addAttribute("month", AdminDashboardService.monthOrNow(month).toString());
        model.addAttribute("year", year == null ? LocalDate.now().getYear() : year);
        model.addAttribute("periodLabel", dashboard.periodLabel(range, day, month, year));
        model.addAttribute("revenueLabels", series.stream().map(RevenuePoint::label).toList());
        model.addAttribute("revenueValues", series.stream().map(RevenuePoint::value).toList());
        model.addAttribute("bestSellers", best);
        model.addAttribute("slowestSellers", slowest);
        model.addAttribute("bestLabels", best.stream().map(item -> item.name()).toList());
        model.addAttribute("bestValues", best.stream().map(item -> item.soldCount()).toList());
        model.addAttribute("slowLabels", slowest.stream().map(item -> item.name()).toList());
        model.addAttribute("slowValues", slowest.stream().map(item -> item.soldCount()).toList());
        model.addAttribute("activeMenu", "dashboard");
        return "admin/index";
    }
}
