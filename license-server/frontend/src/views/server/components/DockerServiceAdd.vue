<template>
  <a-modal
    v-model:open="modalVisible"
    title="添加Docker服务"
    width="900px"
    @ok="handleSubmit"
    @cancel="handleCancel"
    :confirmLoading="saving"
    :bodyStyle="{ paddingRight: '40px' }"
    destroyOnClose
  >
    <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
      <a-divider orientation="left">
        快速导入
        <a-tooltip title="粘贴 docker-compose.yml 格式的服务配置，自动解析填充表单">
          <QuestionCircleOutlined style="margin-left: 8px; color: #999;" />
        </a-tooltip>
      </a-divider>

      <a-form-item label="YAML配置">
        <a-textarea
          v-model:value="yamlContent"
          placeholder="粘贴 docker-compose.yml 格式的单个服务配置，例如：
jeecg-boot-redis:
  image: registry.cn-hangzhou.aliyuncs.com/jeecgdocker/redis:5.0
  ports:
    - 6379:6379
  restart: always
  hostname: jeecg-boot-redis
  container_name: jeecg-boot-redis
  networks:
    - jeecg-boot"
          :rows="8"
          style="font-family: 'Courier New', monospace; font-size: 12px;"
        />
        <a-space style="margin-top: 8px;">
          <a-button type="primary" @click="parseYaml" :loading="parsing" size="small">
            <template #icon><ImportOutlined /></template>
            解析并填充
          </a-button>
          <a-button @click="clearYaml" size="small">
            <template #icon><ClearOutlined /></template>
            清空
          </a-button>
        </a-space>
      </a-form-item>

      <a-divider style="margin: 12px 0;" />

      <a-divider orientation="left">基础配置</a-divider>

      <a-form-item label="服务名称" required>
        <a-input v-model:value="formData.serviceName" placeholder="例如：jeecg-boot-redis" />
      </a-form-item>
      <a-form-item label="容器名称">
        <a-input v-model:value="formData.containerName" placeholder="例如：jeecg-boot-redis（留空则使用服务名称）" />
      </a-form-item>
      <a-form-item label="镜像地址" required>
        <a-input v-model:value="formData.imageName" placeholder="例如：registry.cn-hangzhou.aliyuncs.com/jeecgdocker/redis" />
      </a-form-item>
      <a-form-item label="镜像版本" required>
        <a-input v-model:value="formData.currentVersion" placeholder="例如：5.0" />
      </a-form-item>
      <a-form-item label="主机名">
        <a-input v-model:value="formData.hostname" placeholder="例如：jeecg-boot-redis" />
      </a-form-item>

      <a-divider orientation="left">端口配置</a-divider>

      <a-form-item label="端口映射">
        <div class="port-header-row">
          <a-row :gutter="8">
            <a-col :span="8"><span class="port-label">外网端口</span></a-col>
            <a-col :span="8"><span class="port-label">容器端口</span></a-col>
            <a-col :span="5"><span class="port-label">协议</span></a-col>
            <a-col :span="3"></a-col>
          </a-row>
        </div>
        <div v-for="(port, index) in portList" :key="index" style="margin-bottom: 8px;">
          <a-row :gutter="8">
            <a-col :span="8"><a-input v-model:value="port.host" placeholder="如：3100" /></a-col>
            <a-col :span="8"><a-input v-model:value="port.container" placeholder="如：80" /></a-col>
            <a-col :span="5">
              <a-select v-model:value="port.protocol" placeholder="协议">
                <a-select-option value="tcp">TCP</a-select-option>
                <a-select-option value="udp">UDP</a-select-option>
              </a-select>
            </a-col>
            <a-col :span="3" style="text-align: center;">
              <a-button type="link" danger @click="removePort(index)"><DeleteOutlined /></a-button>
            </a-col>
          </a-row>
        </div>
        <a-button type="dashed" block @click="addPort"><PlusOutlined /> 添加端口</a-button>
      </a-form-item>

      <a-divider orientation="left">环境变量</a-divider>

      <a-form-item label="PARAMS模式">
        <a-switch v-model:checked="formData.useParamsMode" checked-children="开启" un-checked-children="关闭" />
        <span style="margin-left: 12px; color: #999; font-size: 12px;">
          <QuestionCircleOutlined /> 开启后，环境变量将以 PARAMS: > 的方式导出（适用于某些特殊镜像）
        </span>
      </a-form-item>

      <a-form-item label="环境变量">
        <div v-for="(env, index) in envList" :key="index" style="margin-bottom: 8px;">
          <a-row :gutter="8">
            <a-col :span="10"><a-input v-model:value="env.key" placeholder="变量名" /></a-col>
            <a-col :span="11"><a-input v-model:value="env.value" placeholder="变量值" /></a-col>
            <a-col :span="3" style="text-align: center;">
              <a-button type="link" danger @click="removeEnv(index)"><DeleteOutlined /></a-button>
            </a-col>
          </a-row>
        </div>
        <a-button type="dashed" block @click="addEnv"><PlusOutlined /> 添加环境变量</a-button>
      </a-form-item>

      <a-divider orientation="left">数据卷</a-divider>

      <a-form-item label="数据卷映射">
        <div v-for="(volume, index) in volumeList" :key="index" style="margin-bottom: 8px;">
          <a-row :gutter="8">
            <a-col :span="10"><a-input v-model:value="volume.host" placeholder="宿主机路径" /></a-col>
            <a-col :span="10"><a-input v-model:value="volume.container" placeholder="容器路径" /></a-col>
            <a-col :span="1" style="text-align: center; padding-top: 5px;">:</a-col>
            <a-col :span="3" style="text-align: center;">
              <a-button type="link" danger @click="removeVolume(index)"><DeleteOutlined /></a-button>
            </a-col>
          </a-row>
        </div>
        <a-button type="dashed" block @click="addVolume"><PlusOutlined /> 添加数据卷</a-button>
      </a-form-item>

      <a-divider orientation="left">网络与依赖</a-divider>

      <a-form-item label="网络">
        <div v-for="(network, index) in networkList" :key="index" style="margin-bottom: 8px;">
          <a-row :gutter="8">
            <a-col :span="10"><a-input v-model:value="network.name" placeholder="网络名称，如：jeecg-boot" /></a-col>
            <a-col :span="11"><a-input v-model:value="network.ipv4Address" placeholder="内网IP（可选），如：172.19.0.104" /></a-col>
            <a-col :span="3" style="text-align: center;">
              <a-button type="link" danger @click="removeNetwork(index)"><DeleteOutlined /></a-button>
            </a-col>
          </a-row>
        </div>
        <a-button type="dashed" block @click="addNetwork"><PlusOutlined /> 添加网络</a-button>
      </a-form-item>

      <a-form-item label="依赖服务">
        <div v-for="(dep, index) in dependsList" :key="index" style="margin-bottom: 8px;">
          <a-row :gutter="8">
            <a-col :span="21"><a-input v-model:value="dep.service" placeholder="依赖的服务名称" /></a-col>
            <a-col :span="3" style="text-align: center;">
              <a-button type="link" danger @click="removeDepends(index)"><DeleteOutlined /></a-button>
            </a-col>
          </a-row>
        </div>
        <a-button type="dashed" block @click="addDepends"><PlusOutlined /> 添加依赖</a-button>
      </a-form-item>

      <a-form-item label="重启策略">
        <a-select v-model:value="formData.restartPolicy" placeholder="选择重启策略">
          <a-select-option value="no">no - 不自动重启</a-select-option>
          <a-select-option value="always">always - 总是重启</a-select-option>
          <a-select-option value="on-failure">on-failure - 失败时重启</a-select-option>
          <a-select-option value="unless-stopped">unless-stopped - 除非手动停止</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="启动命令">
        <a-textarea v-model:value="formData.command" placeholder="例如：redis-server --appendonly yes" :rows="2" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script lang="ts" setup>
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, DeleteOutlined, QuestionCircleOutlined, ImportOutlined, ClearOutlined } from '@ant-design/icons-vue'
import { saveDockerService } from '../../../api/server'
import * as yaml from 'js-yaml'

