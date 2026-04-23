<template>
  <div v-if="visible" class="emoji-picker" ref="pickerRef">
    <div class="emoji-tabs">
      <span
        v-for="(cat, idx) in categories"
        :key="idx"
        class="emoji-tab"
        :class="{ active: activeTab === idx }"
        @click="activeTab = idx"
        :title="cat.name"
      >{{ cat.icon }}</span>
    </div>
    <div class="emoji-grid">
      <span
        v-for="(emoji, i) in categories[activeTab].emojis"
        :key="i"
        class="emoji-item"
        @click="$emit('select', emoji)"
        :title="emoji"
      >{{ emoji }}</span>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';

defineProps<{ visible: boolean }>();
const emit = defineEmits<{
  (e: 'select', emoji: string): void;
  (e: 'close'): void;
}>();

const activeTab = ref(0);
const pickerRef = ref<HTMLElement | null>(null);

const categories = [
  {
    name: '笑脸与人物',
    icon: '😊',
    emojis: [
      '😀','😃','😄','😁','😆','😅','🤣','😂','🙂','🙃',
      '😉','😊','😇','🥰','😍','🤩','😘','😗','😚','😙',
      '🥲','😋','😛','😜','🤪','😝','🤑','🤗','🤭','🤫',
      '🤔','🫡','🤐','🤨','😐','😑','😶','🫠','😏','😒',
      '🙄','😬','🤥','😌','😔','😪','🤤','😴','😷','🤒',
      '🤕','🤢','🤮','🥵','🥶','🥴','😵','🤯','🤠','🥳',
      '🥸','😎','🤓','🧐','😕','🫤','😟','🙁','😮','😯',
      '😲','😳','🥺','🥹','😦','😧','😨','😰','😥','😢',
      '😭','😱','😖','😣','😞','😓','😩','😫','🥱','😤',
    ],
  },
  {
    name: '手势与身体',
    icon: '👋',
    emojis: [
      '👋','🤚','🖐','✋','🖖','🫱','🫲','🫳','🫴','👌',
      '🤌','🤏','✌️','🤞','🫰','🤟','🤘','🤙','👈','👉',
      '👆','🖕','👇','☝️','🫵','👍','👎','✊','👊','🤛',
      '🤜','👏','🙌','🫶','👐','🤲','🤝','🙏','✍️','💅',
      '🤳','💪','🦾','🦿','🦵','🦶','👂','🦻','👃','🧠',
      '🫀','🫁','🦷','🦴','👀','👁','👅','👄','🫦','💋',
    ],
  },
  {
    name: '动物与自然',
    icon: '🐱',
    emojis: [
      '🐶','🐱','🐭','🐹','🐰','🦊','🐻','🐼','🐻‍❄️','🐨',
      '🐯','🦁','🐮','🐷','🐽','🐸','🐵','🙈','🙉','🙊',
      '🐒','🐔','🐧','🐦','🐤','🐣','🐥','🦆','🦅','🦉',
      '🦇','🐺','🐗','🐴','🦄','🐝','🪱','🐛','🦋','🐌',
      '🐞','🐜','🪰','🪲','🪳','🦟','🦗','🕷','🌸','🌺',
      '🌻','🌹','🌷','🌼','🌱','🪴','🌲','🌳','🌴','🍀',
    ],
  },
  {
    name: '食物与饮品',
    icon: '🍔',
    emojis: [
      '🍎','🍐','🍊','🍋','🍌','🍉','🍇','🍓','🫐','🍈',
      '🍒','🍑','🥭','🍍','🥥','🥝','🍅','🥑','🍆','🌶',
      '🫑','🥒','🥬','🥦','🧅','🧄','🍄','🥜','🫘','🌰',
      '🍞','🥐','🥖','🫓','🥨','🥯','🥞','🧇','🧀','🍖',
      '🍗','🥩','🥓','🍔','🍟','🍕','🌭','🥪','🌮','🌯',
      '🫔','🥙','🧆','🥚','🍳','🥘','🍲','🫕','🥣','🥗',
      '🍿','🧈','🍱','🍘','🍙','🍚','🍛','🍜','🍝','🍠',
      '☕','🍵','🧋','🥤','🍺','🍻','🥂','🍷','🍸','🍹',
    ],
  },
  {
    name: '旅行与地点',
    icon: '✈️',
    emojis: [
      '🚗','🚕','🚙','🚌','🚎','🏎','🚓','🚑','🚒','🚐',
      '🛻','🚚','🚛','🚜','🏍','🛵','🚲','🛴','🛹','🛼',
      '🚁','🛸','🚀','✈️','🛩','🚢','⛴','🛳','🚤','⛵',
      '🏠','🏡','🏢','🏣','🏤','🏥','🏦','🏨','🏩','🏪',
      '🏫','🏬','🏭','🏯','🏰','💒','🗼','🗽','⛪','🕌',
      '🛕','🕍','⛩','🕋','⛲','⛺','🌁','🌃','🏙','🌄',
      '🌅','🌆','🌇','🌉','🗻','🏔','🌋','🏕','🏖','🏜',
    ],
  },
  {
    name: '活动与运动',
    icon: '⚽',
    emojis: [
      '⚽','🏀','🏈','⚾','🥎','🎾','🏐','🏉','🥏','🎱',
      '🪀','🏓','🏸','🏒','🏑','🥍','🏏','🪃','🥅','⛳',
      '🪁','🏹','🎣','🤿','🥊','🥋','🎽','🛹','🛼','🛷',
      '⛸','🥌','🎿','⛷','🏂','🪂','🏋','🤼','🤸','⛹',
      '🤺','🤾','🏌','🏇','🧘','🏄','🏊','🤽','🚣','🧗',
      '🎪','🎭','🎨','🎬','🎤','🎧','🎼','🎹','🥁','🪘',
      '🎷','🎺','🪗','🎸','🪕','🎻','🎲','♟','🎯','🎳',
    ],
  },
  {
    name: '物品',
    icon: '💡',
    emojis: [
      '💡','🔦','🏮','🪔','📱','💻','⌨️','🖥','🖨','🖱',
      '🖲','💾','💿','📀','📷','📸','📹','🎥','📽','🎞',
      '📞','☎️','📟','📠','📺','📻','🎙','🎚','🎛','🧭',
      '⏱','⏲','⏰','🕰','⌛','⏳','📡','🔋','🔌','💰',
      '🪙','💴','💵','💶','💷','💸','💳','🧾','💎','⚖️',
      '🪜','🧰','🪛','🔧','🔨','⚒','🛠','⛏','🪚','🔩',
      '📎','🖇','📏','📐','✂️','🗑','📦','📫','📬','📮',
    ],
  },
  {
    name: '符号与标志',
    icon: '❤️',
    emojis: [
      '❤️','🧡','💛','💚','💙','💜','🖤','🤍','🤎','💔',
      '❣️','💕','💞','💓','💗','💖','💘','💝','💟','☮️',
      '✝️','☪️','🕉','☸️','✡️','🔯','🕎','☯️','☦️','🛐',
      '⛎','♈','♉','♊','♋','♌','♍','♎','♏','♐',
      '♑','♒','♓','🆔','⚛️','🉑','☢️','☣️','📴','📳',
      '🈶','🈚','🈸','🈺','🈷️','✴️','🆚','💮','🉐','㊙️',
      '㊗️','🈴','🈵','🈹','🈲','🅰️','🅱️','🆎','🆑','🅾️',
      '✅','❌','❓','❔','❕','❗','⭕','🚫','💯','🔥',
      '⭐','🌟','✨','💫','🎉','🎊','🎁','🏆','🏅','🥇',
    ],
  },
];

