<template>
  <div id="user-info">
    <h1>Update - {{ userName }}</h1>
    <v-row no-gutters>
      <v-col class="col-7">
        <label for="user-name" class="disabled">User Name</label>
        <v-text-field dense outlined disabled id="user-name" v-model="userName" />

        <label for="first-name">First Name</label>
        <v-text-field dense outlined id="first-name" v-model="firstName" />

        <label for="last-name">Last Name</label>
        <v-text-field dense outlined id="last-name" v-model="lastName" />
      </v-col>
    </v-row>

    <v-card outlined class="subgroup">
      <h2>Permissions</h2>

      <v-row no-gutters>
        <v-col class="col-7">
          <label for="select-client">Application</label>
          <v-autocomplete
            id="select-client"
            outlined
            dense
            :items="clients"
            item-text="clientId"
            item-value="id"
            placeholder="Select an Application"
            v-model="selectedClientId"
            v-on:change="getClientRoles"
          ></v-autocomplete>
        </v-col>
        <v-col class="col-7">
          <label v-show="selectedClientId">Roles</label>
          <div class="checkbox-group">
            <v-checkbox
              hide-details="auto"
              v-for="role in rolesOfSelectedClient"
              v-model="selectedRoles"
              :value="role.name"
              :label="role.name"
              v-bind:key="role.name"
            ></v-checkbox>
          </div>

          {{ selectedRoles }}
        </v-col>
      </v-row>
    </v-card>
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
      selectedClientId: null,
      rolesOfSelectedClient: null,
      selectedRoles: [],
      userName: "123-tschiavo",
      firstName: "trevor",
      lastName: "Schiavone"
    };
  },
  methods: {
    getClientRoles: function() {
      ClientsRepository.getRoles(this.selectedClientId)
        .then(response => {
          this.rolesOfSelectedClient = response.data;
        })
        .catch(e => {
          console.log(e);
        });
    }
  },
  mounted() {
    ClientsRepository.get()
      .then(response => {
        this.clients = response.data;
      })
      .catch(e => {
        console.log(e);
      });
    /* Todo - get all of the users roles */
  }
};
</script>