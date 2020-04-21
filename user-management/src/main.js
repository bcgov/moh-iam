import Vue from 'vue'
import App from './App.vue'
import Keycloak from 'keycloak-js';

Vue.config.productionTip = false

let keycloak = Keycloak();
Vue.prototype.$keycloak = keycloak;

var initOptions = {
  responseMode: 'fragment',
  flow: 'standard',
  onLoad: 'login-required'
};

keycloak.init(initOptions).success((auth) => {

  if (!auth) {
    window.location.reload();
  } else {
    console.log("Authenticated");
  }

  var vm = new Vue({
    render: h => h(App)
  }).$mount('#app');
  console.log(vm.$children[0].message);

  setInterval(() => {
    keycloak.updateToken(70).success((refreshed) => {
      if (refreshed) {
        console.log('Token refreshed' + refreshed);
      } else {
        console.log('Token not refreshed, valid for '
          + Math.round(keycloak.tokenParsed.exp + keycloak.timeSkew - new Date().getTime() / 1000) + ' seconds');
      }
    }).error(() => {
      console.log('Failed to refresh token');
    });

  }, 60000)

}).error(() => {
  console.log("Authenticated Failed");
});



