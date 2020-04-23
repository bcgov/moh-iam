<template>
  <footer>
    <div class="container">
      <div class="col1">
        <ul>
          <li>
            <a>Help</a>
          </li>
          <li>
            <a>Contact Us</a>
          </li>
        </ul>
      </div>
      <div class="col1">
        <ul>
          <li>
            <a href="http://www2.gov.bc.ca/gov/content/home/disclaimer">Disclaimer</a>
          </li>
          <li>
            <a href="http://www2.gov.bc.ca/gov/content/home/privacy">Privacy</a>
          </li>
          <li>
            <a href="http://www2.gov.bc.ca/gov/content/home/accessibility">Accessibility</a>
          </li>
          <li>
            <a href="http://www2.gov.bc.ca/gov/content/home/copyright">Copyright</a>
          </li>
        </ul>
      </div>
    </div>
  </footer>
</template>

<script>
export default {
  name: "TheFooter"
};
</script>

<style>
footer {
    height: 160px;
    background-color: #003366;
}
footer .container {
    max-width: 1320px;
    min-width: 1100px;
    margin: 0 auto;
    padding: 20px 60px 60px 60px;
}
footer li {
    margin-bottom: 10px;
}
footer li, .v-application footer li a {
    font-size: 1.3rem;
    color: #FFFFFF;
}
footer li a {
    text-decoration: none;
}
footer li a:hover, footer li a:focus {
    text-decoration: underline;
}
</style>