function handleClickOutside(e: MouseEvent) {
  if (pickerRef.value && !pickerRef.value.contains(e.target as Node)) {
    emit('close');
  }
}

onMounted(() => {
  document.addEventListener('mousedown', handleClickOutside);
});
onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleClickOutside);
});
</script>

<style scoped>
.emoji-picker {
  position: absolute;
  bottom: 100%;
  left: 0;
  z-index: 1000;
  width: 340px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
  overflow: hidden;
}
.emoji-tabs {
  display: flex;
  border-bottom: 1px solid #f0f0f0;
  padding: 6px 8px 4px;
  gap: 2px;
  background: #fafafa;
}
.emoji-tab {
  flex: 1;
  text-align: center;
  font-size: 20px;
  cursor: pointer;
  padding: 4px 0;
  border-radius: 6px;
  transition: background 0.2s;
  filter: saturate(1.3) contrast(1.05);
}
.emoji-tab:hover { background: #e6f4ff; }
.emoji-tab.active { background: #e6f4ff; }
.emoji-grid {
  display: flex;
  flex-wrap: wrap;
  padding: 8px;
  height: 260px;
  overflow-y: auto;
  gap: 2px;
}
.emoji-item {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.15s, transform 0.1s;
  color: initial;
  font-family: 'Apple Color Emoji', 'Segoe UI Emoji', 'Noto Color Emoji', sans-serif;
  font-style: normal;
  -webkit-text-stroke: 0;
  text-rendering: optimizeLegibility;
  filter: saturate(1.3) contrast(1.05);
}
.emoji-item:hover {
  background: #e8f0fe;
  transform: scale(1.15);
  filter: saturate(1.5) contrast(1.1);
}
</style>
