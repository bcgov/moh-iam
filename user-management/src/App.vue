<template>
  <div id="app">
    <the-header></the-header>
    <the-nav-bar></the-nav-bar>
    <main>
      <section class="content">
        <the-sub-nav></the-sub-nav>

        <div class="col1">
          <h2>User Account</h2>
          <button onclick="kcMethods.keycloak.accountManagement()">Account</button>
          <button onclick="kcMethods.loadProfile()">Get Profile</button>
          <button onclick="kcMethods.updateProfile()">Update profile</button>
        </div>

        <div class="col1">
          <h2>Token Information</h2>
          <button onclick="kcMethods.refreshToken(9999)">Refresh Token</button>
          <button onclick="kcMethods.refreshToken(30)">Refresh Token (if &lt;30s validity)</button>
          <button onclick="kcMethods.loadUserInfo()">Get User Info</button>
          <button onclick="kcMethods.output(app.keycloak.tokenParsed)">Show Token</button>
          <button onclick="kcMethods.output(app.keycloak.refreshTokenParsed)">Show Refresh Token</button>
          <button onclick="kcMethods.output(app.keycloak.idTokenParsed)">Show ID Token</button>
          <button onclick="kcMethods.showExpires()">Show Expires</button>
          <button onclick="kcMethods.output(kcMethods.keycloak)">Show Details</button>
        </div>
        <div class="col1">
          <h2>User Search</h2>
          <label for="user-name">Username</label>
          <input type="text" id="user-name" value />
          <button onclick="kcMethods.output(kcMethods.loadData())">Search Users</button>
        </div>

        <div class="col4">
          <label>Result</label>
          <pre
            style="background-color: #ddd; border: 1px solid #ccc; padding: 10px; word-wrap: break-word; white-space: pre-wrap; margin-bottom: 20px"
            id="output"
          ></pre>

          <label style="margin-top: 20px">Events</label>
          <pre
            style="background-color: #ddd; border: 1px solid #ccc; padding: 10px; word-wrap: break-word; white-space: pre-wrap;"
            id="events"
          ></pre>
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
      message: "You loaded this page on " + new Date().toLocaleString(),
      testIfStatement: "Test if Statement",
      seen: true,
      listItems: [
        { text: "list item 1" },
        { text: "list item 2" },
        { text: "list item 3" }
      ],
      userInput: "",
      groceryList: [
        { id: 0, text: "Vegetables" },
        { id: 1, text: "Cheese" },
        { id: 2, text: "Whatever else humans are supposed to eat" }
      ]
    };
  },
  methods: {
    reverseMessage: function() {
      this.listItems[1].text = this.listItems[1].text
        .split("")
        .reverse()
        .join("");
    }
  },
  computed: {
    reversedTestIfStatement: function() {
      return this.testIfStatement
        .split("")
        .reverse()
        .join("");
    }
  }
};
</script>

<style src=./assets/css/fonts.css></style>
<style src=./assets/css/grid.css></style>
<style src=./assets/css/main.css></style>
<style src=./assets/css/reset.css></style>
<style src=./assets/css/typography.css></style>