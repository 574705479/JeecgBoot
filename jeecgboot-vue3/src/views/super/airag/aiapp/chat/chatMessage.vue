<template>
  <!-- 系统消息样式 - 居中显示，无头像 -->
  <div class="chat system-message" v-if="inversion === 'system' && getText">
    <div class="system-content">
      <span class="system-text">{{ getText }}</span>
    </div>
  </div>
  <!-- 普通消息样式 -->
  <div class="chat" :class="getChatClass()" v-else-if="getText || (props.presetQuestion && props.presetQuestion.length>0)">
    <div class="avatar" :class="getAvatarClass()" v-if="showAvatar !== 'no' && showAvatar !== false">
      <img v-if="inversion === 'user'" :src="avatar()" />
      <img v-else-if="inversion === 'assistant'" :src="getAgentAvatar()" />
      <img v-else :src="getAiImg()" />
    </div>
    <div class="content">
      <p class="date" v-if="showAvatar !== 'no'">
        <span v-if="inversion === 'ai'" class="sender-name sender-ai">{{appData.name || 'AI助手'}}</span>
        <span v-if="inversion === 'assistant'" class="sender-name sender-agent">{{ senderName || '客服' }}</span>
        <span>{{ dateTime }}</span>
      </p>
      <div v-if="inversion === 'user' && images && images.length>0" class="images">
          <div v-for="(item,index) in images" :key="index" class="image" @click="handlePreview(item)">
            <img :src="getImageUrl(item)"/>
          </div>
      </div>
      <div v-if="inversion === 'ai' && retrievalText && loading" class="retrieval">
        {{retrievalText}}
      </div>
      <div v-if="inversion === 'ai' && isCard" class="card">
        <a-row>
          <a-col :xl="6" :lg="8" :md="10" :sm="24" style="flex:1" v-for="item in getCardList()">
            <a-card class="ai-card" @click="aiCardHandleClick(item.linkUrl)">
               <div class="ai-card-title">{{item.productName}}</div>
               <div class="ai-card-img">
                 <img :src="item.productImage">
               </div>
               <span class="ai-card-desc">{{item.descr}}</span>
            </a-card>
          </a-col>
        </a-row>
      </div>
      <div class="thinkArea" style="margin-bottom: 10px" v-if="!isCard && (eventType === 'thinking' || eventType === 'thinking_end')">
        <a-collapse v-model:activeKey="activeKey" ghost>
          <a-collapse-panel :key="uuid" :header="loading?'正在思考中':'思考结束'">
            <ThinkText :text="text" :inversion="inversion" :error="error" :loading="loading"></ThinkText>
          </a-collapse-panel>
        </a-collapse>
      </div>
      <div class="msgArea" v-else-if="!isCard" :class="showAvatar == 'no' ? 'hidden-avatar' : ''">
        <chatText :text="text" :inversion="inversion" :error="error" :loading="loading" :referenceKnowledge="referenceKnowledge"></chatText>
      </div>
      <div v-if="presetQuestion" v-for="item in presetQuestion" class="question" @click="presetQuestionClick(item.descr)">
        <span>{{item.descr}}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import chatText from './chatText.vue';
  import ThinkText from './ThinkText.vue';
  import defaultAvatar from "@/assets/images/ai/avatar.jpg";
  import { useUserStore } from '/@/store/modules/user';
  import defaultImg from '../img/ailogo.png';
  import { ref } from 'vue';
  import { buildUUID } from '/@/utils/uuid';
  import { getFileAccessHttpUrl } from '/@/utils/common/compUtils';
  import { createImgPreview } from "@/components/Preview";
  import { computed } from "vue";

  const props = defineProps(['dateTime', 'text', 'inversion', 'error', 'loading','appData','presetQuestion','images','retrievalText', 'referenceKnowledge', 'eventType', 'showAvatar', 'senderName', 'senderAvatar']);
  
  const uuid = ref<any>(buildUUID());
  const activeKey = ref<any>(uuid.value);
  const getText = computed(()=>{
    let text = props.text || props.retrievalText;
    if(text){
      text = text.trim();
    }
    return text;
  })

  const isCard = computed(() => {
    let text = props.text;
    if (text && text.indexOf('::card::') != -1) {
      return true;
    }
    return false;
  });

  const { userInfo } = useUserStore();
  const avatar = () => {
    return getFileAccessHttpUrl(userInfo?.avatar) || defaultAvatar;
  };
  const emit = defineEmits(['send']);
  const getAiImg = () => {
    return getFileAccessHttpUrl(props.appData?.icon) || defaultImg;
  };
  
  // 获取客服头像
  import agentDefaultAvatar from "@/assets/images/ai/avatar.jpg"; // 复用默认头像或可以换一个
  const getAgentAvatar = () => {
    return getFileAccessHttpUrl(props.senderAvatar) || agentDefaultAvatar;
  };
  
  // 获取聊天样式类
  const getChatClass = () => {
    if (props.inversion === 'user') return 'self';
    if (props.inversion === 'assistant') return 'chatgpt agent-message';
    return 'chatgpt';
  };
  
  // 获取头像样式类
  const getAvatarClass = () => {
    if (props.inversion === 'assistant') return 'avatar-agent';
    if (props.inversion === 'ai') return 'avatar-ai';
    return '';
  };

  /**
   * 预设问题点击事件
   *
   */
  function presetQuestionClick(descr) {
    emit("send",descr)
  }

  /**
   * 获取图片
   *
   * @param item
   */
  function getImageUrl(item) {
    let url = item;
    if(item.hasOwnProperty('url')){
      url = item.url;
    }
    if(item.hasOwnProperty('base64Data') && item.base64Data){
      return item.base64Data;
    }
    return getFileAccessHttpUrl(url);
  }

  /**
   * 图片预览
   * @param url
   */
  function handlePreview(url){
    const onImgLoad = ({ index, url, dom }) => {
      console.log(`第${index + 1}张图片已加载，URL为：${url}`, dom);
    };
    let imageList = [getImageUrl(url)];
    createImgPreview({ imageList: imageList, defaultWidth: 700, rememberState: true, onImgLoad });
  }

  /**
   * 获取卡片列表
   */
  function getCardList() {
    let text = props.text;
    let card = text.replace('::card::', '').replace(/\s+/g, '');
    try {
      return JSON.parse(card);
    } catch (e) {
      console.log(e)
      return '';
    }
  }

  /**
   * ai卡片点击事件
   * @param url
   */
  function aiCardHandleClick(url){
    window.open(url,'_blank');
  }