interface PortMapping { host: string; container: string; protocol: string }
interface EnvVariable { key: string; value: string }
interface VolumeMapping { host: string; container: string }
interface NetworkConfig { name: string; ipv4Address?: string }
interface DependencyConfig { service: string }

const props = defineProps({
  open: { type: Boolean, default: false },
  serverId: { type: Number, required: true },
})

const emit = defineEmits(['update:open', 'success'])

const modalVisible = computed({
  get: () => props.open,
  set: (val) => emit('update:open', val),
})

const saving = ref(false)
const yamlContent = ref('')
const parsing = ref(false)

const formData = ref({
  serviceName: '',
  containerName: '',
  imageName: '',
  currentVersion: '',
  hostname: '',
  restartPolicy: 'always',
  command: '',
  useParamsMode: false,
})

const portList = ref<PortMapping[]>([{ host: '', container: '', protocol: 'tcp' }])
const envList = ref<EnvVariable[]>([])
const volumeList = ref<VolumeMapping[]>([])
const networkList = ref<NetworkConfig[]>([{ name: 'jeecg-boot', ipv4Address: '' }])
const dependsList = ref<DependencyConfig[]>([])

const addPort = () => { portList.value.push({ host: '', container: '', protocol: 'tcp' }) }
const removePort = (index: number) => { portList.value.splice(index, 1) }
const addEnv = () => { envList.value.push({ key: '', value: '' }) }
const removeEnv = (index: number) => { envList.value.splice(index, 1) }
const addVolume = () => { volumeList.value.push({ host: '', container: '' }) }
const removeVolume = (index: number) => { volumeList.value.splice(index, 1) }
const addNetwork = () => { networkList.value.push({ name: '', ipv4Address: '' }) }
const removeNetwork = (index: number) => { networkList.value.splice(index, 1) }
const addDepends = () => { dependsList.value.push({ service: '' }) }
const removeDepends = (index: number) => { dependsList.value.splice(index, 1) }

