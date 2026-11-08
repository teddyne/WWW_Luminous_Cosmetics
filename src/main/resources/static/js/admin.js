function toggleSidebar(open) {
  document.body.classList.toggle("sidebar-open", open);
}

function openConfirm(action, message) {
  var form = document.getElementById("confirmForm");
  var text = document.getElementById("confirmText");
  if (!form || !text) return;
  form.querySelectorAll('input[name="ids"], input[name="bulkAction"]').forEach(function (input) {
    input.remove();
  });
  form.setAttribute("action", action);
  text.textContent = message || "Bạn có chắc chắn muốn thực hiện thao tác này?";
  new bootstrap.Modal(document.getElementById("confirmModal")).show();
}

function collectCheckedIds() {
  return Array.from(document.querySelectorAll(".row-check:checked")).map(function (c) {
    return c.value;
  });
}

function updateBulkBar() {
  var bar = document.getElementById("bulkBar");
  if (!bar) return;
  var checked = document.querySelectorAll(".row-check:checked").length;
  var total = document.querySelectorAll(".row-check").length;
  var count = document.getElementById("bulkCount");
  if (count) count.textContent = checked;
  bar.hidden = checked === 0;
  var all = document.querySelector(".select-all");
  if (all) {
    all.checked = total > 0 && checked === total;
    all.indeterminate = checked > 0 && checked < total;
  }
}

function changePageSize(size) {
  var url = new URL(window.location.href);
  url.searchParams.set("size", size);
  url.searchParams.delete("page");
  window.location.href = url.toString();
}

function fillBulkForm(form, ids) {
  form.querySelectorAll('input[name="ids"]').forEach(function (input) {
    input.remove();
  });
  ids.forEach(function (id) {
    var hidden = document.createElement("input");
    hidden.type = "hidden";
    hidden.name = "ids";
    hidden.value = id;
    form.appendChild(hidden);
  });
}

function submitBulk(action) {
  var bar = document.getElementById("bulkBar");
  var form = document.getElementById("bulkForm");
  if (!bar || !form) return;
  var ids = collectCheckedIds();
  if (!ids.length) return;
  fillBulkForm(form, ids);
  form.action = bar.dataset.bulkUrl;
  var actionInput = document.getElementById("bulkActionInput");
  if (actionInput) actionInput.value = action;
  var statusSelect = document.getElementById("bulkStatusSelect");
  var statusInput = document.getElementById("bulkStatusInput");
  if (statusSelect && statusInput) statusInput.value = statusSelect.value;
  form.submit();
}

function confirmBulkDelete() {
  var bar = document.getElementById("bulkBar");
  if (!bar) return;
  var ids = collectCheckedIds();
  if (!ids.length) return;
  var form = document.getElementById("confirmForm");
  var text = document.getElementById("confirmText");
  if (!form || !text) return;
  form.querySelectorAll('input[name="ids"], input[name="bulkAction"]').forEach(function (input) {
    input.remove();
  });
  form.setAttribute("action", bar.dataset.bulkUrl);
  ids.forEach(function (id) {
    var hidden = document.createElement("input");
    hidden.type = "hidden";
    hidden.name = "ids";
    hidden.value = id;
    form.appendChild(hidden);
  });
  var actionInput = document.createElement("input");
  actionInput.type = "hidden";
  actionInput.name = "bulkAction";
  actionInput.value = "delete";
  form.appendChild(actionInput);
  text.textContent = "Xóa " + ids.length + " mục đã chọn? Những mục đã có dữ liệu liên quan sẽ được bỏ qua.";
  new bootstrap.Modal(document.getElementById("confirmModal")).show();
}

function openCategoryModal(action, id, name, description, status) {
  var form = document.getElementById("categoryModalForm");
  var title = document.getElementById("categoryModalTitle");
  var idInput = document.getElementById("categoryIdText");
  if (!form) return;
  form.setAttribute("action", action);
  title.textContent = id ? "Chỉnh sửa loại sản phẩm" : "Thêm loại sản phẩm";
  idInput.value = id ? "#" + id : "Tự động";
  document.getElementById("categoryName").value = name || "";
  document.getElementById("categoryDescription").value = description || "";
  document.getElementById("categoryStatus").value = status || "ACTIVE";
  var error = document.getElementById("categoryNameError");
  if (error) error.textContent = "";
  new bootstrap.Modal(document.getElementById("categoryModal")).show();
}

function bindVoucherScope() {
  var radios = document.querySelectorAll("input[name='scopeType']");
  if (!radios.length) return;
  var apply = function () {
    var value = document.querySelector("input[name='scopeType']:checked");
    var scope = value ? value.value : "ALL";
    var categoryBox = document.getElementById("categoryScope");
    var productBox = document.getElementById("productScope");
    if (categoryBox) categoryBox.classList.toggle("d-none", scope !== "CATEGORY");
    if (productBox) productBox.classList.toggle("d-none", scope !== "PRODUCT");
  };
  radios.forEach(function (radio) { radio.addEventListener("change", apply); });
  apply();
}

