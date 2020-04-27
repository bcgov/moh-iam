<template>
  <nav role="navigation">
    <div class="container user-select-off">
      <ul>
        <li class="active">
          <a id="homeLink" v-on:click="$emit('homeTabClicked')">Home</a>
        </li>
      </ul>
    </div>
  </nav>
</template>

<script>
export default {
  name: 'TheNavBar'
}
</script>

<style>
/* Main Navigation */
nav {
    height: 40px;
    background-color: #38598A;
}
nav .container {
    max-width: 1320px;
    min-width: 1100px;
    margin: 0 auto;
    padding: 0 60px;
}
nav .container ul {
  padding: 0;
}
nav .container ul li {
    color: #FFFFFF;
    display: inline-block;
    height: 40px;
    line-height: 40px;
    vertical-align: top;
    font-weight: bold;
}
nav .container ul li.active {
    background-color: #496DA2;
}
nav .container ul li:first-child {
    margin-left: -20px;
}
nav .container ul li:hover {
    cursor: pointer;
    background-color: #496DA2;
}
nav .container ul li a {
    display: block;
    font-size: 1rem;
    line-height: 40px;
    color: #FFFFFF;
    text-decoration: none;
    height: 40px;
    padding: 0 20px;
}
nav .container ul li a:focus {
    background-color: #496DA2;
}
</style>