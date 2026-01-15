<template>
  <div class="visitor-container">
    <!-- 搜索区域 -->
    <div class="search-area">
      <a-space>
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索昵称/姓名/手机号"
          style="width: 250px"
          @search="handleSearch"
          allowClear
        />
        <a-select
          v-model:value="searchLevel"
          placeholder="客户等级"
          style="width: 120px"
          allowClear
          @change="handleSearch"
        >
          <a-select-option :value="1">普通</a-select-option>
          <a-select-option :value="2">重要</a-select-option>
          <a-select-option :value="3">VIP</a-select-option>
        </a-select>
        <a-checkbox v-model:checked="onlyStar" @change="handleSearch">
          仅星标
        </a-checkbox>
        <a-button type="primary" @click="handleSearch">
          <SearchOutlined /> 搜索
        </a-button>
      </a-space>
    </div>

    <!-- 访客列表 -->
    <a-table
      :dataSource="visitorList"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      rowKey="id"
      @change="handleTableChange"
    >
      <!-- 访客信息 -->
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'visitor'">
          <div class="visitor-cell">
            <a-avatar :size="40">
              <template #icon><UserOutlined /></template>
            </a-avatar>
            <div class="visitor-info">
              <div class="visitor-name">
                <StarFilled v-if="record.star === 1" class="star-icon" />
                {{ record.nickname || record.userId }}
                <a-tag v-if="record.level === 3" color="gold" size="small">VIP</a-tag>
                <a-tag v-else-if="record.level === 2" color="blue" size="small">重要</a-tag>
              </div>
              <div class="visitor-id">{{ record.userId }}</div>
            </div>
          </div>
        </template>

        <!-- 联系方式 -->
        <template v-else-if="column.key === 'contact'">
          <div class="contact-info">
            <div v-if="record.phone">📱 {{ record.phone }}</div>
            <div v-if="record.email">📧 {{ record.email }}</div>
            <div v-if="!record.phone && !record.email" class="empty">-</div>
          </div>
        </template>

        <!-- 标签 -->
        <template v-else-if="column.key === 'tags'">
          <div class="tags-cell">
            <a-tag v-for="tag in parseTags(record.tags)" :key="tag" size="small">
              {{ tag }}
            </a-tag>
            <span v-if="!record.tags" class="empty">-</span>
          </div>
        </template>

        <!-- 统计 -->
        <template v-else-if="column.key === 'stats'">
          <div class="stats-cell">
            <div>访问: {{ record.visitCount || 0 }}次</div>
            <div>会话: {{ record.conversationCount || 0 }}次</div>
          </div>
        </template>

        <!-- 最后访问 -->
        <template v-else-if="column.key === 'lastVisitTime'">
          {{ record.lastVisitTime || '-' }}
        </template>

        <!-- 操作 -->
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleEdit(record)">
              编辑
            </a-button>
            <a-button type="link" size="small" @click="handleToggleStar(record)">
              {{ record.star === 1 ? '取消星标' : '星标' }}
            </a-button>
            <a-popconfirm
              title="确定删除此访客吗?"
              @confirm="handleDelete(record)"
            >
              <a-button type="link" size="small" danger>
                删除
              </a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 编辑弹窗 -->
    <CsVisitorModal
      v-model:open="modalVisible"
      :appId="currentAppId"
      :userId="currentUserId"
      @saved="handleSearch"
    />
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { UserOutlined, StarFilled, SearchOutlined } from '@ant-design/icons-vue';
import { defHttp } from '/@/utils/http/axios';
import CsVisitorModal from './CsVisitorModal.vue';

// 搜索条件
const searchKeyword = ref('');
const searchLevel = ref<number | undefined>();
const onlyStar = ref(false);

// 列表数据
const visitorList = ref<any[]>([]);
const loading = ref(false);

// 分页
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
});

// 弹窗
const modalVisible = ref(false);
const currentAppId = ref('');
const currentUserId = ref('');

// 表格列
const columns = [
  {
    title: '访客信息',
    key: 'visitor',
    width: 250
  },
  {
    title: '联系方式',
    key: 'contact',
    width: 180
  },
  {
    title: '公司/职位',
    dataIndex: 'company',
    width: 150,
    customRender: ({ record }: any) => {
      if (record.company && record.position) {
        return `${record.company} / ${record.position}`;
      }
      return record.company || record.position || '-';
    }
  },
  {
    title: '标签',
    key: 'tags',
    width: 200
  },
  {
    title: '统计',
    key: 'stats',
    width: 120
  },
  {
    title: '最后访问',
    key: 'lastVisitTime',
    width: 160
  },
  {
    title: '操作',
    key: 'action',
    width: 180,
    fixed: 'right'
  }
];

onMounted(() => {
  loadVisitors();
});

// 加载访客列表
async function loadVisitors() {
  loading.value = true;
  try {
    const res = await defHttp.get({
      url: '/airag/cs/visitor/list',
      params: {
        keyword: searchKeyword.value || undefined,
        level: searchLevel.value,
        star: onlyStar.value ? 1 : undefined,
        pageNo: pagination.current,
        pageSize: pagination.pageSize
      }
    });
    
    visitorList.value = res.records || [];
    pagination.total = res.total || 0;
  } catch {
    message.error('加载访客列表失败');
  } finally {
    loading.value = false;
  }
}

// 搜索
function handleSearch() {
  pagination.current = 1;
  loadVisitors();
}

// 表格变化
function handleTableChange(pag: any) {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  loadVisitors();
}

// 解析标签
function parseTags(tags: string): string[] {
  if (!tags) return [];
  try {
    return JSON.parse(tags);
  } catch {
    return [];
  }
}

// 编辑访客
function handleEdit(record: any) {
  currentAppId.value = record.appId;
  currentUserId.value = record.userId;
  modalVisible.value = true;
}

// 切换星标
async function handleToggleStar(record: any) {
  try {
    await defHttp.post({
      url: '/airag/cs/visitor/toggleStar',
      data: { id: record.id }
    });
    record.star = record.star === 1 ? 0 : 1;
    message.success('操作成功');
  } catch {
    message.error('操作失败');
  }
}

// 删除访客
async function handleDelete(record: any) {
  try {
    await defHttp.delete({
      url: '/airag/cs/visitor/delete',
      params: { id: record.id }
    });
    message.success('删除成功');
    loadVisitors();
  } catch {
    message.error('删除失败');
  }
}
</script>

<style lang="less" scoped>
.visitor-container {
  padding: 20px;
  background: #fff;
  border-radius: 8px;

  .search-area {
    margin-bottom: 20px;
  }

  .visitor-cell {
    display: flex;
    align-items: center;
    gap: 12px;

    .visitor-info {
      .visitor-name {
        display: flex;
        align-items: center;
        gap: 6px;
        font-weight: 500;

        .star-icon {
          color: #faad14;
          font-size: 14px;
        }
      }

      .visitor-id {
        color: #999;
        font-size: 12px;
        margin-top: 2px;
      }
    }
  }

  .contact-info,
  .stats-cell {
    font-size: 13px;
    line-height: 1.8;
  }

  .tags-cell {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
  }

  .empty {
    color: #ccc;
  }
}
</style>
