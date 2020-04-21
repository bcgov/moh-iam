<template>
  <div id="app">
    <the-header></the-header>
    <the-nav-bar></the-nav-bar>
    <main>
      <section class="content">
        <the-sub-nav></the-sub-nav>

        <div class="col1">
          <h2>User Account</h2>
          <button v-on:click="loadProfile">Get Profile</button>
          <button v-on:click="updateProfile">Update profile</button>
        </div>

        <div class="col1">
          <h2>Token Information</h2>
          <button v-on:click="loadUserInfo">Get User Info</button>
          <button v-on:click="showToken">Show Token</button>
          <button v-on:click="showRefreshToken">Show Refresh Token</button>
          <button v-on:click="showIdToken">Show ID Token</button>
          <button v-on:click="showExpires">Show Expires</button>
          <button v-on:click="showDetails">Show Details</button>
        </div>
        <div class="col1">
          <h2>User Search</h2>
          <label for="user-name">Username</label>
          <input type="text" v-model="userSearchInput" id="user-name" />
          <button v-on:click="searchUser">Search Users</button>
        </div>

        <div class="col4">
          <label>Result</label>
          <pre
            style="background-color: #ddd; border: 1px solid #ccc; padding: 10px; word-wrap: break-word; white-space: pre-wrap; margin-bottom: 20px"
            id="output"
          >{{ result }}</pre>
        </div>
      </section>
    </main>

    <the-footer></the-footer>
  </div>
</template>

<script>
import TheHeader from "./components/TheHeader.vue";
import TheFooter from "./components/TheFooter.vue";
import TheNavBar from "./components/TheNavBar.vue";
import TheSubNav from "./components/TheSubNav.vue";

export default {
  name: "App",
  components: {
    TheHeader,
    TheNavBar,
    TheFooter,
    TheSubNav
  },
  data() {
    return {
      result: "",
      userSearchInput: ""
    };
  },
  methods: {
    loadUserInfo: function() {
      var vm = this;
      this.$keycloak
        .loadUserInfo()
        .success(function(responseData) {
          vm.result = JSON.stringify(responseData, null, "  ");
        })
        .error(function() {
          vm.result = "Failed to load user info";
        });
    },
    loadProfile: function() {
      var vm = this;
      this.$keycloak
        .loadUserProfile()
        .success(function(profile) {
          vm.result = JSON.stringify(profile, null, "  ");
        })
        .error(function() {
          vm.result = "Failed to load user info";
        });
    },
    searchUser: function() {
      var vm = this;
      this.$keycloak.updateToken().success(function() {
        var url =
          vm.$keycloak.authServerUrl +
          "admin/realms/" +
          vm.$keycloak.realm +
          "/users?briefRepresentation=true&first=0&max=20&search=" +
          vm.userSearchInput;

        var req = new XMLHttpRequest();
        req.open("GET", url, true);
        req.setRequestHeader("Accept", "application/json");
        req.setRequestHeader("Authorization", "Bearer " + vm.$keycloak.token);

        req.onreadystatechange = function() {
          if (req.readyState === 4) {
            if (req.status === 200) {
              vm.result = this.responseText;
            } else if (req.status === 403) {
              vm.result = "Forbidden";
            } else {
              vm.result = "Failed";
            }
          }
        };

        req.send();
      });
    },
    showToken: function() {
      this.result = this.$keycloak.tokenParsed;
    },
    showRefreshToken: function() {
      this.result = this.$keycloak.refreshTokenParsed;
    },
    showIdToken: function() {
      this.result = this.$keycloak.idTokenParsed;
    },
    showDetails: function() {
      this.result = this.$keycloak;
    },
    showExpires: function() {
      if (!this.$keycloak.tokenParsed) {
        this.result = "Not authenticated";
        return;
      }
      var o =
        "Token Expires:\t\t" +
        new Date(
          (this.$keycloak.tokenParsed.exp + this.$keycloak.timeSkew) * 1000
        ).toLocaleString() +
        "\n";
      o +=
        "Token Expires in:\t" +
        Math.round(
          this.$keycloak.tokenParsed.exp +
            this.$keycloak.timeSkew -
            new Date().getTime() / 1000
        ) +
        " seconds\n";

      if (this.$keycloak.refreshTokenParsed) {
        o +=
          "Refresh Token Expires:\t" +
          new Date(
            (this.$keycloak.refreshTokenParsed.exp + this.$keycloak.timeSkew) *
              1000
          ).toLocaleString() +
          "\n";
        o +=
          "Refresh Expires in:\t" +
          Math.round(
            this.$keycloak.refreshTokenParsed.exp +
              this.$keycloak.timeSkew -
              new Date().getTime() / 1000
          ) +
          " seconds";
      }

      this.result = o;
    },
    updateProfile: function() {
      var url = this.$keycloak.createAccountUrl().split("?")[0];
      var req = new XMLHttpRequest();
      req.open("POST", url, true);
      req.setRequestHeader("Accept", "application/json");
      req.setRequestHeader("Content-Type", "application/json");
      req.setRequestHeader("Authorization", "bearer " + this.$keycloak.token);

      var vm = this;
      req.onreadystatechange = function() {
        if (req.readyState === 4) {
          if (req.status === 200) {
            vm.result = "Success";
          } else {
            vm.result = "Failed";
          }
        }
      };

      req.send(
        '{"email":"myemail@foo.bar","firstName":"test","lastName":"bar"}'
      );
    }
  }
};
</script>

<style src=./assets/css/fonts.css></style>
<style src=./assets/css/grid.css></style>
<style src=./assets/css/main.css></style>
<style src=./assets/css/reset.css></style>
<style src=./assets/css/typography.css></style>