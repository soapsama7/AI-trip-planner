import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import ResultView from '@/views/ResultView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView },
    { path: '/result/:taskId', component: ResultView },
    { path: '/result', redirect: '/' }
  ]
})

export default router