function bindDiscountType() {
  var radios = document.querySelectorAll("input[name='discountType']");
  if (!radios.length) return;
  var apply = function () {
    var value = document.querySelector("input[name='discountType']:checked");
    var isPercent = value && value.value === "PERCENTAGE";
    var unit = document.getElementById("discountUnit");
    var maxGroup = document.getElementById("maxDiscountGroup");
    if (unit) unit.textContent = isPercent ? "%" : "đ";
    if (maxGroup) maxGroup.classList.toggle("d-none", !isPercent);
  };
  radios.forEach(function (radio) { radio.addEventListener("change", apply); });
  apply();
}

/* Xem truoc the voucher o form them/sua khuyen mai: mau + nhan doi theo
   loai da chon, cap nhat truc tiep khi admin doi loai / muc giam / ten / ma /
   han su dung / don toi thieu. Chon loai se tu dat dung hinh thuc giam
   (mien phi van chuyen -> phi ship, giam % -> theo %, giam co dinh -> so tien);
   rieng "Voucher dac biet" chi doi mau nhan sang vang, giu nguyen muc giam. */
function bindVoucherPreview() {
  var card = document.getElementById("voucherPreview");
  if (!card) return;
  var typeSelect = document.getElementById("voucherType");
  var scopeSelect = document.getElementById("voucherScope");
  var valueInput = document.getElementById("discountValue");
  var nameInput = document.getElementById("voucherName");
  var codeInput = document.getElementById("voucherCode");
  var endInput = document.getElementById("endDate");
  var minInput = document.getElementById("minOrder");

  var trimNum = function (n) {
    return String(n).replace(/\.0+$/, "").replace(/(\.\d*?)0+$/, "$1");
  };
  var formatK = function (n) {
    if (isNaN(n)) return "0đ";
    if (n >= 1000) return trimNum(n / 1000) + "K";
    return trimNum(n) + "đ";
  };
  var readValue = function () {
    if (!valueInput || !valueInput.value) return NaN;
    return parseFloat(valueInput.value.replace(/,/g, "").replace(/\s/g, ""));
  };
  var setDiscountType = function (type) {
    var radio = document.querySelector("input[name='discountType'][value='" + type + "']");
    if (radio && !radio.checked) radio.click();
  };

  var update = function () {
    var type = typeSelect ? typeSelect.value : "";
    var isSpecial = type === "Voucher đặc biệt";
    var scope = scopeSelect ? scopeSelect.value : "ALL";
    var checked = document.querySelector("input[name='discountType']:checked");
    var isPercent = checked && checked.value === "PERCENTAGE";
    var kind = isSpecial ? "special"
      : (scope === "SHIPPING" ? "shipping" : (isPercent ? "percent" : "fixed"));
    card.className = "voucher-card voucher-card--" + kind;

    var label = document.getElementById("previewLabel");
    if (label) {
      var num = readValue();
      if (kind === "shipping") label.textContent = "MIỄN PHÍ VẬN CHUYỂN";
      else if (kind === "percent") label.textContent = "GIẢM " + (isNaN(num) ? "0" : trimNum(num)) + "%";
      else if (kind === "fixed") label.textContent = "GIẢM " + formatK(num);
      else label.textContent = "ƯU ĐÃI ĐẶC BIỆT";
    }
    var brand = document.getElementById("previewBrand");
    if (brand) brand.style.display = kind === "special" ? "" : "none";

    var name = document.getElementById("previewName");
    if (name) name.textContent = nameInput && nameInput.value.trim() ? nameInput.value.trim() : "Tên chương trình";
    var code = document.getElementById("previewCode");
    if (code) code.textContent = codeInput && codeInput.value.trim() ? codeInput.value.trim() : "—";
    var end = document.getElementById("previewEnd");
    if (end) {
      var raw = endInput ? endInput.value : "";
      if (raw && raw.length >= 10) {
        var parts = raw.slice(0, 10).split("-");
        end.textContent = parts.length === 3 ? parts[2] + "/" + parts[1] + "/" + parts[0] : raw;
      } else {
        end.textContent = "—";
      }
    }
    var min = document.getElementById("previewMin");
    if (min) min.textContent = minInput && minInput.value.trim() ? minInput.value.trim() + "đ" : "—";
  };

  if (typeSelect) {
    typeSelect.addEventListener("change", function () {
      var t = typeSelect.value;
      if (t === "Miễn phí vận chuyển") {
        if (scopeSelect) scopeSelect.value = "SHIPPING";
        setDiscountType("FIXED_AMOUNT");
      } else if (t === "Giảm giá theo phần trăm") {
        if (scopeSelect) scopeSelect.value = "ALL";
        setDiscountType("PERCENTAGE");
      } else if (t === "Giảm số tiền cố định") {
        if (scopeSelect) scopeSelect.value = "ALL";
        setDiscountType("FIXED_AMOUNT");
      } else if (scopeSelect) {
        scopeSelect.value = "ALL";
      }
      update();
    });
  }
  [valueInput, nameInput, codeInput, endInput, minInput].forEach(function (el) {
    if (el) el.addEventListener("input", update);
  });
  document.querySelectorAll("input[name='discountType']").forEach(function (r) {
    r.addEventListener("change", update);
  });
  if (scopeSelect) scopeSelect.addEventListener("change", update);
  update();
}

