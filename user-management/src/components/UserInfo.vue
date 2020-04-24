<template>
  <div id="user-info">
    <h1>Update - {{ userName }}</h1>

    <div class="col2">
      <label for="user-name" class="disabled">User Name</label>
      <v-text-field dense outlined disabled hide-details="auto" id="user-name" v-model="userName" />
    </div>
    <div class="col2">
      <label for="first-name">First Name</label>
      <v-text-field dense outlined hide-details="auto" id="first-name" v-model="firstName" />
    </div>
    <div class="col2">
      <label for="last-name">Last Name</label>
      <v-text-field dense outlined hide-details="auto" id="last-name" v-model="lastName" />
    </div>
    <div class="col2">
      <label>User Roles</label>
      <v-autocomplete outlined dense :items="clients" item-text="clientId" item-value="clientId" placeholder="Select an Application" v-model="selectedClient" ></v-autocomplete>
      {{ selectedClient }}
    </div>
  </div>
</template>

<script>
import { RepositoryFactory } from "./../api/RepositoryFactory";
const ClientsRepository = RepositoryFactory.get("clients");

export default {
  name: "UserInfo",
  data() {
    return {
      clients: [],
      selectedClient: null,
      userName: "123-tschiavo",
      firstName: "trevor",
      lastName: "Schiavone"
    };
  },
  /* TODO add methods() to get the possible client roles when a client is selected */
  mounted() {
    var vm = this;
    ClientsRepository.get()
      .then(response => {
        vm.clients = response.data;
      })
      .catch(e => {
        vm.clients = e;
      });
      /* Todo - get all of the users roles */
  }
};
</script>