const resetForm = () => {
  formData.value = {
    serviceName: '', containerName: '', imageName: '', currentVersion: '',
    hostname: '', restartPolicy: 'always', command: '', useParamsMode: false,
  }
  portList.value = [{ host: '', container: '', protocol: 'tcp' }]
  envList.value = []
  volumeList.value = []
  networkList.value = [{ name: 'jeecg-boot', ipv4Address: '' }]
  dependsList.value = []
  yamlContent.value = ''
}

watch(() => props.open, (newVal) => {
  if (newVal) resetForm()
})

const parseYaml = () => {
  if (!yamlContent.value.trim()) {
    message.warning('请先粘贴 YAML 配置内容')
    return
  }

  parsing.value = true
  try {
    const parsed: any = yaml.load(yamlContent.value)
    const serviceNames = Object.keys(parsed)
    if (serviceNames.length === 0) {
      message.error('无法解析 YAML 内容，请检查格式')
      parsing.value = false
      return
    }
    if (serviceNames.length > 1) {
      message.warning('检测到多个服务，将只解析第一个服务')
    }

    const serviceName = serviceNames[0]
    const serviceConfig = parsed[serviceName]

    formData.value.serviceName = serviceName
    formData.value.containerName = serviceConfig.container_name || serviceName
    formData.value.hostname = serviceConfig.hostname || ''

    if (serviceConfig.image) {
      const imageParts = serviceConfig.image.split(':')
      formData.value.imageName = imageParts[0]
      formData.value.currentVersion = imageParts[1] || 'latest'
    }

    if (serviceConfig.ports && Array.isArray(serviceConfig.ports)) {
      portList.value = serviceConfig.ports.map((port: any) => {
        const portStr = String(port)
        const protocolMatch = portStr.match(/^(.+)\/(\w+)$/)
        if (protocolMatch) {
          const [, mapping, protocol] = protocolMatch
          const [host, container] = mapping.split(':')
          return { host, container, protocol }
        } else {
          const parts = portStr.split(':')
          return parts.length === 2
            ? { host: parts[0], container: parts[1], protocol: 'tcp' }
            : { host: parts[0], container: parts[0], protocol: 'tcp' }
        }
      })
    } else {
      portList.value = [{ host: '', container: '', protocol: 'tcp' }]
    }

    if (serviceConfig.environment) {
      let hasParams = false
      if (Array.isArray(serviceConfig.environment)) {
        envList.value = serviceConfig.environment.map((env: string) => {
          const [key, ...valueParts] = env.split('=')
          return { key, value: valueParts.join('=') }
        })
      } else if (typeof serviceConfig.environment === 'object') {
        const tempEnvList: EnvVariable[] = []
        Object.entries(serviceConfig.environment).forEach(([key, value]) => {
          if (key === 'PARAMS') {
            hasParams = true
            const lines = String(value).split('\n')
            lines.forEach((line: string) => {
              const trimmedLine = line.trim()
              if (trimmedLine && trimmedLine.startsWith('--')) {
                const paramLine = trimmedLine.substring(2)
                const equalIndex = paramLine.indexOf('=')
                if (equalIndex > 0) {
                  tempEnvList.push({ key: paramLine.substring(0, equalIndex), value: paramLine.substring(equalIndex + 1) })
                }
              }
            })
          } else {
            tempEnvList.push({ key, value: String(value) })
          }
        })
        envList.value = tempEnvList
      }
      if (hasParams) {
        formData.value.useParamsMode = true
        message.success('检测到PARAMS格式，已自动开启PARAMS模式并拆分为单个环境变量')
      }
    } else {
      envList.value = []
    }

    if (serviceConfig.volumes && Array.isArray(serviceConfig.volumes)) {
      volumeList.value = serviceConfig.volumes.map((volume: string) => {
        const parts = volume.split(':')
        return { host: parts[0] || '', container: parts[1] || '' }
      })
    } else {
      volumeList.value = []
    }

    if (serviceConfig.networks) {
      if (Array.isArray(serviceConfig.networks)) {
        networkList.value = serviceConfig.networks.map((net: string) => ({ name: net, ipv4Address: '' }))
      } else if (typeof serviceConfig.networks === 'object') {
        networkList.value = Object.keys(serviceConfig.networks).map(name => {
          const config = serviceConfig.networks[name]
          return { name, ipv4Address: config?.ipv4_address || '' }
        })
      }
    } else {
      networkList.value = [{ name: 'jeecg-boot', ipv4Address: '' }]
    }

    if (serviceConfig.depends_on) {
      if (Array.isArray(serviceConfig.depends_on)) {
        dependsList.value = serviceConfig.depends_on.map((dep: string) => ({ service: dep }))
      } else if (typeof serviceConfig.depends_on === 'object') {
        dependsList.value = Object.keys(serviceConfig.depends_on).map(service => ({ service }))
      }
    } else {
      dependsList.value = []
    }

    formData.value.restartPolicy = serviceConfig.restart || 'always'

    if (serviceConfig.command) {
      formData.value.command = Array.isArray(serviceConfig.command) ? serviceConfig.command.join(' ') : serviceConfig.command
    } else {
      formData.value.command = ''
    }

    message.success('YAML 解析成功！已自动填充表单')
  } catch (error: any) {
    message.error('YAML 解析失败: ' + (error.message || '格式错误'))
  } finally {
    parsing.value = false
  }
}

