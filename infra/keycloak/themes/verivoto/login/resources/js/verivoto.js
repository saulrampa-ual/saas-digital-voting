// VeriVoto login theme. Two small additions to keycloak.v2, both without touching template.ftl:
// 1) a one-line subtitle under the sign-in title (the second step, the email code, is announced in advance);
// 2) live password requirements on login-update-password.ftl (only rules the browser can check; the rest say
//    "checked when saving"). Texts that come from the server are rendered by the template, not here.
(function () {
  'use strict';
  var lang = (document.documentElement.lang || 'es').toLowerCase().indexOf('en') === 0 ? 'en' : 'es';
  var SUBTITLE = {
    es: 'Accede con tu cuenta de gestión. Después te pediremos un código de verificación.',
    en: 'Sign in with your management account. Next, we will ask you for a verification code.'
  };

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

  function start() { addSubtitle(); passwordRules(); }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start); else start();
})();
