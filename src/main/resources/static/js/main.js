// Luminous - JS nho, khong framework: slider, an/hien mat khau, captcha, tim kiem.
function scrollSlider(id, direction) {
  const el = document.getElementById(id);
  if (el) el.scrollBy({ left: direction * 520, behavior: 'smooth' });
}

function togglePassword(inputId, button) {
  const input = document.getElementById(inputId);
  if (!input) return;
  const show = input.type === 'password';
  input.type = show ? 'text' : 'password';
  const icon = button.querySelector('i');
  if (icon) {
    icon.className = show ? 'bi bi-eye-slash' : 'bi bi-eye';
  }
  button.setAttribute('aria-label', show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
  button.setAttribute('title', show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
}

function showLogoutModal() {
  const params = new URLSearchParams(window.location.search);
  if (!params.has('logout')) return;
  const el = document.getElementById('logoutSuccessModal');
  if (el && window.bootstrap && window.bootstrap.Modal) {
    new window.bootstrap.Modal(el).show();
  }
  params.delete('logout');
  const rest = params.toString();
  const url = window.location.pathname + (rest ? '?' + rest : '');
  window.history.replaceState({}, '', url);
}

function refreshCaptcha() {
  const img = document.getElementById('captchaImg');
  if (img) img.src = '/captcha.png?t=' + Date.now();
  const input = document.getElementById('captchaInput');
  if (input) { input.value = ''; input.focus(); }
}

// Ham nay khong con nut nao goi nua (header da bo o tim kiem), giu lai
// de khong vo neu template cu nao con goi no.
function toggleMobileSearch() {
  const box = document.getElementById('mobileSearchBox');
  if (box) box.classList.toggle('d-none');
}

document.addEventListener('DOMContentLoaded', function () {
  showLogoutModal();
  document.querySelectorAll('a.page-transition-link').forEach(function (link) {
    link.addEventListener('click', function (e) {
      const href = link.getAttribute('href');
      if (!href || href.startsWith('#')) return;
      e.preventDefault();
      document.body.classList.add('page-fade-out');
      setTimeout(function () { window.location.href = href; }, 50);
    });
  });
  var menuPage = document.getElementById('menuPage');
  var leftWrap = menuPage ? menuPage.querySelector('.menu-page-left-wrap') : null;
  var productBtn = document.getElementById('menuProductBtn');
  if (menuPage && leftWrap && productBtn) {
    var hideTimer = null;
    productBtn.addEventListener('mouseenter', function () {
      if (hideTimer) { clearTimeout(hideTimer); hideTimer = null; }
      menuPage.classList.add('menu-page--show-filter');
      productBtn.setAttribute('aria-expanded', 'true');
    });
    leftWrap.addEventListener('mouseenter', function () {
      if (hideTimer) { clearTimeout(hideTimer); hideTimer = null; }
    });
    leftWrap.addEventListener('mouseleave', function () {
      if (window.__menuFilterLocked) return;
      hideTimer = setTimeout(function () {
        menuPage.classList.remove('menu-page--show-filter');
        productBtn.setAttribute('aria-expanded', 'false');
      }, 250);
    });
  }

  // Hieu ung xuat hien muot khi cuon trang: gan .reveal cho cac section
  // trong <main> (ap dung cho moi trang dung layout chung), hien dan khi
  // vao tam nhin. Ton trong tuy chon giam chuyen dong cua trinh duyet.
  if ('IntersectionObserver' in window) {
    var revealEls = document.querySelectorAll('main section, main .product-card, main .blog-card');
    var observer = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible');
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: 0.08, rootMargin: '0px 0px -40px 0px' });
    revealEls.forEach(function (el) {
      el.classList.add('reveal');
      observer.observe(el);
    });
  }
  document.querySelectorAll('.menu-page .menu-item, .menu-page-filter a').forEach(function (item) {
    item.addEventListener('click', function () {
      document.querySelectorAll('.menu-page .menu-item, .menu-page-filter a').forEach(function (el) {
        el.classList.remove('active');
      });
      item.classList.add('active');
    });
  });

  // Dem so thong ke (count-up): chay khi cuon toi, chi chay 1 lan.
  // Doc muc tieu + hau to tu data-target / data-decimals / data-suffix.
  var countEls = document.querySelectorAll('.count-up');
  if (countEls.length && 'IntersectionObserver' in window) {
    var countObserver = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (!entry.isIntersecting) return;
        var el = entry.target;
        countObserver.unobserve(el);
        var target = parseFloat(el.getAttribute('data-target') || '0');
        var decimals = parseInt(el.getAttribute('data-decimals') || '0', 10);
        var suffix = el.getAttribute('data-suffix') || '';
        var prefix = el.getAttribute('data-prefix') || '';
        var format = function (v) { return decimals > 0 ? v.toFixed(decimals) : String(Math.round(v)); };
        if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
          el.textContent = prefix + format(target) + suffix;
          return;
        }
        var start = null;
        var duration = 1200;
        var frame = function (ts) {
          if (start === null) start = ts;
          var p = Math.min((ts - start) / duration, 1);
          var eased = 1 - Math.pow(1 - p, 3);
          el.textContent = prefix + format(target * eased) + suffix;
          if (p < 1) requestAnimationFrame(frame);
        };
        requestAnimationFrame(frame);
      });
    }, { threshold: 0.4 });
    countEls.forEach(function (el) { countObserver.observe(el); });
  }
});

function toggleProductFilter() {
  const page = document.getElementById('menuPage');
  const btn = document.getElementById('menuProductBtn');
  if (!page) return;
  const show = page.classList.toggle('menu-page--show-filter');
  window.__menuFilterLocked = show;
  if (btn) btn.setAttribute('aria-expanded', show ? 'true' : 'false');
}
