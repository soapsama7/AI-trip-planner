import { createApp } from 'vue'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import router from './router'
import App from './App.vue'
import { bootstrapAmapSecurity } from './utils/amapSecurity'

bootstrapAmapSecurity()

createApp(App).use(router).use(Antd).mount('#app')