</script>

<style lang="less" scoped>
  .chat {
    display: flex;
    margin-bottom: 1.5rem;
    &.self {
      flex-direction: row-reverse;
      .avatar {
        margin-right: 0;
        margin-left: 10px;
      }
      .msgArea {
        flex-direction: row-reverse;
        margin-bottom: 6px;
      }
      .thinkArea{
        margin: 0;
        padding: 5px 0 5px 22px;
        position: relative;
      }
      .date {
        text-align: right;
      }
    }
  }
  :deep(.ant-collapse-header){
    padding: 0 !important;
  }
  .hidden-avatar{
    left: 44px;
    position: relative;
    top: -18px;
  }
  .avatar {
    flex: none;
    margin-right: 10px;
    img {
      width: 34px;
      height: 34px;
      border-radius: 50%;
      overflow: hidden;
    }
    svg {
      font-size: 28px;
    }
  }
  .content {
    width: 90%;
    .date {
      color: #b4bbc4;
      font-size: 0.75rem;
      margin-bottom: 10px;
    }
    .msgArea {
      display: flex;
    }
  }

  .question{
    margin-top: 10px;
    border-radius: 0.375rem;
    padding-top: 0.5rem;
    padding-bottom: 0.5rem;
    padding-left: 0.75rem;
    padding-right: 0.75rem;
    background-color: #ffffff;
    font-size: 0.875rem;
    line-height: 1.25rem;
    cursor: pointer;
    border: 1px solid #f0f0f0;
    box-shadow: 0 2px 4px #e6e6e6;
  }

  .images{
    margin-bottom: 10px;
    flex-wrap: wrap;
    display: flex;
    gap: 10px;
    justify-content: end;
    .image{
      width: 120px;
      height: 80px;
      cursor: pointer;
      img{
        width: 100%;
        height: 100%;
        object-fit: cover;
        border-radius: 4px;
      }
    }
  }
  .retrieval,
  .card {
    background-color: #f4f6f8;
    font-size: 0.875rem;
    line-height: 1.25rem;
    border-radius: 0.375rem;
    padding-top: 0.5rem;
    padding-bottom: 0.5rem;
    padding-left: 0.75rem;
    padding-right: 0.75rem;
  }
  .retrieval:after{
    animation: blink 1s steps(5, start) infinite;
    color: #000;
    content: '_';
    font-weight: 700;
    margin-left: 3px;
    vertical-align: baseline;
  }
  .card{
    width: 100%;
    background-color: unset;
  }
  .ai-card{
     width: 98%;
     height: 100%;
     cursor: pointer;
    .ai-card-title{
      width: 100%;
      line-height: 20px;
      letter-spacing: 0;
      white-space: pre-line;
      overflow: hidden;
      display: -webkit-box;
      text-overflow: ellipsis;
      -webkit-box-orient: vertical;
      font-weight: 600;
      font-size: 18px;
      text-align: left;
      color: #191919;
      -webkit-line-clamp: 1;
    }
    .ai-card-img{
      margin-top: 10px;
      background-color: transparent;
      border-radius: 8px;
      display: flex;
      width: 100%;
      height: max-content;
    }
    .ai-card-desc{
      margin-top: 10px;
      width: 100%;
      font-size: 14px;
      font-weight: 400;
      line-height: 20px;
      letter-spacing: 0;
      white-space: pre-line;
      -webkit-box-orient: vertical;
      overflow: hidden;
      display: -webkit-box;
      text-overflow: ellipsis;
      text-align: left;
      color: #666f;
      -webkit-line-clamp: 3;
    }
  }
  @media (max-width: 600px) {
    .content{
      width: 100%;
    }
  }
  
  /* 系统消息样式 - 居中显示 */
  .system-message {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 8px 0;
    margin: 8px 0;
    
    .system-content {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      padding: 6px 16px;
      background: rgba(0, 0, 0, 0.04);
      border-radius: 16px;
      max-width: 80%;
      
      .system-text {
        font-size: 12px;
        color: #909399;
        line-height: 1.5;
        text-align: center;
        word-break: break-word;
      }
    }
  }
  
  /* 客服消息样式 */
  .agent-message {
    :deep(.chatTextArea) {
      background: linear-gradient(135deg, #667eea, #764ba2) !important;
      color: #fff !important;
    }
  }
  
  /* 发送者名称样式 */
  .sender-name {
    margin-right: 10px;
    font-weight: 500;
  }
  
  .sender-ai {
    color: #52c41a;
  }
  
  .sender-agent {
    color: #667eea;
  }
  
  /* 头像样式 */
  .avatar-agent {
    img {
      border: 2px solid #667eea;
    }
  }
  
  .avatar-ai {
    img {
      border: 2px solid #52c41a;
    }
  }
</style>
