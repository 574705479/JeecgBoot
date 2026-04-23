/**
 * 访客端入口
 *
 * 与主项目 src/main.ts 比较：
 *   - 不引 pinia（访客端无登录态、无全局 store）
 *   - 不引 i18n
 *   - 不引 vxe-table / icons 全集
 *   - 不引 directives 全集（仅按需注册 v-cse-html）
 *   - 不注册 SW / PWA
 */
import { createApp } from 'vue';
import App from '/@/App.vue';
import router from '/@/router';
import { vCseHtml } from '/@/directives/cseHtmlImg';

const app = createApp(App);

app.directive('cse-html', vCseHtml as any);

app.use(router);
app.mount('#app');
