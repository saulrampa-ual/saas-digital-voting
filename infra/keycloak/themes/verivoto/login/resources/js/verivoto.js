// VeriVoto login theme. Two small additions to keycloak.v2, both without touching template.ftl:
// 1) a one-line subtitle under the sign-in title (the second step, the email code, is announced in advance);
// 2) the language select shown as two small flag buttons (links to the same URLs Keycloak puts in the select), plus a
//    one-line hint for voters who land here by mistake;
// 3) live password requirements on login-update-password.ftl (only rules the browser can check; the rest say
//    "checked when saving"). Texts that come from the server are rendered by the template, not here.
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
      a.innerHTML = info.flag + '<span aria-hidden="true">' + info.code + '</span>';
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

  function start() { addSubtitle(); languageButtons(); voterHint(); passwordRules(); }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start); else start();
})();
