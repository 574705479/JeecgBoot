<template>
  <div class="message-board-records">
    <a-card title="留言记录" :bordered="false">
      <!-- 筛选 -->
      <div class="filter-bar">
        <a-select v-model:value="filterStatus" style="width: 120px;" @change="loadData" placeholder="全部状态">
          <a-select-option :value="undefined">全部状态</a-select-option>
          <a-select-option :value="0">待回复</a-select-option>
          <a-select-option :value="1">已回复</a-select-option>
        </a-select>
        <a-button type="primary" @click="loadData">
          查询
        </a-button>
      </div>

      <!-- 列表 -->
      <a-table 
        :dataSource="dataList" 
        :columns="columns" 
        :loading="loading"
        :pagination="pagination"
        @change="onTableChange"
        rowKey="id"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 0 ? 'orange' : 'green'">
              {{ record.status === 0 ? '待回复' : '已回复' }}
            </a-tag>
          </template>
          <template v-if="column.dataIndex === 'content'">
            <span class="content-ellipsis">{{ record.content || '-' }}</span>
          </template>
          <template v-if="column.dataIndex === 'action'">
            <a-space>
              <a @click="showDetail(record)">查看</a>
              <a v-if="record.status === 0" @click="showReplyModal(record)">回复</a>
              <a-popconfirm v-if="record.status === 1" title="确定撤回该回复？" ok-text="确定" cancel-text="取消" @confirm="recallReply(record)">
                <a style="color: #f5222d">撤回回复</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 详情弹窗 -->
    <a-modal 
      v-model:open="detailVisible" 
      title="留言详情" 
      :footer="null"
      width="600px"
    >
      <template v-if="currentRecord">
        <a-descriptions bordered size="small" :column="1">
          <a-descriptions-item label="姓名">{{ currentRecord.name || '-' }}</a-descriptions-item>
          <a-descriptions-item label="手机">{{ currentRecord.phone || '-' }}</a-descriptions-item>
          <a-descriptions-item label="邮箱">{{ currentRecord.email || '-' }}</a-descriptions-item>
          <a-descriptions-item label="QQ">{{ currentRecord.qq || '-' }}</a-descriptions-item>
          <a-descriptions-item label="微信">{{ currentRecord.wechat || '-' }}</a-descriptions-item>
          <a-descriptions-item label="留言内容">{{ currentRecord.content || '-' }}</a-descriptions-item>
          <a-descriptions-item label="图片" v-if="currentRecord.imageUrl">
            <a-image :src="currentRecord.imageUrl" :width="200" />
          </a-descriptions-item>
          <a-descriptions-item label="留言时间">{{ currentRecord.createTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="currentRecord.status === 0 ? 'orange' : 'green'">
              {{ currentRecord.status === 0 ? '待回复' : '已回复' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="回复内容" v-if="currentRecord.reply">
            {{ currentRecord.reply }}
          </a-descriptions-item>
          <a-descriptions-item label="回复时间" v-if="currentRecord.replyTime">
            {{ currentRecord.replyTime }}
          </a-descriptions-item>
          <a-descriptions-item label="操作" v-if="currentRecord.status === 1">
            <a-popconfirm title="确定撤回该回复？" ok-text="确定" cancel-text="取消" @confirm="recallReply(currentRecord)">
              <a-button type="link" danger size="small">撤回回复</a-button>
            </a-popconfirm>
          </a-descriptions-item>
        </a-descriptions>

        <div v-if="currentRecord.status === 0" style="margin-top: 16px;">
          <a-divider>回复留言</a-divider>
          <a-textarea 
            v-model:value="replyContent" 
            placeholder="请输入回复内容"
            :rows="4"
          />
          <div style="margin-top: 8px; text-align: right;">
            <a-button type="primary" @click="submitReply" :loading="replying">
              提交回复
            </a-button>
          </div>
        </div>
      </template>
    </a-modal>

    <!-- 快捷回复弹窗 -->
    <a-modal 
      v-model:open="replyVisible" 
      title="回复留言"
      @ok="submitReply"
      :confirmLoading="replying"
    >
      <div style="margin-bottom: 8px;">
        <strong>{{ currentRecord?.name || '访客' }}</strong> 的留言：{{ currentRecord?.content || '-' }}
      </div>
      <a-textarea 
        v-model:value="replyContent" 
        placeholder="请输入回复内容"
        :rows="4"
      />
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';

const { createMessage: message } = useMessage();

const loading = ref(false);
const replying = ref(false);
const detailVisible = ref(false);
const replyVisible = ref(false);
const filterStatus = ref<number | undefined>(undefined);
const replyContent = ref('');
const currentRecord = ref<any>(null);
const dataList = ref<any[]>([]);

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
});

const columns = [
  { title: '姓名', dataIndex: 'name', width: 100 },
  { title: '手机', dataIndex: 'phone', width: 120 },
  { title: '留言内容', dataIndex: 'content', ellipsis: true },
  { title: '状态', dataIndex: 'status', width: 90, align: 'center' as const },
  { title: '留言时间', dataIndex: 'createTime', width: 170 },
  { title: '回复时间', dataIndex: 'replyTime', width: 170 },
  { title: '操作', dataIndex: 'action', width: 120, align: 'center' as const },
];

onMounted(async () => {
  await loadData();
});

async function loadData() {
  loading.value = true;
  try {
    const params: Record<string, any> = {
      pageNo: pagination.current,
      pageSize: pagination.pageSize,
    };
    if (filterStatus.value !== undefined) {
      params.status = filterStatus.value;
    }
    const res = await defHttp.get({ url: '/cs/leaveMessage/list', params });
    const data = res?.result || res;
    if (data) {
      dataList.value = data.records || [];
      pagination.total = data.total || 0;
    }
  } catch (e) {
    console.error('加载留言记录失败', e);
  } finally {
    loading.value = false;
  }
}

function onTableChange(pag: any) {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  loadData();
}

function showDetail(record: any) {
  currentRecord.value = record;
  replyContent.value = '';
  detailVisible.value = true;
}

function showReplyModal(record: any) {
  currentRecord.value = record;
  replyContent.value = '';
  replyVisible.value = true;
}

async function recallReply(record: any) {
  try {
    await defHttp.put({ url: `/cs/leaveMessage/${record.id}/recallReply` });
    message.success('撤回成功');
    detailVisible.value = false;
    await loadData();
  } catch (e) {
    console.error('撤回失败', e);
    message.error('撤回失败');
  }
}

async function submitReply() {
  if (!replyContent.value.trim()) {
    message.warning('请输入回复内容');
    return;
  }
  if (!currentRecord.value?.id) return;

  replying.value = true;
  try {
    await defHttp.put({
      url: `/cs/leaveMessage/${currentRecord.value.id}/reply`,
      data: { reply: replyContent.value },
    });
    message.success('回复成功');
    replyVisible.value = false;
    detailVisible.value = false;
    replyContent.value = '';
    await loadData();
  } catch (e) {
    console.error('回复失败', e);
    message.error('回复失败');
  } finally {
    replying.value = false;
  }
}
</script>

<style lang="less" scoped>
.message-board-records {
  padding: 16px;

  .filter-bar {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
  }

  .content-ellipsis {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}
</style>
