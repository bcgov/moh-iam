<template>
  <v-app>
    <the-header></the-header>
    <the-nav-bar></the-nav-bar>
    <main>
      <section class="content">
        <router-view></router-view>
      </section>
      <v-checkbox v-model="showKeycloakTools" :label="`Keycloak Dev Tools`"></v-checkbox>
      <KeycloakDevTools v-show="showKeycloakTools" />
    </main>
    <the-footer></the-footer>
  </v-app>
</template>

<script>
import TheHeader from "./components/TheHeader.vue";
import TheNavBar from "./components/TheNavBar.vue";
import TheFooter from "./components/TheFooter.vue";

import KeycloakDevTools from "./KeycloakDevTools";

export default {
  name: "App",
  components: {
    TheHeader,
    TheNavBar,
    TheFooter,
    KeycloakDevTools
  },
  data() {
    return {
      showKeycloakTools: false
    };
  }
};
</script>

<style src=./assets/css/fonts.css></style>
<style src=./assets/css/grid.css></style>
<style src=./assets/css/main.css></style>
<style src=./assets/css/reset.css></style>
<style src=./assets/css/typography.css></style>