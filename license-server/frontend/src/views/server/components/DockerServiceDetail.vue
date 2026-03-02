<template>
  <a-modal
    v-model:open="modalVisible"
    title="Docker服务详细配置"
    width="900px"
    destroyOnClose
    :bodyStyle="{ padding: '20px', maxHeight: '75vh', overflow: 'auto' }"
    @ok="handleSave"
    @cancel="handleCancel"
    :confirmLoading="saving"
    okText="保存配置"
    cancelText="取消"
  >
    <a-form ref="formRef" :model="formData" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
      <a-tabs v-model:activeKey="activeTab" type="card">
        <a-tab-pane key="basic" tab="基础配置">
          <a-form-item label="服务名称" name="serviceName">
            <a-input v-model:value="formData.serviceName" placeholder="服务名称" />
          </a-form-item>
          <a-form-item label="容器名称" name="containerName">
            <a-input v-model:value="formData.containerName" placeholder="容器名称" />
          </a-form-item>
          <a-form-item label="镜像名称" name="imageName">
            <a-input v-model:value="formData.imageName" placeholder="镜像名称" />
          </a-form-item>
          <a-form-item label="当前版本" name="currentVersion">
            <a-input v-model:value="formData.currentVersion" placeholder="当前版本" />
          </a-form-item>
          <a-form-item label="目标版本" name="targetVersion">
            <a-input v-model:value="formData.targetVersion" placeholder="目标版本" />
          </a-form-item>
          <a-form-item label="重启策略" name="restartPolicy">
            <a-select v-model:value="formData.restartPolicy" placeholder="选择重启策略">
              <a-select-option value="no">no - 不自动重启</a-select-option>
              <a-select-option value="always">always - 总是重启</a-select-option>
              <a-select-option value="on-failure">on-failure - 失败时重启</a-select-option>
              <a-select-option value="unless-stopped">unless-stopped - 除非手动停止</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="启动命令" name="command">
            <a-textarea v-model:value="formData.command" placeholder="启动命令" :rows="2" />
          </a-form-item>
        </a-tab-pane>

        <a-tab-pane key="ports" tab="端口配置">
          <div class="config-section">
            <div class="section-header">
              <span class="section-title">端口映射配置</span>
              <a-button type="primary" size="small" @click="addPort"><PlusOutlined /> 添加端口</a-button>
            </div>
            <div class="port-config-header">
              <a-row :gutter="8">
                <a-col :span="7"><span class="port-label">外网端口（宿主机）</span></a-col>
                <a-col :span="1"></a-col>
                <a-col :span="7"><span class="port-label">容器端口（内部）</span></a-col>
                <a-col :span="5"><span class="port-label">协议</span></a-col>
                <a-col :span="4"></a-col>
              </a-row>
            </div>
            <div class="config-list">
              <div v-for="(port, index) in portList" :key="index" class="config-item">
                <a-row :gutter="8" align="middle">
                  <a-col :span="7"><a-input v-model:value="port.host" placeholder="如：3100" /></a-col>
                  <a-col :span="1" style="text-align: center;">→</a-col>
                  <a-col :span="7"><a-input v-model:value="port.container" placeholder="如：80" /></a-col>
                  <a-col :span="5">
                    <a-select v-model:value="port.protocol" placeholder="协议">
                      <a-select-option value="tcp">TCP</a-select-option>
                      <a-select-option value="udp">UDP</a-select-option>
                    </a-select>
                  </a-col>
                  <a-col :span="4" style="text-align: center;">
                    <a-button type="link" danger size="small" @click="removePort(index)"><DeleteOutlined /> 删除</a-button>
                  </a-col>
                </a-row>
              </div>
              <a-empty v-if="portList.length === 0" description="暂无端口配置" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
            </div>
          </div>
        </a-tab-pane>

        <a-tab-pane key="environment" tab="环境变量">
          <div class="config-section">
            <div class="section-header">
              <span class="section-title">PARAMS启动模式</span>
            </div>
            <a-form-item label="PARAMS模式">
              <a-switch v-model:checked="useParamsMode" checked-children="开启" un-checked-children="关闭" />
              <span style="margin-left: 12px; color: #999; font-size: 12px;">
                开启后，环境变量将以 PARAMS: > 的方式导出（适用于某些特殊镜像如xxl-job-admin）
              </span>
            </a-form-item>
          </div>
          <a-divider />
          <div class="config-section">
            <div class="section-header">
              <span class="section-title">环境变量配置</span>
              <a-button type="primary" size="small" @click="addEnv"><PlusOutlined /> 添加环境变量</a-button>
            </div>
            <div class="config-list">
              <div v-for="(env, index) in envList" :key="index" class="config-item">
                <a-row :gutter="8" align="middle">
                  <a-col :span="9"><a-input v-model:value="env.key" placeholder="变量名" /></a-col>
                  <a-col :span="1" style="text-align: center;">=</a-col>
                  <a-col :span="10"><a-input v-model:value="env.value" placeholder="变量值" /></a-col>
                  <a-col :span="4" style="text-align: center;">
                    <a-button type="link" danger size="small" @click="removeEnv(index)"><DeleteOutlined /> 删除</a-button>
                  </a-col>
                </a-row>
              </div>
              <a-empty v-if="envList.length === 0" description="暂无环境变量" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
            </div>
          </div>
        </a-tab-pane>

        <a-tab-pane key="volumes" tab="数据卷">
          <div class="config-section">
            <div class="section-header">
              <span class="section-title">数据卷映射配置</span>
              <a-button type="primary" size="small" @click="addVolume"><PlusOutlined /> 添加数据卷</a-button>
            </div>
            <div class="config-list">
              <div v-for="(volume, index) in volumeList" :key="index" class="config-item">
                <a-row :gutter="8" align="middle">
                  <a-col :span="9"><a-input v-model:value="volume.host" placeholder="主机路径" /></a-col>
                  <a-col :span="1" style="text-align: center;">→</a-col>
                  <a-col :span="9"><a-input v-model:value="volume.container" placeholder="容器路径" /></a-col>
                  <a-col :span="1" style="text-align: center;">:</a-col>
                  <a-col :span="4" style="text-align: center;">
                    <a-button type="link" danger size="small" @click="removeVolume(index)"><DeleteOutlined /> 删除</a-button>
                  </a-col>
                </a-row>
              </div>
              <a-empty v-if="volumeList.length === 0" description="暂无数据卷配置" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
            </div>
          </div>
        </a-tab-pane>

        <a-tab-pane key="advanced" tab="网络与依赖">
          <div class="config-section">
            <div class="section-header">
              <span class="section-title">网络配置</span>
              <a-button type="primary" size="small" @click="addNetwork"><PlusOutlined /> 添加网络</a-button>
            </div>
            <div class="config-list">
              <div v-for="(network, index) in networkList" :key="index" class="config-item">
                <a-row :gutter="8" align="middle">
                  <a-col :span="9"><a-input v-model:value="network.name" placeholder="网络名称，如：jeecg-boot" /></a-col>
                  <a-col :span="1" style="text-align: center;">→</a-col>
                  <a-col :span="10"><a-input v-model:value="network.ipv4Address" placeholder="内网IP（可选），如：172.19.0.104" /></a-col>
                  <a-col :span="4" style="text-align: center;">
                    <a-button type="link" danger size="small" @click="removeNetwork(index)"><DeleteOutlined /> 删除</a-button>
                  </a-col>
                </a-row>
              </div>
              <a-empty v-if="networkList.length === 0" description="暂无网络配置" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
            </div>
          </div>
          <a-divider />
          <div class="config-section">
            <div class="section-header">
              <span class="section-title">服务依赖</span>
              <a-button type="primary" size="small" @click="addDependency"><PlusOutlined /> 添加依赖</a-button>
            </div>
            <div class="config-list">
              <div v-for="(dep, index) in dependencyList" :key="index" class="config-item">
                <a-row :gutter="8" align="middle">
                  <a-col :span="9"><a-input v-model:value="dep.service" placeholder="依赖服务名" /></a-col>
                  <a-col :span="11">
                    <a-select v-model:value="dep.condition" placeholder="启动条件（可选）" allowClear>
                      <a-select-option value="service_started">service_started</a-select-option>
                      <a-select-option value="service_healthy">service_healthy</a-select-option>
                      <a-select-option value="service_completed_successfully">service_completed_successfully</a-select-option>
                    </a-select>
                  </a-col>
                  <a-col :span="4" style="text-align: center;">
                    <a-button type="link" danger size="small" @click="removeDependency(index)"><DeleteOutlined /> 删除</a-button>
                  </a-col>
                </a-row>
              </div>
              <a-empty v-if="dependencyList.length === 0" description="暂无服务依赖" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
            </div>
          </div>
        </a-tab-pane>

        <a-tab-pane key="extra" tab="扩展配置">
          <a-form-item label="主机名(hostname)" name="hostname">
            <a-input v-model:value="formData.hostname" placeholder="容器主机名，如: jeecg-boot-system" />
          </a-form-item>

          <a-divider orientation="left">Logging 日志配置</a-divider>
          <a-form-item label="日志驱动">
            <a-select v-model:value="extraConfig.loggingDriver" placeholder="选择日志驱动">
              <a-select-option value="">不配置</a-select-option>
              <a-select-option value="json-file">json-file</a-select-option>
              <a-select-option value="syslog">syslog</a-select-option>
              <a-select-option value="journald">journald</a-select-option>
              <a-select-option value="gelf">gelf</a-select-option>
              <a-select-option value="fluentd">fluentd</a-select-option>
              <a-select-option value="awslogs">awslogs</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="日志大小限制"><a-input v-model:value="extraConfig.loggingMaxSize" placeholder="如: 10M, 100K" /></a-form-item>
          <a-form-item label="日志文件数量"><a-input v-model:value="extraConfig.loggingMaxFile" placeholder="如: 3" /></a-form-item>

          <a-divider orientation="left">Labels 标签配置</a-divider>
          <div class="config-section">
            <div class="section-header">
              <span class="section-title">容器标签</span>
              <a-button type="primary" size="small" @click="addLabel"><PlusOutlined /> 添加标签</a-button>
            </div>
            <div class="config-list">
              <div v-for="(label, index) in labelList" :key="index" class="config-item">
                <a-row :gutter="8" align="middle">
                  <a-col :span="9"><a-input v-model:value="label.key" placeholder="标签名" /></a-col>
                  <a-col :span="1" style="text-align: center;">=</a-col>
                  <a-col :span="10"><a-input v-model:value="label.value" placeholder="标签值" /></a-col>
                  <a-col :span="4" style="text-align: center;">
                    <a-button type="link" danger size="small" @click="removeLabel(index)"><DeleteOutlined /> 删除</a-button>
                  </a-col>
                </a-row>
              </div>
              <a-empty v-if="labelList.length === 0" description="暂无标签配置" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
            </div>
          </div>

          <a-divider orientation="left">Healthcheck 健康检查</a-divider>
          <a-form-item label="检查命令"><a-input v-model:value="extraConfig.healthcheckTest" placeholder='如: ["CMD", "curl", "-f", "http://localhost/health"]' /></a-form-item>
          <a-form-item label="检查间隔"><a-input v-model:value="extraConfig.healthcheckInterval" placeholder="如: 30s" /></a-form-item>
          <a-form-item label="超时时间"><a-input v-model:value="extraConfig.healthcheckTimeout" placeholder="如: 10s" /></a-form-item>
          <a-form-item label="重试次数"><a-input v-model:value="extraConfig.healthcheckRetries" placeholder="如: 3" /></a-form-item>
          <a-form-item label="启动等待"><a-input v-model:value="extraConfig.healthcheckStartPeriod" placeholder="如: 30s（容器启动后等待多久开始检查）" /></a-form-item>
        </a-tab-pane>
      </a-tabs>
    </a-form>
  </a-modal>
