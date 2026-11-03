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
      setTimeout(function () { window.location.href = href; }, 50);;
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
  document.querySelectorAll('.menu-page .menu-item, .menu-page-filter a').forEach(function (item) {
    item.addEventListener('click', function () {
      document.querySelectorAll('.menu-page .menu-item, .menu-page-filter a').forEach(function (el) {
        el.classList.remove('active');
      });
      item.classList.add('active');
    });
  });
});

function toggleProductFilter() {
  const page = document.getElementById('menuPage');
  const btn = document.getElementById('menuProductBtn');
  if (!page) return;
  const show = page.classList.toggle('menu-page--show-filter');
  window.__menuFilterLocked = show;
  if (btn) btn.setAttribute('aria-expanded', show ? 'true' : 'false');
}
