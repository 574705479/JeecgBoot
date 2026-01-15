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
  // 新版用户聊天（支持AI和人工客服，不需要appId参数）
  {
    path: "/cs/chat",
    name: "cs-user-chat",
    component: () => import("/@/views/super/airag/cs/userChat/index.vue"),
    meta: {
      title: '在线客服',
      ignoreAuth: true,
    },
  },  
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
