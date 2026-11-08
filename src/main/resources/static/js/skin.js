var skinQuizStep = 0;

// --- Quiz: chuyen buoc, chi cho qua khi buoc hien tai da chon xong ---
function quizSteps() {
  return Array.prototype.slice.call(document.querySelectorAll('.quiz-step'));
}
function quizShowStep(index) {
  var steps = quizSteps();
  if (!steps.length) return;
  skinQuizStep = Math.max(0, Math.min(index, steps.length - 1));
  steps.forEach(function (el, i) {
    el.classList.toggle('quiz-step-active', i === skinQuizStep);
  });
  var bar = document.getElementById('quizProgressBar');
  var text = document.getElementById('quizProgressText');
  if (bar) bar.style.width = Math.round(((skinQuizStep + 1) / steps.length) * 100) + '%';
  if (text) text.textContent = 'Bước ' + (skinQuizStep + 1) + ' / ' + steps.length;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}
function quizCurrentValid() {
  var steps = quizSteps();
  var step = steps[skinQuizStep];
  if (!step) return true;
  var error = step.querySelector('.quiz-error');
  var ok = true;
  var checked = step.querySelectorAll('input:checked');
  if (step.querySelectorAll('input[name="goals"]').length) {
    ok = checked.length >= 1 && checked.length <= 3;
    var goalError = document.getElementById('goalError');
    if (goalError) goalError.classList.toggle('d-none', ok);
  } else {
    ok = checked.length >= 1;
    if (error) error.classList.toggle('d-none', ok);
  }
  if (!ok) {
    step.classList.remove('quiz-shake');
    void step.offsetWidth; // khoi dong lai animation rung
    step.classList.add('quiz-shake');
  }
  return ok;
}
function quizMove(delta) {
  if (delta > 0 && !quizCurrentValid()) return;
  quizShowStep(skinQuizStep + delta);
}

