<template>
  <a-modal
    v-model:open="visible"
    title="访客详情"
    width="600px"
    :footer="null"
    @cancel="handleClose"
  >
    <a-spin :spinning="loading">
      <div class="visitor-detail">
        <!-- 头部信息 -->
        <div class="visitor-header">
          <a-avatar :size="64" class="visitor-avatar">
            <template #icon><UserOutlined /></template>
          </a-avatar>
          <div class="visitor-basic">
            <div class="visitor-name">
              <span class="name">{{ visitor.nickname || visitor.userId }}</span>
              <a-tag v-if="visitor.level === 3" color="gold">VIP</a-tag>
              <a-tag v-else-if="visitor.level === 2" color="blue">重要</a-tag>
              <StarFilled v-if="visitor.star === 1" class="star-icon" @click="toggleStar" />
              <StarOutlined v-else class="star-icon-empty" @click="toggleStar" />
            </div>
            <div class="visitor-id">ID: {{ visitor.userId }}</div>
          </div>
        </div>

        <!-- 编辑表单 -->
        <a-form :model="formData" layout="vertical" class="visitor-form">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="备注昵称">
                <a-input v-model:value="formData.nickname" placeholder="给访客起个备注名" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="真实姓名">
                <a-input v-model:value="formData.realName" placeholder="访客真实姓名" />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="手机号">
                <a-input v-model:value="formData.phone" placeholder="手机号码" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="邮箱">
                <a-input v-model:value="formData.email" placeholder="电子邮箱" />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="性别">
                <a-select v-model:value="formData.gender" placeholder="选择性别">
                  <a-select-option :value="0">未知</a-select-option>
                  <a-select-option :value="1">男</a-select-option>
                  <a-select-option :value="2">女</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="客户等级">
                <a-select v-model:value="formData.level" placeholder="选择等级">
                  <a-select-option :value="1">普通</a-select-option>
                  <a-select-option :value="2">重要</a-select-option>
                  <a-select-option :value="3">VIP</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="公司/组织">
                <a-input v-model:value="formData.company" placeholder="所属公司或组织" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="职位">
                <a-input v-model:value="formData.position" placeholder="职位" />
              </a-form-item>
            </a-col>
          </a-row>

          <a-form-item label="地址">
            <a-input v-model:value="formData.address" placeholder="详细地址" />
          </a-form-item>

          <a-form-item label="标签">
            <div class="tags-container">
              <a-tag
                v-for="tag in tagList"
                :key="tag"
                closable
                @close="removeTag(tag)"
              >
                {{ tag }}
              </a-tag>
              <a-input
                v-if="inputVisible"
                ref="inputRef"
                v-model:value="inputValue"
                type="text"
                size="small"
                :style="{ width: '78px' }"
                @blur="handleInputConfirm"
                @keyup.enter="handleInputConfirm"
              />
              <a-tag v-else class="add-tag" @click="showInput">
                <PlusOutlined /> 添加标签
              </a-tag>
            </div>
          </a-form-item>

          <a-form-item label="备注说明">
            <a-textarea
              v-model:value="formData.notes"
              placeholder="详细备注信息..."
              :rows="3"
            />
          </a-form-item>
        </a-form>

        <!-- 统计信息 -->
        <a-divider>访问统计</a-divider>
        <a-descriptions :column="2" size="small">
          <a-descriptions-item label="首次访问">
            {{ visitor.firstVisitTime || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="最后访问">
            {{ visitor.lastVisitTime || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="访问次数">
            {{ visitor.visitCount || 0 }} 次
          </a-descriptions-item>
          <a-descriptions-item label="会话数">
            {{ visitor.conversationCount || 0 }} 次
          </a-descriptions-item>
          <a-descriptions-item label="来源渠道">
            {{ visitor.source || '-' }}
          </a-descriptions-item>
        </a-descriptions>

        <!-- 操作按钮 -->
        <div class="visitor-actions">
          <a-button @click="handleClose">取消</a-button>
          <a-button type="primary" :loading="saving" @click="handleSave">
            保存
          </a-button>
        </div>
      </div>
    </a-spin>
  </a-modal>
</template>

<script lang="ts" setup>
import { ref, reactive, watch, nextTick } from 'vue';
import { message } from 'ant-design-vue';
import { UserOutlined, StarFilled, StarOutlined, PlusOutlined } from '@ant-design/icons-vue';
import { defHttp } from '/@/utils/http/axios';

const props = defineProps<{
  open: boolean;
  appId: string;
  userId: string;
}>();

const emit = defineEmits(['update:open', 'saved']);

const visible = ref(false);
const loading = ref(false);
const saving = ref(false);
const visitor = ref<any>({});
const formData = reactive<any>({});
const tagList = ref<string[]>([]);
const inputVisible = ref(false);
const inputValue = ref('');
const inputRef = ref();

// 监听open变化
watch(() => props.open, (val) => {
  visible.value = val;
  if (val && props.appId && props.userId) {
    loadVisitor();
  }
});

// 监听visible变化同步到父组件
watch(visible, (val) => {
  emit('update:open', val);
});

// 加载访客信息
async function loadVisitor() {
  loading.value = true;
  try {
    const res = await defHttp.get({
      url: '/airag/cs/visitor/getByUser',
      params: { appId: props.appId, userId: props.userId }
    });
    
    if (res) {
      visitor.value = res;
      // 填充表单
      Object.assign(formData, {
        id: res.id,
        nickname: res.nickname,
        realName: res.realName,
        phone: res.phone,
        email: res.email,
        gender: res.gender || 0,
        level: res.level || 1,
        company: res.company,
        position: res.position,
        address: res.address,
        notes: res.notes
      });
      // 解析标签
      if (res.tags) {
        try {
          tagList.value = JSON.parse(res.tags);
        } catch {
          tagList.value = [];
        }
      } else {
        tagList.value = [];
      }
    }
  } catch (e: any) {
    // 访客不存在，创建新访客
    visitor.value = {
      appId: props.appId,
      userId: props.userId
    };
    Object.assign(formData, {
      nickname: '',
      realName: '',
      phone: '',
      email: '',
      gender: 0,
      level: 1,
      company: '',
      position: '',
      address: '',
      notes: ''
    });
    tagList.value = [];
  } finally {
    loading.value = false;
  }
}

// 切换星标
async function toggleStar() {
  if (!visitor.value.id) return;
  try {
    await defHttp.post({
      url: '/airag/cs/visitor/toggleStar',
      params: { id: visitor.value.id }
    });
    visitor.value.star = visitor.value.star === 1 ? 0 : 1;
    message.success('操作成功');
  } catch {
    message.error('操作失败');
  }
}

// 标签相关
function showInput() {
  inputVisible.value = true;
  nextTick(() => {
    inputRef.value?.focus();
  });
}

function handleInputConfirm() {
  if (inputValue.value && !tagList.value.includes(inputValue.value)) {
    tagList.value.push(inputValue.value);
  }
  inputVisible.value = false;
  inputValue.value = '';
}

function removeTag(tag: string) {
  tagList.value = tagList.value.filter(t => t !== tag);
}

// 保存
async function handleSave() {
  saving.value = true;
  try {
    const data = {
      ...formData,
      appId: props.appId,
      userId: props.userId,
      tags: JSON.stringify(tagList.value)
    };
    
    if (visitor.value.id) {
      // 更新
      await defHttp.post({
        url: '/airag/cs/visitor/update',
        data
      });
    } else {
      // 创建（通过getOrCreate自动创建）
      await defHttp.post({
        url: '/airag/cs/visitor/update',
        data
      });
    }
    
    message.success('保存成功');
    emit('saved');
    handleClose();
  } catch {
    message.error('保存失败');
  } finally {
    saving.value = false;
  }
}

function handleClose() {
  visible.value = false;
}
</script>

<style lang="less" scoped>
.visitor-detail {
  .visitor-header {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 24px;
    padding-bottom: 16px;
    border-bottom: 1px solid #f0f0f0;

    .visitor-avatar {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }

    .visitor-basic {
      flex: 1;

      .visitor-name {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 18px;
        font-weight: 500;

        .star-icon {
          color: #faad14;
          cursor: pointer;
          font-size: 18px;
        }

        .star-icon-empty {
          color: #d9d9d9;
          cursor: pointer;
          font-size: 18px;

          &:hover {
            color: #faad14;
          }
        }
      }

      .visitor-id {
        color: #999;
        font-size: 12px;
        margin-top: 4px;
      }
    }
  }

  .visitor-form {
    .tags-container {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;

      .add-tag {
        background: #fff;
        border-style: dashed;
        cursor: pointer;
      }
    }
  }

  .visitor-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 24px;
    padding-top: 16px;
    border-top: 1px solid #f0f0f0;
  }
}
</style>
