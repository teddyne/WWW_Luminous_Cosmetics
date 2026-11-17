(function () {
  var root = document.getElementById('luminousAiChat');
  if (!root) return;

  var fab = document.getElementById('aiChatFab');
  var win = document.getElementById('aiChatWindow');
  var messages = document.getElementById('aiChatMessages');
  var form = document.getElementById('aiChatForm');
  var input = document.getElementById('aiChatInput');
  var statusEl = document.getElementById('aiChatStatus');
  var attachBtn = document.getElementById('aiChatAttachBtn');
  var attachMenu = document.getElementById('aiChatAttachMenu');
  var uploadBtn = document.getElementById('aiChatUploadBtn');
  var linkBtn = document.getElementById('aiChatLinkBtn');
  var fileInput = document.getElementById('aiChatFileInput');
  var preview = document.getElementById('aiChatPreview');
  var previewImg = document.getElementById('aiChatPreviewImg');
  var previewName = document.getElementById('aiChatPreviewName');
  var previewRemove = document.getElementById('aiChatPreviewRemove');
  var reloadBtn = document.getElementById('aiChatReload');

  var historyLoaded = false;
  var sending = false;
  var attachedFile = null;
  var attachedUrl = null;
  var typingRow = null;

  function scrollBottom() {
    messages.scrollTop = messages.scrollHeight;
  }

  function setStatus(text) {
    if (statusEl) statusEl.textContent = text;
  }

  function escapeHtml(text) {
    var div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  function renderMessage(text) {
    var html = escapeHtml(text || '');
    html = html.replace(/\*\*([^*]+)\*\*/g, '<b>$1</b>');
    html = html.replace(/(^|\n)- /g, '$1\u2022 ');
    return html.replace(/\n/g, '<br/>');
  }

  function createAiRow() {
    var row = document.createElement('div');
    row.className = 'ai-chat-msg ai-chat-msg-ai';
    var avatar = document.createElement('img');
    avatar.src = '/images/logo.png';
    avatar.alt = 'Luminous';
    avatar.className = 'ai-chat-msg-avatar';
    row.appendChild(avatar);
    var bubble = document.createElement('div');
    bubble.className = 'ai-chat-bubble';
    row.appendChild(bubble);
    messages.appendChild(row);
    return { row: row, bubble: bubble };
  }

  function showTyping() {
    if (typingRow) return;
    var created = createAiRow();
    typingRow = created.row;
    created.bubble.innerHTML = '<span class="ai-chat-typing"><i></i><i></i><i></i></span>';
    scrollBottom();
  }

  function hideTyping() {
    if (typingRow) {
      typingRow.remove();
      typingRow = null;
    }
  }

  function addMessage(role, text, products, draft) {
    if (role === 'user') {
      var userRow = document.createElement('div');
      userRow.className = 'ai-chat-msg ai-chat-msg-user';
      var userBubble = document.createElement('div');
      userBubble.className = 'ai-chat-bubble';
      userBubble.innerHTML = renderMessage(text);
      userRow.appendChild(userBubble);
      messages.appendChild(userRow);
      scrollBottom();
      return;
    }
    var created = createAiRow();
    created.bubble.innerHTML = renderMessage(text);
    if (products && products.length) {
      messages.appendChild(buildProducts(products));
    }
    if (draft) {
      messages.appendChild(buildDraft(draft));
    }
    scrollBottom();
  }

  function buildProducts(products) {
    var wrap = document.createElement('div');
    wrap.className = 'ai-chat-products';
    products.forEach(function (p) {
      var card = document.createElement('div');
      card.className = 'ai-chat-product';
      var img = document.createElement('img');
      img.src = p.imageUrl || '/images/no-image.svg';
      img.alt = p.name;
      card.appendChild(img);
      var info = document.createElement('div');
      info.className = 'ai-chat-product-info';
      var link = document.createElement('a');
      link.href = p.url;
      link.textContent = p.name;
      info.appendChild(link);
      var price = document.createElement('div');
      price.className = 'ai-chat-product-price';
      price.textContent = p.displayPriceText || '';
      if (p.onSale && p.priceText) {
        var old = document.createElement('s');
        old.textContent = p.priceText;
        price.appendChild(old);
      }
      info.appendChild(price);
      card.appendChild(info);
      var add = document.createElement('button');
      add.type = 'button';
      add.className = 'ai-chat-product-add';
      add.textContent = 'Thêm vào giỏ';
      add.addEventListener('click', function () {
        addToCart(p.id, add);
      });
      card.appendChild(add);
      wrap.appendChild(card);
    });
    return wrap;
  }

  function buildDraft(draft) {
    var box = document.createElement('div');
    box.className = 'ai-chat-draft';
    var title = document.createElement('strong');
    title.textContent = 'Đơn nháp của bạn';
    box.appendChild(title);
    var lines = document.createElement('div');
    lines.textContent = 'Tạm tính: ' + (draft.subtotalText || '') + ' • Phí vận chuyển: ' + (draft.shippingFeeText || '') + ' • Tổng: ' + (draft.totalText || '');
    box.appendChild(lines);
    var note = document.createElement('div');
    note.textContent = 'Đơn nháp hết hạn sau 30 phút. Đây chưa phải đơn hàng đã xác nhận.';
    box.appendChild(note);
    var actions = document.createElement('div');
    actions.className = 'ai-chat-draft-actions';
    var confirm = document.createElement('button');
    confirm.type = 'button';
    confirm.className = 'ai-chat-draft-confirm';
    confirm.textContent = 'Chuyển vào giỏ và xem lại';
    confirm.addEventListener('click', function () {
      confirmDraft(draft.id, confirm);
    });
    var dismiss = document.createElement('button');
    dismiss.type = 'button';
    dismiss.className = 'ai-chat-draft-dismiss';
    dismiss.textContent = 'Để sau';
    dismiss.addEventListener('click', function () {
      box.remove();
    });
    actions.appendChild(confirm);
    actions.appendChild(dismiss);
    box.appendChild(actions);
    return box;
  }

  function addToCart(productId, btn) {
    btn.disabled = true;
    btn.textContent = 'Đang thêm...';
    fetch('/api/ai/cart/add', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ productId: productId, quantity: 1 })
    })
      .then(function (r) { return r.json(); })
      .then(function (data) {
        if (data.ok) {
          btn.textContent = 'Đã thêm ✓';
          addMessage('ai', 'Đã thêm "' + data.productName + '" vào giỏ hàng của bạn.');
          if (typeof window.updateCartCount === 'function') window.updateCartCount();
        } else {
          btn.disabled = false;
          btn.textContent = 'Thêm vào giỏ';
          addMessage('ai', data.error || 'Không thêm được sản phẩm vào giỏ hàng.');
        }
      })
      .catch(function () {
        btn.disabled = false;
        btn.textContent = 'Thêm vào giỏ';
        addMessage('ai', 'Không kết nối được tới shop, bạn thử lại sau nhé.');
      });
  }

  function confirmDraft(draftId, btn) {
    btn.disabled = true;
    btn.textContent = 'Đang chuyển...';
    fetch('/api/ai/draft/' + draftId + '/confirm', { method: 'POST' })
      .then(function (r) { return r.json(); })
      .then(function (data) {
        if (data.ok) {
          window.location.href = '/cart';
        } else {
          btn.disabled = false;
          btn.textContent = 'Chuyển vào giỏ và xem lại';
          addMessage('ai', data.error || 'Không chuyển được đơn nháp vào giỏ hàng.');
        }
      })
      .catch(function () {
        btn.disabled = false;
        btn.textContent = 'Chuyển vào giỏ và xem lại';
        addMessage('ai', 'Không kết nối được tới shop, bạn thử lại sau nhé.');
      });
  }

  function loadHistory() {
    if (historyLoaded) return;
    historyLoaded = true;
    fetch('/api/ai/history')
      .then(function (r) { return r.json(); })
      .then(function (data) {
        if (data.ok && data.messages) {
          data.messages.forEach(function (m) {
            addMessage(m.role === 'USER' ? 'user' : 'ai', m.content);
          });
        }
      })
      .catch(function () {});
  }

  function showGreeting() {
    var row = document.createElement('div');
    row.className = 'ai-chat-msg ai-chat-msg-ai';
    var avatar = document.createElement('img');
    avatar.src = '/images/logo.png';
    avatar.alt = 'Luminous';
    avatar.className = 'ai-chat-msg-avatar';
    row.appendChild(avatar);
    var bubble = document.createElement('div');
    bubble.className = 'ai-chat-bubble';
    bubble.textContent = 'Xin chào! Mình là trợ lý AI của Luminous. Mình có thể tư vấn sản phẩm, so sánh giá, thêm vào giỏ hàng, tra cứu đơn hàng và giải đáp chính sách của shop. Bạn cần tìm gì hôm nay?';
    row.appendChild(bubble);
    messages.appendChild(row);
  }

  function resetChat() {
    messages.innerHTML = '';
    typingRow = null;
    showGreeting();
    attachedFile = null;
    attachedUrl = null;
    fileInput.value = '';
    preview.hidden = true;
    input.value = '';
    historyLoaded = true;
    setStatus('Trực tuyến');
    scrollBottom();
    fetch('/api/ai/history', { method: 'DELETE' }).catch(function () {});
  }

  function toggleWindow(show) {
    var open = show !== undefined ? show : win.hidden;
    win.hidden = !open;
    if (open) {
      loadHistory();
      input.focus();
    }
  }

  fab.addEventListener('click', function () { toggleWindow(); });
  if (reloadBtn) reloadBtn.addEventListener('click', function () { resetChat(); });
  document.getElementById('aiChatMinimize').addEventListener('click', function () { toggleWindow(false); });
  document.getElementById('aiChatClose').addEventListener('click', function () { toggleWindow(false); });

  attachBtn.addEventListener('click', function (e) {
    e.stopPropagation();
    attachMenu.hidden = !attachMenu.hidden;
  });
  document.addEventListener('click', function () {
    attachMenu.hidden = true;
  });
  uploadBtn.addEventListener('click', function () {
    attachMenu.hidden = true;
    fileInput.click();
  });
  linkBtn.addEventListener('click', function () {
    attachMenu.hidden = true;
    var url = window.prompt('Dán link ảnh (chỉ HTTPS, tối đa 5MB):');
    if (url && url.trim()) {
      attachedUrl = url.trim();
      attachedFile = null;
      previewImg.src = attachedUrl;
      previewName.textContent = attachedUrl;
      preview.hidden = false;
    }
  });
  fileInput.addEventListener('change', function () {
    var file = fileInput.files && fileInput.files[0];
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) {
      window.alert('Ảnh vượt quá 5MB.');
      fileInput.value = '';
      return;
    }
    attachedFile = file;
    attachedUrl = null;
    previewImg.src = URL.createObjectURL(file);
    previewName.textContent = file.name;
    preview.hidden = false;
  });
  previewRemove.addEventListener('click', function () {
    attachedFile = null;
    attachedUrl = null;
    fileInput.value = '';
    preview.hidden = true;
  });

  form.addEventListener('submit', function (e) {
    e.preventDefault();
    if (sending) return;
    var text = input.value.trim();
    if (!text && !attachedFile && !attachedUrl) return;
    sending = true;
    setStatus('Đang trả lời...');
    if (attachedFile) {
      var imgRow = document.createElement('div');
      imgRow.className = 'ai-chat-msg ai-chat-msg-user';
      var img = document.createElement('img');
      img.src = URL.createObjectURL(attachedFile);
      img.className = 'ai-chat-msg-image';
      imgRow.appendChild(img);
      messages.appendChild(imgRow);
    }
    if (text) addMessage('user', text);
    input.value = '';
    var formData = null;
    var payload = null;
    if (attachedFile) {
      formData = new FormData();
      formData.append('message', text);
      formData.append('image', attachedFile);
    } else {
      payload = { message: text };
      if (attachedUrl) payload.imageUrl = attachedUrl;
    }
    attachedFile = null;
    attachedUrl = null;
    fileInput.value = '';
    preview.hidden = true;
    scrollBottom();
    showTyping();

    if (formData) {
      sendChatJson(null, formData);
    } else {
      sendChatStream(payload);
    }
  });

  function sendChatJson(payload, formData) {
    var request = formData
      ? fetch('/api/ai/chat', { method: 'POST', body: formData })
      : fetch('/api/ai/chat', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
    request
      .then(function (r) { return r.json(); })
      .then(function (data) {
        hideTyping();
        if (data.reply) {
          addMessage('ai', data.reply, data.products, data.draft);
          if (data.fallback) setStatus('Đang trả lời không dùng AI');
          else setStatus('Trực tuyến');
        } else {
          addMessage('ai', data.error || 'Xin lỗi, mình chưa trả lời được lúc này.');
        }
      })
      .catch(function () {
        hideTyping();
        addMessage('ai', 'Không kết nối được tới shop, bạn thử lại sau nhé.');
      })
      .finally(function () {
        sending = false;
        setStatus('Trực tuyến');
      });
  }

  function sendChatStream(payload) {
    var streamRow = null;
    var streamBubble = null;
    var rawText = '';
    var finished = false;
    fetch('/api/ai/chat/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })
      .then(function (r) {
        if (!r.ok || !r.body || typeof r.body.getReader !== 'function') throw new Error('no-stream');
        var reader = r.body.getReader();
        var decoder = new TextDecoder();
        var buffer = '';
        function handleBlock(block) {
          var eventName = 'message';
          var dataLines = [];
          block.split('\n').forEach(function (line) {
            if (line.indexOf('event:') === 0) eventName = line.slice(6).trim();
            else if (line.indexOf('data:') === 0) dataLines.push(line.slice(5)); // Spring ghi "data:" khong co khoang trang sau dau hai cham, nen khong duoc cat them 1 khoang trang o dau chunk (chunk cua Gemini thuong bat dau bang khoang trang, cat di se dinh chu lai nhu "Bâygiờ")
          });
          var data = dataLines.join('\n');
          if (eventName === 'chunk') {
            rawText += data;
            if (!streamBubble) {
              hideTyping();
              var created = createAiRow();
              streamRow = created.row;
              streamBubble = created.bubble;
            }
            streamBubble.innerHTML = renderMessage(rawText);
            scrollBottom();
          } else if (eventName === 'done') {
            finished = true;
            hideTyping();
            finishStream(streamRow, streamBubble, rawText, data);
          }
        }
        function readNext() {
          return reader.read().then(function (result) {
            if (result.done) {
              if (!finished) finishStream(streamRow, streamBubble, rawText, null);
              return;
            }
            buffer += decoder.decode(result.value, { stream: true });
            var idx;
            while ((idx = buffer.indexOf('\n\n')) >= 0) {
              var block = buffer.slice(0, idx);
              buffer = buffer.slice(idx + 2);
              handleBlock(block);
            }
            return readNext();
          });
        }
        return readNext();
      })
      .catch(function () {
        if (streamBubble || finished) {
          hideTyping();
          sending = false;
          setStatus('Trực tuyến');
          return;
        }
        sendChatJson(payload, null);
      });
  }

  function finishStream(streamRow, streamBubble, rawText, doneData) {
    var data = null;
    if (doneData) {
      try { data = JSON.parse(doneData); } catch (e) { data = null; }
    }
    if (data && data.error) {
      if (streamBubble) streamBubble.innerHTML = renderMessage(data.error);
      else addMessage('ai', data.error);
      setStatus('Trực tuyến');
    } else if (data) {
      if (!streamBubble) {
        addMessage('ai', data.reply || rawText, data.products, data.draft);
      } else {
        if (data.products && data.products.length) messages.appendChild(buildProducts(data.products));
        if (data.draft) messages.appendChild(buildDraft(data.draft));
        scrollBottom();
      }
      if (data.fallback) setStatus('Đang trả lời không dùng AI');
      else setStatus('Trực tuyến');
    } else if (!streamBubble && rawText) {
      addMessage('ai', rawText);
      setStatus('Trực tuyến');
    }
    sending = false;
  }
})();