// Gioi han muc tieu: chon toi da 3, cai thu 4 se tu bo chon va hien loi.
document.addEventListener('DOMContentLoaded', function () {
  var goalBoxes = document.querySelectorAll('input[name="goals"]');
  goalBoxes.forEach(function (box) {
    box.addEventListener('change', function () {
      var checked = document.querySelectorAll('input[name="goals"]:checked');
      if (checked.length > 3) {
        box.checked = false;
        var err = document.getElementById('goalError');
        if (err) err.classList.remove('d-none');
      } else {
        var err2 = document.getElementById('goalError');
        if (err2 && checked.length >= 1) err2.classList.add('d-none');
      }
    });
  });

  // Tom tat lua chon o buoc cuoi de ban thay ro minh da chon du chua.
  function updateQuizSummary() {
    var summary = document.getElementById('quizSummary');
    if (!summary) return;
    var goalNames = [];
    document.querySelectorAll('input[name="goals"]:checked').forEach(function (box) {
      var label = box.closest('label');
      if (label) goalNames.push(label.textContent.trim());
    });
    var budgetBox = document.querySelector('input[name="budget"]:checked');
    var budgetName = 'chưa chọn ngân sách';
    if (budgetBox) {
      var strong = budgetBox.closest('label').querySelector('strong');
      budgetName = strong ? strong.textContent.trim() : 'đã chọn ngân sách';
    }
    summary.textContent = 'Bạn đã chọn: mục tiêu '
      + (goalNames.length ? goalNames.join(', ') : 'chưa chọn')
      + ' · ngân sách ' + budgetName + '.';
  }
  document.querySelectorAll('input[name="goals"], input[name="budget"]').forEach(function (input) {
    input.addEventListener('change', updateQuizSummary);
  });
  updateQuizSummary();

  // Chan submit khi thieu, nhung bao CU THE thieu cai gi va nhay toi buoc do,
  // khong dung alert chung chung nhu ban cu nua.
  var form = document.getElementById('skinQuizForm');
  if (form) {
    form.addEventListener('submit', function (e) {
      // Dap an da: moi cau 1 name rieng answer1..answerN.
      var answers = form.querySelectorAll('input[name^="answer"]:checked');
      var questionSteps = quizSteps().slice(2); // buoc 0 muc tieu, buoc 1 ngan sach
      var totalQuestions = questionSteps.length;
      var goals = form.querySelectorAll('input[name="goals"]:checked');
      var budget = form.querySelector('input[name="budget"]:checked');
      var missing = [];
      var jumpTo = -1;
      if (goals.length < 1 || goals.length > 3) {
        missing.push('mục tiêu (chọn 1–3 mục tiêu)');
        jumpTo = 0;
      }
      if (!budget) {
        missing.push('ngân sách (chọn 1 mức)');
        if (jumpTo < 0) jumpTo = 1;
      }
      if (answers.length !== totalQuestions) {
        missing.push('câu hỏi về da (còn thiếu ' + (totalQuestions - answers.length) + ' câu)');
        if (jumpTo < 0) {
          // Tim cau da dau tien chua chon de nhay toi dung cau do.
          for (var i = 0; i < questionSteps.length; i++) {
            if (!questionSteps[i].querySelector('input:checked')) { jumpTo = i + 2; break; }
          }
          if (jumpTo < 0) jumpTo = 2;
        }
      }
      if (missing.length) {
        e.preventDefault();
        alert('Bạn còn thiếu: ' + missing.join('; ') + '.');
        if (jumpTo >= 0) quizShowStep(jumpTo);
      }
    });
  }

  // --- Man hinh "dang thiet lap": tich tung buoc roi tu chuyen sang /my-routine ---
  var building = document.getElementById('buildingSteps');
  if (building) {
    var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    var items = building.querySelectorAll('li');
    if (reduceMotion) {
      items.forEach(function (li) { li.classList.add('building-done'); });
      setTimeout(function () { window.location.href = '/my-routine'; }, 1200);
    } else {
      items.forEach(function (li, i) {
        setTimeout(function () { li.classList.add('building-done'); }, 600 * (i + 1));
      });
      setTimeout(function () { window.location.href = '/my-routine'; }, 3200);
    }
  }

  // --- Tab lich sang / toi ---
  document.querySelectorAll('[data-routine-tab]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      document.querySelectorAll('[data-routine-tab]').forEach(function (b) {
        b.classList.toggle('routine-tab-active', b === btn);
      });
      document.querySelectorAll('.routine-panel').forEach(function (p) {
        p.classList.remove('routine-panel-active');
      });
      var panel = document.getElementById('routine-' + btn.getAttribute('data-routine-tab'));
      if (panel) panel.classList.add('routine-panel-active');
    });
  });

  // --- Lich tuan: ghi ngay thang that cua tuan nay + dau "da lam" luu o trinh duyet ---
  var weekDays = document.querySelectorAll('.week-day');
  if (weekDays.length) {
    var today = new Date();
    var monday = new Date(today);
    var day = today.getDay(); // 0 = Chu nhat
    monday.setDate(today.getDate() - (day === 0 ? 6 : day - 1));
    weekDays.forEach(function (card) {
      var offset = parseInt(card.getAttribute('data-day-offset') || '0', 10);
      var date = new Date(monday);
      date.setDate(monday.getDate() + offset);
      var label = card.querySelector('.week-date');
      if (label) {
        label.textContent = date.getDate() + '/' + (date.getMonth() + 1) + '/' + date.getFullYear();
      }
      var key = 'lum-routine-done-' + date.getFullYear() + '-' + (date.getMonth() + 1) + '-' + date.getDate();
      var btn = card.querySelector('.week-done-btn');
      if (!btn) return;
      var apply = function (done) {
        card.classList.toggle('week-done', done);
        btn.textContent = done ? '✓ Đã làm xong' : '☐ Đã làm xong';
      };
      apply(localStorage.getItem(key) === '1');
      btn.addEventListener('click', function () {
        var done = localStorage.getItem(key) !== '1';
        if (done) localStorage.setItem(key, '1'); else localStorage.removeItem(key);
        apply(done);
      });
    });
  }
});
