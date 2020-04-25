<template>
  <v-app>
    <the-header></the-header>
    <the-nav-bar></the-nav-bar>
    <main>
      <section class="content">
        <the-sub-nav></the-sub-nav>
        <v-alert v-model="alert" :type="alertType" dismissible>{{ alertMessage }}</v-alert>
        <UserSearch v-show="!selectedUser" v-on:userSelected="setSelectedUser" />
        <UserInfo v-bind:user="selectedUser" v-if="selectedUser" />

        <div class="col4">
          <v-checkbox v-model="showKeycloakTools" :label="`Keycloak Dev Tools`"></v-checkbox>
          <KeycloakDevTools v-show="showKeycloakTools"
        </div>
      </section>
    </main>

    <the-footer></the-footer>
  </v-app>
</template>

<script>
import TheHeader from "./components/TheHeader.vue";
import TheFooter from "./components/TheFooter.vue";
import TheNavBar from "./components/TheNavBar.vue";
import TheSubNav from "./components/TheSubNav.vue";

import KeycloakDevTools from "./KeycloakDevTools";
import UserSearch from "./components/UserSearch";
import UserInfo from "./components/UserInfo";

export default {
  name: "App",
  components: {
    UserSearch,
    KeycloakDevTools,
    UserInfo,
    TheHeader,
    TheNavBar,
    TheFooter,
    TheSubNav
  },
  methods: {
    setSelectedUser: function(user) {
      this.selectedUser = user;
      console.log(this.selectedUser);
    }
  },
  data() {
    return {
      alert: false,
      alertType: "success",
      alertMessage: "",
      showKeycloakTools: false,
      selectedUser: null
    };
  }
};
</script>

<style src=./assets/css/fonts.css></style>
<style src=./assets/css/grid.css></style>
<style src=./assets/css/main.css></style>
<style src=./assets/css/reset.css></style>
<style src=./assets/css/typography.css></style>
