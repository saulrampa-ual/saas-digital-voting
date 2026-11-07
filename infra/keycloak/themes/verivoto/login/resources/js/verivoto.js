// VeriVoto login theme. Small additions to keycloak.v2, both without touching template.ftl:
// 1) a one-line subtitle under the sign-in title (the second step, the email code, is announced in advance);
// 2) the language select shown as two small flag buttons (links to the same URLs Keycloak puts in the select), plus a
//    one-line hint for voters who land here by mistake;
// 3) live password requirements on login-update-password.ftl (only rules the browser can check; the rest say
//    "checked when saving"). Texts that come from the server are rendered by the template, not here;
// 4) the email code as six boxes with the resend countdown (login-email-otp.ftl).
(function () {
  'use strict';
  var lang = (document.documentElement.lang || 'es').toLowerCase().indexOf('en') === 0 ? 'en' : 'es';
  var SUBTITLE = {
    es: 'Accede con tu cuenta de gestión. Después te pediremos un código de verificación.',
    en: 'Sign in with your management account. Next, we will ask you for a verification code.'
  };

  var VOTER_HINT = {
    es: '¿Vas a votar? Usa el enlace personal que recibiste por correo.',
    en: 'Going to vote? Use the personal link you received by email.'
  };
  var LANGS = {
    es: { label: 'Español', code: 'ES',
      flag: '<svg viewBox="0 0 30 20" aria-hidden="true" focusable="false"><rect width="30" height="20" fill="#AA151B"/><rect y="5" width="30" height="10" fill="#F1BF00"/></svg>' },
    en: { label: 'English', code: 'EN',
      flag: '<svg viewBox="0 0 60 40" aria-hidden="true" focusable="false"><clipPath id="vv-uk"><path d="M0 0h60v40H0z"/></clipPath><g clip-path="url(#vv-uk)"><path d="M0 0h60v40H0z" fill="#012169"/><path d="M0 0l60 40M60 0L0 40" stroke="#fff" stroke-width="8"/><path d="M0 0l60 40M60 0L0 40" stroke="#C8102E" stroke-width="3"/><path d="M30 0v40M0 20h60" stroke="#fff" stroke-width="12"/><path d="M30 0v40M0 20h60" stroke="#C8102E" stroke-width="7"/></g></svg>' }
  };
  var NAV_LABEL = { es: 'Idioma', en: 'Language' };

  function languageButtons() {
    var select = document.getElementById('login-select-toggle');
    if (!select || document.querySelector('.vv-langs')) return;
    var holder = select.closest('.pf-v5-c-login__main-header-utilities');
    if (!holder) return;
    var nav = document.createElement('nav');
    nav.className = 'vv-langs';
    nav.setAttribute('aria-label', NAV_LABEL[lang]);
    Array.prototype.forEach.call(select.options, function (option) {
      var match = /[?&]kc_locale=([a-zA-Z-]+)/.exec(option.value);
      var code = match ? match[1].toLowerCase().slice(0, 2) : null;
      var info = code && LANGS[code];
      if (!info) return;
      var a = document.createElement('a');
      a.className = 'vv-lang';
      a.href = option.value;
      a.lang = code;
      a.hreflang = code;
      a.setAttribute('aria-label', info.label + ' (' + info.code + ')');
      if (option.selected) a.setAttribute('aria-current', 'true');
      // Only constants of LANGS (flag SVG and code), looked up by a code that must be a LANGS key: no page input.
      // nosemgrep (reviewed 2026-11-04, docs/architecture/quality-and-security.md)
      a.innerHTML =info.flag + '<span aria-hidden="true">' + info.code + '</span>';
      nav.appendChild(a);
    });
    if (nav.children.length < 2) return; // unknown locales: keep Keycloak's select
    // Always ES, then EN (Keycloak sorts the options by their translated label)
    Array.prototype.slice.call(nav.children).sort(function (x, y) {
      return Object.keys(LANGS).indexOf(x.lang) - Object.keys(LANGS).indexOf(y.lang);
    }).forEach(function (el) { nav.appendChild(el); });
    holder.appendChild(nav);
    holder.classList.add('vv-has-langs');
  }

  function voterHint() {
    var main = document.querySelector('.pf-v5-c-login__main');
    if (!main || document.querySelector('.vv-voter-hint')) return;
    if (document.body.getAttribute('data-page-id') !== 'login-login') return;
    var footer = document.createElement('footer'); // a landmark, so the hint is not loose content
    footer.className = 'vv-voter-hint';
    footer.textContent = VOTER_HINT[lang];
    main.insertAdjacentElement('afterend', footer);
  }

  function addSubtitle() {
    if (document.body.getAttribute('data-page-id') !== 'login-login') return;
    var title = document.getElementById('kc-page-title');
    if (!title || document.querySelector('.vv-subtitle')) return;
    var p = document.createElement('p');
    p.className = 'vv-subtitle';
    p.textContent = SUBTITLE[lang];
    title.insertAdjacentElement('afterend', p);
  }

  // Same counting rules as Keycloak's password policies (special = neither letter nor digit).
  var CHECKS = {
    length: function (v, n) { return v.length >= n; },
    maxLength: function (v, n) { return v.length <= n; },
    lowerCase: function (v, n) { return (v.match(/\p{Ll}/gu) || []).length >= n; },
    upperCase: function (v, n) { return (v.match(/\p{Lu}/gu) || []).length >= n; },
    digits: function (v, n) { return (v.match(/\p{Nd}/gu) || []).length >= n; },
    specialChars: function (v, n) { return (v.match(/[^\p{L}\p{Nd}]/gu) || []).length >= n; }
  };

  function passwordRules() {
    var box = document.getElementById('vv-pw-rules');
    var input = document.getElementById('password-new');
    var confirm = document.getElementById('password-confirm');
    if (!box || !input) return;
    var live = document.getElementById('vv-pw-live');
    var met = box.getAttribute('data-met');
    var pending = box.getAttribute('data-pending');
    var describe = function (el, id) {
      var ids = (el.getAttribute('aria-describedby') || '').split(/\s+/).filter(Boolean);
      if (ids.indexOf(id) < 0) ids.push(id);
      el.setAttribute('aria-describedby', ids.join(' '));
    };
    describe(input, 'vv-pw-rules');
    var rules = Array.prototype.slice.call(box.querySelectorAll('.vv-rule[data-rule]'));

    function update(announce) {
      var value = input.value;
      var changed = [];
      rules.forEach(function (li) {
        var check = CHECKS[li.getAttribute('data-rule')];
        if (!check) return;
        var ok = value.length > 0 && check(value, Number(li.getAttribute('data-value')));
        var state = ok ? 'met' : 'pending';
        if (li.getAttribute('data-state') !== state) {
          li.setAttribute('data-state', state);
          li.querySelector('.vv-rule-state').textContent = ' (' + (ok ? met : pending) + ')';
          if (ok) changed.push(li.querySelector('.vv-rule-text').textContent);
        }
      });
      if (announce && live && changed.length) live.textContent = met + ': ' + changed.join('; ');
      updateMatch();
    }

    var match = document.getElementById('vv-pw-match');
    function updateMatch() {
      if (!match || !confirm) return;
      if (!confirm.value) { match.textContent = ''; match.removeAttribute('data-state'); return; }
      var same = confirm.value === input.value;
      match.setAttribute('data-state', same ? 'met' : 'pending');
      match.textContent = same ? match.getAttribute('data-yes') : match.getAttribute('data-no');
    }
    if (confirm && match) {
      describe(confirm, 'vv-pw-match');
      confirm.addEventListener('input', updateMatch);
    }
    input.addEventListener('input', function () { update(true); });
    update(false);
  }

  // 4) SR13: the email code (login-email-otp.ftl) drawn as six boxes. The real input stays on top of them, transparent,
  //    so typing, paste, Backspace and one-time-code autofill are the browser's own; this only mirrors its value.
  //    «Continuar» is enabled at six digits and «Reenviar código» waits for the realm resend cooldown after a send.
  function otpBoxes() {
    var form = document.getElementById('kc-otp-login-form');
    var input = document.getElementById('email-otp');
    if (!form || !input || input.parentNode.querySelector('.vv-otp-boxes')) return;
    var field = input.parentNode;
    var size = parseInt(input.getAttribute('maxlength'), 10) || 6;
    var boxes = document.createElement('div');
    boxes.className = 'vv-otp-boxes';
    boxes.setAttribute('aria-hidden', 'true');
    for (var i = 0; i < size; i++) {
      var box = document.createElement('span');
      box.className = 'vv-otp-box';
      boxes.appendChild(box);
    }
    field.insertBefore(boxes, input);
    field.classList.add('vv-otp-js');
    if (input.getAttribute('aria-invalid') === 'true') field.classList.add('is-invalid');
    var submit = document.getElementById('kc-login');

    function digits(value) { return (value || '').replace(/\D/g, '').slice(0, size); }
    function caretToEnd() { var n = input.value.length; try { input.setSelectionRange(n, n); } catch (e) { /* not a text input */ } }
    function render() {
      var value = input.value;
      Array.prototype.forEach.call(boxes.children, function (box, index) {
        box.textContent = value.charAt(index);
        box.classList.toggle('is-filled', index < value.length);
        box.classList.toggle('is-active', index === Math.min(value.length, size - 1));
      });
      if (submit) submit.disabled = value.length !== size;
    }
    input.addEventListener('input', function () {
      var clean = digits(input.value);
      if (clean !== input.value) input.value = clean;
      field.classList.remove('is-invalid');
      render();
    });
    input.addEventListener('paste', function (event) {
      var data = event.clipboardData && event.clipboardData.getData('text');
      if (!data) return;
      event.preventDefault();
      input.value = digits(data);
      input.dispatchEvent(new Event('input', { bubbles: true }));
    });
    input.addEventListener('focus', function () { field.classList.add('is-focus'); caretToEnd(); });
    input.addEventListener('blur', function () { field.classList.remove('is-focus'); });
    input.addEventListener('click', caretToEnd);
    input.addEventListener('keyup', caretToEnd);
    if (document.activeElement === input) field.classList.add('is-focus');
    render();

    var wait = parseInt(form.getAttribute('data-resend-wait'), 10) || 0;
    var resend = document.getElementById('kc-resend-email');
    var note = document.getElementById('vv-otp-wait');
    if (wait > 0 && resend && note && form.getAttribute('data-start-wait') === 'true') {
      var end = Date.now() + wait * 1000;
      var template = note.getAttribute('data-template') || '';
      resend.disabled = true;
      (function tick() {
        var left = Math.max(0, Math.ceil((end - Date.now()) / 1000));
        if (left === 0) { resend.disabled = false; note.textContent = ''; return; }
        note.textContent = template.replace('%TIME%', Math.floor(left / 60) + ':' + ('0' + (left % 60)).slice(-2));
        setTimeout(tick, 1000);
      })();
    }
  }

  function start() { addSubtitle(); languageButtons(); voterHint(); passwordRules(); otpBoxes(); }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start); else start();
})();
