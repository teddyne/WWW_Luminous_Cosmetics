package com.luminous.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Dia chi giao hang cua khach (bang Address). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Address")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "addressID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userID", nullable = false)
    private User user;

    @Column(name = "recipientName", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "phone", nullable = false, length = 15)
    private String phone;

    @Column(name = "province", nullable = false, length = 100)
    private String province;

    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @Column(name = "ward", nullable = false, length = 100)
    private String ward;

    @Column(name = "detailAddress", nullable = false, length = 255)
    private String detailAddress;

    @Column(name = "isDefault", nullable = false)
    private Boolean isDefault = false;

    /** Chuoi dia chi day du de luu vao Orders.shippingAddress.
     * Bo qua phan nao rong (dia chi moi khong co district vi da bo cap huyen);
     * dia chi cu co du district thi hien y nhu truoc. */
    public String fullAddress() {
        StringBuilder sb = new StringBuilder(detailAddress);
        if (ward != null && !ward.isBlank()) sb.append(", ").append(ward);
        if (district != null && !district.isBlank()) sb.append(", ").append(district);
        if (province != null && !province.isBlank()) sb.append(", ").append(province);
        return sb.toString();
    }
}
