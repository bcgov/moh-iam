import Vue from 'vue'
import App from './App.vue'

Vue.config.productionTip = false

/* let keycloak = Keycloak('./security/keycloak.json');

keycloak.init(initOptions).success(function (authenticated) { */
 
  
  var vm = new Vue({
    render: h => h(App)
  }).$mount('#app')
/* 
}).error(function () {
  
}); */


console.log(vm.$children[0].message);
