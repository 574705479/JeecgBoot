import type { App } from 'vue';
import { router } from "/@/router";
import type { RouteRecordRaw } from "vue-router";
import { LAYOUT } from "@/router/constant";

const ChatRoutes: RouteRecordRaw[] = [
  {
    path: "/ai/app/chat/:appId",
    name: "ai-chat-@appId-@modeType",
    component: () => import("/@/views/super/airag/aiapp/chat/AiChat.vue"),
    meta: {
      title: 'AI聊天',
      ignoreAuth: true,
    },
  },
  // 访客端 /cs/userChat（含 alias /cs/chat）路由已彻底下线，
  // 由独立子项目 jeecgboot-vue3-visitor 接管，build:with-visitor 会把访客端产物
  // 输出到 dist/cs/userChat/index.html，由 nginx 优先匹配真实文件直接返回访客端 SPA。
  {
    path: "/ai/app/chatIcon/:appId",
    name: "ai-chatIcon-@appId",
    component: () => import("/@/views/super/airag/aiapp/chat/AiChatIcon.vue"),
    meta: {
      title: 'AI聊天',
      ignoreAuth: true,
    },
  },
  {
    path: '/ai/chat',
    name: 'aiChat',
    component: LAYOUT,
    meta: {
      title: 'ai聊天',
    },
    children: [
      {
        path: "/ai/chat/:appId",
        name: "ai-chat-@appId",
        component: () => import("/@/views/super/airag/aiapp/chat/AiChat.vue"),
        meta: {
          title:'AI助手',
          ignoreAuth: false,
        },
      },
      {
        path: "/ai/chat",
        name: "ai-chat",
        component: () => import("/@/views/super/airag/aiapp/chat/AiChat.vue"),
        meta: {
          title:'AI助手',
          ignoreAuth: false,
        },
      }
    ],
  },
]

/** 注册路由 */
export async function register(app: App) {
  await registerMyAppRouter(app);
  console.log('[聊天路由] 注册完成！');
}

async function registerMyAppRouter(_: App) {
  for(let appRoute of ChatRoutes){
    await router.addRoute(appRoute);
  }
}
