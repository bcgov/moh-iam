<template>
  <div>
    <div class="col1">
      <v-text-field outlined dense v-model="userSearchInput" placeholder="Search for user" />
      <v-btn class="secondary" small v-on:click="searchUser">Search Users</v-btn>
    </div>
    <div class="col4">
      <v-data-table
        :headers="headers"
        :items="searchResults"
        :items-per-page="5"
        class="elevation-1"
      ></v-data-table>
    </div>
  </div>
</template>

<script>
import axios from "axios";

export default {
  name: "UserSearch",

  data() {
    return {
      headers: [
        {
          text: "ID",
          align: "start",
          sortable: false,
          value: "id"
        },
        { text: "Username", value: "username" }
      ],
      result: "",
      userSearchInput: "",
      searchResults: [],
      clients: [],
      checkbox1: false
    };
  },
  methods: {
    searchUser: function() {
      var vm = this;
      this.$keycloak.updateToken().success(function() {
        var url =
          vm.$keycloak.authServerUrl +
          "admin/realms/" +
          vm.$keycloak.realm +
          "/users?briefRepresentation=true&first=0&max=20&search=" +
          vm.userSearchInput;

        axios
          .get(url, {
            headers: { Authorization: "Bearer " + vm.$keycloak.token }
          })
          .then(response => {
            vm.result = response.data;
            vm.searchResults = response.data;
          })
          .catch(e => {
            vm.result = e;
          });
      });
    }
  }
};
</script>

<style scoped>
</style>