</template>

<script lang="ts" setup>
import { ref, computed, watch, reactive } from 'vue'
import { Empty, message } from 'ant-design-vue'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { saveDockerService } from '../../../api/server'

interface PortMapping { host: string; container: string; protocol: string }
interface EnvVariable { key: string; value: string }
interface VolumeMapping { host: string; container: string; mode: string }
interface NetworkConfig { name: string; ipv4Address?: string }
interface DependencyConfig { service: string; condition?: string }

const props = defineProps({
  open: { type: Boolean, default: false },
  serviceData: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['update:open', 'success'])

const modalVisible = computed({
  get: () => props.open,
  set: (val) => emit('update:open', val),
})

const formRef = ref()
void formRef
const activeTab = ref('basic')
const saving = ref(false)

const formData = reactive({
  id: null as number | null,
  serviceName: '',
  containerName: '',
  hostname: '',
  imageName: '',
  currentVersion: '',
  targetVersion: '',
  restartPolicy: 'no',
  command: '',
})

const portList = ref<PortMapping[]>([])
const envList = ref<EnvVariable[]>([])
const volumeList = ref<VolumeMapping[]>([])
const networkList = ref<NetworkConfig[]>([])
const dependencyList = ref<DependencyConfig[]>([])
const labelList = ref<{ key: string; value: string }[]>([])
const useParamsMode = ref(false)

const extraConfig = reactive({
  loggingDriver: '',
  loggingMaxSize: '',
  loggingMaxFile: '',
  healthcheckTest: '',
  healthcheckInterval: '',
  healthcheckTimeout: '',
  healthcheckRetries: '',
  healthcheckStartPeriod: '',
})

watch(() => [props.open, props.serviceData], ([newVisible, newData]) => {
  if (newVisible && newData && typeof newData === 'object' && 'id' in newData && (newData as any).id) {
    initFormData(newData)
  }
}, { immediate: true })

const initFormData = (data: any) => {
  formData.id = data.id || null
  formData.serviceName = data.serviceName || ''
  formData.containerName = data.containerName || ''
  formData.hostname = data.hostname || ''
  formData.imageName = data.imageName || ''
  formData.currentVersion = data.currentVersion || ''
  formData.targetVersion = data.targetVersion || ''
  formData.restartPolicy = data.restartPolicy || 'no'
  formData.command = data.command || ''

  useParamsMode.value = data.useParamsMode === 1

  try {
    const ports = data.ports
    if (ports && Array.isArray(ports)) {
      portList.value = ports.map((port: any) => {
        if (typeof port === 'string') {
          const parts = port.split(':')
          const containerPart = parts[1] || ''
          const [container, protocol = 'tcp'] = containerPart.split('/')
          return { host: parts[0] || '', container: container || '', protocol: protocol || 'tcp' }
        }
        return port
      })
    } else {
      portList.value = []
    }
  } catch { portList.value = [] }

  try {
    const env = data.environment
    if (env && Array.isArray(env)) {
      envList.value = env.map((e: any) => {
        if (typeof e === 'string') {
          const equalIndex = e.indexOf('=')
          if (equalIndex > 0) return { key: e.substring(0, equalIndex), value: e.substring(equalIndex + 1) }
        }
        return e
      }).filter((e: any) => e && e.key)
    } else {
      envList.value = []
    }
  } catch { envList.value = [] }

  try {
    const volumes = data.volumes
    if (volumes && Array.isArray(volumes)) {
      volumeList.value = volumes.map((v: any) => {
        if (typeof v === 'string') {
          const parts = v.split(':')
          return { host: parts[0] || '', container: parts[1] || '', mode: parts[2] || 'rw' }
        }
        return v
      })
    } else {
      volumeList.value = []
    }
  } catch { volumeList.value = [] }

  try {
    const networks = data.networks
    if (networks && typeof networks === 'object' && !Array.isArray(networks)) {
      networkList.value = Object.keys(networks).map(name => {
        const config = networks[name]
        return { name, ipv4Address: config?.ipv4_address || '' }
      })
    } else if (networks && Array.isArray(networks)) {
      networkList.value = networks.map((item: any) => typeof item === 'string' ? { name: item, ipv4Address: '' } : { name: item.name || '', ipv4Address: item.ipv4Address || '' })
    } else {
      networkList.value = []
    }
  } catch { networkList.value = [] }

  try {
    const deps = data.dependsOn
    if (deps && Array.isArray(deps)) {
      dependencyList.value = deps.map((dep: any) =>
        typeof dep === 'string' ? { service: dep } : { service: dep.service, condition: dep.condition || undefined }
      )
    } else {
      dependencyList.value = []
    }
  } catch { dependencyList.value = [] }

  try {
    const ext = data.extraConfig
    if (ext && typeof ext === 'object') {
      if (ext.logging) {
        extraConfig.loggingDriver = ext.logging.driver || ''
        extraConfig.loggingMaxSize = ext.logging.options?.['max-size'] || ''
        extraConfig.loggingMaxFile = ext.logging.options?.['max-file'] || ''
      }
      if (ext.labels) {
        if (Array.isArray(ext.labels)) {
          labelList.value = ext.labels.map((label: any) => {
            if (typeof label === 'string') {
              const eqIdx = label.indexOf('=')
              if (eqIdx > 0) return { key: label.substring(0, eqIdx), value: label.substring(eqIdx + 1) }
            }
            return label
          }).filter((l: any) => l && l.key)
        } else if (typeof ext.labels === 'object') {
          labelList.value = Object.keys(ext.labels).map(key => ({ key, value: ext.labels[key] }))
        }
      } else {
        labelList.value = []
      }
      if (ext.healthcheck) {
        extraConfig.healthcheckTest = ext.healthcheck.test ? JSON.stringify(ext.healthcheck.test) : ''
        extraConfig.healthcheckInterval = ext.healthcheck.interval || ''
        extraConfig.healthcheckTimeout = ext.healthcheck.timeout || ''
        extraConfig.healthcheckRetries = ext.healthcheck.retries ? String(ext.healthcheck.retries) : ''
        extraConfig.healthcheckStartPeriod = ext.healthcheck.start_period || ''
      }
    } else {
      extraConfig.loggingDriver = ''
      extraConfig.loggingMaxSize = ''
      extraConfig.loggingMaxFile = ''
      extraConfig.healthcheckTest = ''
      extraConfig.healthcheckInterval = ''
      extraConfig.healthcheckTimeout = ''
      extraConfig.healthcheckRetries = ''
      extraConfig.healthcheckStartPeriod = ''
      labelList.value = []
    }
  } catch {
    extraConfig.loggingDriver = ''
    extraConfig.loggingMaxSize = ''
    extraConfig.loggingMaxFile = ''
    extraConfig.healthcheckTest = ''
    extraConfig.healthcheckInterval = ''
    extraConfig.healthcheckTimeout = ''
    extraConfig.healthcheckRetries = ''
    extraConfig.healthcheckStartPeriod = ''
    labelList.value = []
  }
}

const addPort = () => { portList.value.push({ host: '', container: '', protocol: 'tcp' }) }
const removePort = (index: number) => { portList.value.splice(index, 1) }
const addEnv = () => { envList.value.push({ key: '', value: '' }) }
const removeEnv = (index: number) => { envList.value.splice(index, 1) }
const addVolume = () => { volumeList.value.push({ host: '', container: '', mode: 'rw' }) }
const removeVolume = (index: number) => { volumeList.value.splice(index, 1) }
const addNetwork = () => { networkList.value.push({ name: '', ipv4Address: '' }) }
const removeNetwork = (index: number) => { networkList.value.splice(index, 1) }
const addDependency = () => { dependencyList.value.push({ service: '' }) }
const removeDependency = (index: number) => { dependencyList.value.splice(index, 1) }
const addLabel = () => { labelList.value.push({ key: '', value: '' }) }
const removeLabel = (index: number) => { labelList.value.splice(index, 1) }

const handleSave = async () => {
  try {
    saving.value = true

    const portsArray = portList.value
      .filter(p => p.host && p.container)
      .map(p => p.protocol && p.protocol !== 'tcp' ? `${p.host}:${p.container}/${p.protocol}` : `${p.host}:${p.container}`)

    const envArray = envList.value.filter(e => e.key && e.value).map(e => `${e.key}=${e.value}`)

    const volumesArray = volumeList.value
      .filter(v => v.host && v.container)
      .map(v => v.mode && v.mode !== 'rw' ? `${v.host}:${v.container}:${v.mode}` : `${v.host}:${v.container}`)

    const networksObj: any = {}
    networkList.value.filter(n => n.name).forEach(n => {
      networksObj[n.name] = n.ipv4Address?.trim() ? { ipv4_address: n.ipv4Address.trim() } : {}
    })

    const dependsArray = dependencyList.value.filter(d => d.service).map(d => {
      const item: Record<string, string> = { service: d.service }
      if (d.condition) item.condition = d.condition
      return item
    })

    const extraConfigObj: any = {}
    if (extraConfig.loggingDriver) {
      extraConfigObj.logging = { driver: extraConfig.loggingDriver, options: {} as Record<string, string> }
      if (extraConfig.loggingMaxSize) extraConfigObj.logging.options['max-size'] = extraConfig.loggingMaxSize
      if (extraConfig.loggingMaxFile) extraConfigObj.logging.options['max-file'] = extraConfig.loggingMaxFile
    }
    if (labelList.value.length > 0) {
      const labelsObj: Record<string, string> = {}
      labelList.value.filter(l => l.key).forEach(l => { labelsObj[l.key] = l.value || '' })
      if (Object.keys(labelsObj).length > 0) extraConfigObj.labels = labelsObj
    }
    if (extraConfig.healthcheckTest) {
      extraConfigObj.healthcheck = {} as any
      try { extraConfigObj.healthcheck.test = JSON.parse(extraConfig.healthcheckTest) } catch { extraConfigObj.healthcheck.test = [extraConfig.healthcheckTest] }
      if (extraConfig.healthcheckInterval) extraConfigObj.healthcheck.interval = extraConfig.healthcheckInterval
      if (extraConfig.healthcheckTimeout) extraConfigObj.healthcheck.timeout = extraConfig.healthcheckTimeout
      if (extraConfig.healthcheckRetries) extraConfigObj.healthcheck.retries = parseInt(extraConfig.healthcheckRetries)
      if (extraConfig.healthcheckStartPeriod) extraConfigObj.healthcheck.start_period = extraConfig.healthcheckStartPeriod
    }

    const saveData = {
      ...formData,
      ports: portsArray,
      environment: envArray,
      volumes: volumesArray,
      networks: Object.keys(networksObj).length > 0 ? networksObj : null,
      dependsOn: dependsArray.length > 0 ? dependsArray : null,
      extraConfig: Object.keys(extraConfigObj).length > 0 ? extraConfigObj : null,
      useParamsMode: useParamsMode.value ? 1 : 0,
    }

    const res = await saveDockerService(saveData, true)
    if (res.data.code === 200) {
      message.success('保存成功')
      emit('success')
      emit('update:open', false)
    } else {
      message.error(res.data.message || '保存失败')
    }
  } catch (error: any) {
    message.error('保存失败: ' + (error.message || error))
  } finally {
    saving.value = false
  }
}

const handleCancel = () => {
  emit('update:open', false)
}
</script>

<style scoped lang="less">
.config-section {
  margin-bottom: 24px;

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding-bottom: 8px;
    border-bottom: 1px solid #f0f0f0;

    .section-title {
      font-weight: 500;
      font-size: 14px;
      color: #262626;
    }
  }

  .config-list {
    .config-item {
      margin-bottom: 12px;
      padding: 12px;
      background: #fafafa;
      border-radius: 4px;
      transition: all 0.3s;

      &:hover {
        background: #f5f5f5;
        box-shadow: 0 2px 4px rgba(0,0,0,0.05);
      }
    }
  }
}

.port-config-header {
  margin-bottom: 12px;
  padding: 8px 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 4px;

  .port-label {
    font-weight: 600;
    font-size: 13px;
    color: #ffffff;
    display: inline-block;
  }
}

:deep(.ant-tabs-content-holder) {
  max-height: 55vh;
  overflow-y: auto;
}

:deep(.ant-empty) {
  margin: 20px 0;
}
</style>
