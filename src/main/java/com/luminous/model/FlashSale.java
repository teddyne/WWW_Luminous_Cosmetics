package com.luminous.model;

import com.luminous.model.enums.FlashSaleStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "FlashSale")
public class FlashSale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flashSaleID")
    private Integer id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "bannerUrl", length = 500)
    private String bannerUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "startAt", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "endAt", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FlashSaleStatus status = FlashSaleStatus.DRAFT;

    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updatedAt", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @OneToMany(mappedBy = "flashSale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FlashSaleItem> items = new ArrayList<>();

    public boolean isLive(LocalDateTime now) {
        return status == FlashSaleStatus.PUBLISHED
                && now != null && !now.isBefore(startAt) && !now.isAfter(endAt);
    }

    public boolean isUpcoming(LocalDateTime now) {
        return status == FlashSaleStatus.PUBLISHED && now != null && now.isBefore(startAt);
    }

    public String displayStatus(LocalDateTime now) {
        if (status == FlashSaleStatus.DRAFT) {
            return "Nháp";
        }
        if (status == FlashSaleStatus.PAUSED) {
            return "Tạm dừng";
        }
        if (now == null || now.isBefore(startAt)) {
            return "Sắp diễn ra";
        }
        if (!now.isAfter(endAt)) {
            return "Đang diễn ra";
        }
        return "Đã kết thúc";
    }
}
