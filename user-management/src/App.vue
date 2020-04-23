<template>
  <v-app>
    <the-header></the-header>
    <the-nav-bar></the-nav-bar>
    <main>
      <section class="content">
        <the-sub-nav></the-sub-nav>

        <UserSearch />

        <div class="col4">
          <v-btn class="secondary" small v-on:click="loadClients">Load Clients</v-btn>
          <ul id="client-list">
            <li v-for="client in clients" v-bind:key="client.clientId">{{ client.clientId }}</li>
          </ul>
        </div>

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

import { RepositoryFactory } from "./api/RepositoryFactory";
import KeycloakDevTools from "./KeycloakDevTools";
import UserSearch from "./components/UserSearch";

const ClientsRepository = RepositoryFactory.get("clients");

export default {
  name: "App",
  components: {
    UserSearch,
    KeycloakDevTools,
    TheHeader,
    TheNavBar,
    TheFooter,
    TheSubNav
  },
  data() {
    return {
      clients: [],
      checkbox1: false
    };
  },
  methods: {
    loadClients: function() {
      var vm = this;
      ClientsRepository.get()
        .then(response => {
          vm.clients = response.data;
        })
        .catch(e => {
          vm.clients = e;
        });
    }
  }
};
</script>

<style src=./assets/css/fonts.css></style>
<style src=./assets/css/grid.css></style>
<style src=./assets/css/main.css></style>
<style src=./assets/css/reset.css></style>
<style src=./assets/css/typography.css></style>
