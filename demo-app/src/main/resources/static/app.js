// Shared front-end helpers of Sandbox Shop.
// Testability hook: window.__pending = number of API requests in flight ("network idle" when 0).
(function () {
  window.__pending = 0;
  let spinnerUsers = 0;

  const App = {
    token: () => localStorage.getItem('token'),
    username: () => localStorage.getItem('username'),
    money: cents => '$' + (cents / 100).toFixed(2),
    sleep: ms => new Promise(r => setTimeout(r, ms)),
    $: id => document.getElementById(id),

    async api(method, path, body) {
      window.__pending++;
      try {
        const res = await fetch(path, {
          method,
          headers: { 'Content-Type': 'application/json', 'X-Auth-Token': App.token() || '' },
          body: body === undefined ? undefined : JSON.stringify(body)
        });
        const data = await res.json().catch(() => null);
        if (res.status === 401 && path !== '/api/login') {
          App.logout();
        }
        if (!res.ok) {
          const err = new Error((data && data.error) || res.statusText);
          err.status = res.status;
          throw err;
        }
        return data;
      } finally {
        window.__pending--;
      }
    },

    // Shows the full-page overlay while the promise is running (it blocks all clicks!)
    async withSpinner(promise) {
      spinnerUsers++;
      App.$('spinner').hidden = false;
      try {
        return await promise;
      } finally {
        if (--spinnerUsers === 0) App.$('spinner').hidden = true;
      }
    },

    toast(message, kind) {
      const el = document.createElement('div');
      el.className = 'toast' + (kind ? ' ' + kind : '');
      el.setAttribute('role', 'status');
      el.textContent = message;
      App.$('toasts').appendChild(el);
      setTimeout(() => el.classList.add('fading'), 2000);
      setTimeout(() => el.remove(), 2400);
    },

    requireAuth() {
      if (!App.token()) {
        location.replace('/index.html');
        return false;
      }
      return true;
    },

    logout() {
      localStorage.removeItem('token');
      localStorage.removeItem('username');
      location.replace('/index.html');
    },

    async refreshBadge() {
      const cart = await App.api('GET', '/api/cart');
      const badge = App.$('cart-badge');
      badge.textContent = cart.count;
      badge.hidden = cart.count === 0;
      return cart;
    },

    initHeader() {
      App.$('current-user').textContent = App.username();
      App.$('logout').addEventListener('click', App.logout);
      App.refreshBadge();
    }
  };

  window.App = App;
})();
