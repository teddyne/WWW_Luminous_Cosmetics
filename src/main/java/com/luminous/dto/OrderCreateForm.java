package com.luminous.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class OrderCreateForm {

    private Integer customerId;
    private boolean walkIn;
    private String walkInName;
    private String walkInPhone;
    private String salesChannel = "ONLINE";
    private String paymentMethod = "COD";
    private BigDecimal cashGiven;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private String note;
    private List<Integer> variantIds = new ArrayList<>();
    private List<Integer> quantities = new ArrayList<>();

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public boolean isWalkIn() {
        return walkIn;
    }

    public void setWalkIn(boolean walkIn) {
        this.walkIn = walkIn;
    }

    public String getWalkInName() {
        return walkInName;
    }

    public void setWalkInName(String walkInName) {
        this.walkInName = walkInName;
    }

    public String getWalkInPhone() {
        return walkInPhone;
    }

    public void setWalkInPhone(String walkInPhone) {
        this.walkInPhone = walkInPhone;
    }

    public String getSalesChannel() {
        return salesChannel;
    }

    public void setSalesChannel(String salesChannel) {
        this.salesChannel = salesChannel;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getCashGiven() {
        return cashGiven;
    }

    public void setCashGiven(BigDecimal cashGiven) {
        this.cashGiven = cashGiven;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(BigDecimal shippingFee) {
        this.shippingFee = shippingFee;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<Integer> getVariantIds() {
        return variantIds;
    }

    public void setVariantIds(List<Integer> variantIds) {
        this.variantIds = variantIds;
    }

    public List<Integer> getQuantities() {
        return quantities;
    }

    public void setQuantities(List<Integer> quantities) {
        this.quantities = quantities;
    }
}