function generateVoucherCode() {
  var chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  var code = "";
  for (var i = 0; i < 8; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  var input = document.getElementById("voucherCode");
  if (input) input.value = code;
}

function bindImagePreview() {
  document.querySelectorAll("input[type='file'][data-preview]").forEach(function (input) {
    input.addEventListener("change", function () {
      var target = document.getElementById(input.getAttribute("data-preview"));
      var file = input.files && input.files[0];
      if (!target || !file) return;
      target.src = URL.createObjectURL(file);
      target.classList.remove("d-none");
    });
  });
}

function bindCountUp() {
  var elements = document.querySelectorAll("[data-count]");
  if (!elements.length) return;
  var reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  elements.forEach(function (el) {
    var target = parseFloat(el.getAttribute("data-count") || "0");
    var suffix = el.getAttribute("data-suffix") || "";
    if (isNaN(target)) return;
    if (reduceMotion) {
      el.textContent = Math.round(target).toLocaleString("vi-VN") + suffix;
      return;
    }
    var duration = 1200;
    var start = null;
    var step = function (now) {
      if (!start) start = now;
      var progress = Math.min((now - start) / duration, 1);
      var eased = 1 - Math.pow(1 - progress, 3);
      el.textContent = Math.round(target * eased).toLocaleString("vi-VN") + suffix;
      if (progress < 1) requestAnimationFrame(step);
    };
    requestAnimationFrame(step);
  });
}

function toggleCancelReason() {
  var select = document.getElementById("newStatus");
  var box = document.getElementById("cancelReasonBox");
  if (!select || !box) return;
  box.classList.toggle("d-none", select.value !== "CANCELLED");
}

function validateCancelReason() {
  var select = document.getElementById("newStatus");
  var reason = document.getElementById("cancelReason");
  if (select && select.value === "CANCELLED" && reason && !reason.value.trim()) {
    alert("Vui lòng nhập lý do hủy đơn hàng.");
    reason.focus();
    return false;
  }
  return true;
}

function formatMoneyText(value) {
  var raw = String(value == null ? "" : value).replace(/[^0-9.]/g, "");
  if (!raw) {
    return "";
  }
  var parts = raw.split(".");
  var intPart = parts[0].replace(/^0+(?=\d)/, "");
  var formatted = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  if (parts.length > 1) {
    formatted += "." + parts[1].slice(0, 2);
  }
  return formatted;
}

// Gan dinh dang tien (dau phay hang nghin) cho 1 o nhap; dung cho ca o them dong
// bang JS o form san pham (cac o bien the them sau khi trang tai xong).
function attachMoneyInput(input) {
  if (!input || input.dataset.moneyBound) return;
  input.dataset.moneyBound = "1";
  if (input.value) {
    input.value = formatMoneyText(input.value.replace(/\.00$/, ""));
  }
  input.addEventListener("input", function () {
    var start = input.selectionStart == null ? input.value.length : input.selectionStart;
    var before = input.value.length;
    input.value = formatMoneyText(input.value);
    var pos = Math.max(0, start + (input.value.length - before));
    try {
      input.setSelectionRange(pos, pos);
    } catch (e) {
    }
  });
}

function bindMoneyInputs() {
  document.querySelectorAll(".money-input").forEach(attachMoneyInput);
  document.querySelectorAll("form").forEach(function (form) {
    form.addEventListener("submit", function () {
      form.querySelectorAll(".money-input").forEach(function (input) {
        input.value = input.value.replace(/,/g, "");
      });
    });
  });
}

document.addEventListener("DOMContentLoaded", function () {
  bindVoucherScope();
  bindDiscountType();
  bindVoucherPreview();
  bindImagePreview();
  updateBulkBar();
  toggleCancelReason();
  bindCountUp();
  bindMoneyInputs();
});

document.addEventListener("change", function (event) {
  if (event.target.classList.contains("select-all")) {
    var table = event.target.closest("table");
    if (table) {
      table.querySelectorAll(".row-check").forEach(function (c) {
        c.checked = event.target.checked;
      });
    }
    updateBulkBar();
  }
  if (event.target.classList.contains("row-check")) {
    updateBulkBar();
  }
});