const clearYaml = () => { yamlContent.value = '' }

const handleSubmit = async () => {
  if (!formData.value.serviceName) { message.warning('请输入服务名称'); return }
  if (!formData.value.imageName) { message.warning('请输入镜像地址'); return }
  if (!formData.value.currentVersion) { message.warning('请输入镜像版本'); return }

  saving.value = true
  try {
    const portsArray = portList.value
      .filter(p => p.host && p.container)
      .map(p => p.protocol && p.protocol !== 'tcp' ? `${p.host}:${p.container}/${p.protocol}` : `${p.host}:${p.container}`)

    const envArray = envList.value.filter(e => e.key && e.value).map(e => `${e.key}=${e.value}`)
    const volumesArray = volumeList.value.filter(v => v.host && v.container).map(v => `${v.host}:${v.container}`)

    const networksObj: any = {}
    networkList.value.filter(n => n.name).forEach(n => {
      networksObj[n.name] = n.ipv4Address?.trim() ? { ipv4_address: n.ipv4Address.trim() } : {}
    })

    const dependsArray = dependsList.value.filter(d => d.service).map(d => d.service)

    const submitData = {
      serverId: props.serverId,
      serviceName: formData.value.serviceName,
      containerName: formData.value.containerName || formData.value.serviceName,
      imageName: formData.value.imageName,
      currentVersion: formData.value.currentVersion,
      targetVersion: formData.value.currentVersion,
      ports: portsArray.length > 0 ? portsArray : null,
      environment: envArray.length > 0 ? envArray : null,
      volumes: volumesArray.length > 0 ? volumesArray : null,
      networks: Object.keys(networksObj).length > 0 ? networksObj : null,
      dependsOn: dependsArray.length > 0 ? dependsArray : null,
      restartPolicy: formData.value.restartPolicy || 'always',
      command: formData.value.command || null,
      status: 0,
      useParamsMode: formData.value.useParamsMode ? 1 : 0,
      hostname: formData.value.hostname || null,
    }

    const res = await saveDockerService(submitData, false)
    if (res.data.code === 200) {
      message.success('添加服务成功')
      emit('success')
      emit('update:open', false)
    } else {
      message.error(res.data.message || '添加服务失败')
    }
  } catch (error: any) {
    message.error('添加服务失败: ' + (error.message || error))
  } finally {
    saving.value = false
  }
}

const handleCancel = () => {
  emit('update:open', false)
}
</script>

<style scoped lang="less">
:deep(.ant-divider-horizontal.ant-divider-with-text-left) {
  margin: 16px 0 12px 0;
  font-weight: 500;
}

.port-header-row {
  margin-bottom: 8px;
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
</style>
