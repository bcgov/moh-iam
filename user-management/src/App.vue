<template>
  <v-app>
    <the-header></the-header>
    <the-nav-bar></the-nav-bar>
    <main>
      <section class="content">
        <the-sub-nav></the-sub-nav>

        <UserSearch v-show="!isUserSelected" />
        <UserInfo v-show="isUserSelected" />

        <div class="col4">
          <v-checkbox v-model="checkbox1" :label="`Keycloak Dev Tools`"></v-checkbox>
          <KeycloakDevTools v-show="checkbox1" />
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
  data() {
    return {
      checkbox1: false,
      isUserSelected: true
    };
  }
};
</script>

<style src=./assets/css/fonts.css></style>
<style src=./assets/css/grid.css></style>
<style src=./assets/css/main.css></style>
<style src=./assets/css/reset.css></style>
<style src=./assets/css/typography.css></style>
