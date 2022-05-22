<template>
  <div class="app-container">
    <div id="main">
      <!-- 公共头 -->
      <myheader />
      <div class="main-container">
        <el-scrollbar class="page-component__scroll">
          <!-- 内容区域 -->
          <nuxt />
        </el-scrollbar>
      </div>
      <!-- 公共底 -->
      <myfooter />
    </div>
  </div>
</template>
<script>
import myheader from './myheader'
import myfooter from './myfooter'

import "~/assets/css/app.css";
import "~/assets/css/chunk.css";
import "~/assets/css/iconfont.css";
import "~/assets/css/main.css";
export default {
  components: {
      myheader,myfooter
    }
  }

</script>
