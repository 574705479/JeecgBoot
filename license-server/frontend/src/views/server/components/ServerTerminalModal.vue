<template>
  <div>
    <div class="terminal-wrapper" v-if="innerOpen && !minimized">
      <div class="terminal-window" :class="{ dragging }" :style="windowStyle">
        <div class="terminal-header" @mousedown.prevent="startDrag">
          <span class="terminal-title">{{ `服务器终端 - ${serverName || serverIp || serverId}` }}</span>
          <div class="terminal-controls">
            <a-button type="link" size="small" class="control-btn" @click.stop="toggleMinimize">最小化</a-button>
            <a-button type="link" size="small" class="control-btn" @click.stop="toggleFullscreen">{{ fullscreen ? '还原' : '全屏' }}</a-button>
            <a-button type="link" size="small" class="control-btn" @click.stop="closeTerminal">关闭</a-button>
          </div>
        </div>

        <div class="terminal-status">
          <a-space>
            <a-tag :color="connected ? 'green' : 'red'">{{ connected ? '已连接' : '未连接' }}</a-tag>
            <a-button
              size="small"
              :type="connected ? 'primary' : 'default'"
              @click="connect"
              :disabled="connecting"
              :loading="connecting"
            >
              {{ connected ? '已连接' : '连接' }}
            </a-button>
            <a-button size="small" @click="reconnect" :disabled="connecting">重连</a-button>
            <a-button size="small" @click="disconnect" :disabled="!connected">断开</a-button>
            <a-button size="small" @click="clearTerminal">清空</a-button>
          </a-space>
        </div>

        <div ref="terminalContainerRef" class="terminal-container">
          <div ref="terminalRef" class="terminal-panel" />
        </div>
      </div>
    </div>

    <div v-if="innerOpen && minimized" class="terminal-minimized" @click="restoreWindow">
      <span class="mini-title">{{ serverName || serverIp || `服务器${serverId}` }}</span>
      <a-button type="link" size="small" @click.stop="restoreWindow">恢复</a-button>
      <a-button type="link" size="small" danger @click.stop="closeTerminal">关闭</a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { Terminal } from 'xterm'
import { FitAddon } from 'xterm-addon-fit'
import { WebLinksAddon } from 'xterm-addon-web-links'
import 'xterm/css/xterm.css'

const props = defineProps<{
  modelValue: boolean
  serverId: number | null
  serverName?: string
  serverIp?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const innerOpen = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v),
})

const connected = ref(false)
const connecting = ref(false)
const minimized = ref(false)
const fullscreen = ref(false)
const dragging = ref(false)
const terminalRef = ref<HTMLElement | null>(null)
const terminalContainerRef = ref<HTMLElement | null>(null)

const win = reactive({
  width: 1100,
  height: 700,
  x: 120,
  y: 70,
})
const dragOffset = reactive({ x: 0, y: 0 })

let ws: WebSocket | null = null
let terminal: Terminal | null = null
let fitAddon: FitAddon | null = null
let heartbeatTimer: number | null = null
let resizeObserver: ResizeObserver | null = null

const windowStyle = computed(() => {
  if (fullscreen.value) {
    return {
      top: '0px',
      left: '0px',
      width: '100vw',
      height: '100vh',
    }
  }
  return {
    top: `${win.y}px`,
    left: `${win.x}px`,
    width: `${win.width}px`,
    height: `${win.height}px`,
  }
})

function centerWindow() {
  win.x = Math.max(20, Math.floor((window.innerWidth - win.width) / 2))
  win.y = Math.max(20, Math.floor((window.innerHeight - win.height) / 2))
}

function wsUrl(serverId: number) {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const token = localStorage.getItem('accessToken') || ''
  const query = token ? `?accessToken=${encodeURIComponent(token)}` : ''
  return `${protocol}//${window.location.host}/ws/terminal/${serverId}${query}`
}

function resolveWsTokenProtocol() {
  return localStorage.getItem('accessToken') || 'anonymous'
}

function ensureTerminal() {
  if (!terminalRef.value) return
  if (terminal) {
    const mountedParent = terminal.element?.parentElement || null
    if (mountedParent !== terminalRef.value) {
      terminal.open(terminalRef.value)
      fitAddon?.fit()
    }
    return
  }
  terminal = new Terminal({
    cursorBlink: true,
    fontSize: 14,
    fontFamily: 'Consolas, Menlo, Monaco, monospace',
    convertEol: true,
    scrollback: 5000,
    theme: {
      background: '#0b1221',
      foreground: '#d7e3ff',
      cursor: '#6ea8fe',
    },
  })
  fitAddon = new FitAddon()
  terminal.loadAddon(fitAddon)
  terminal.loadAddon(new WebLinksAddon())
  terminal.open(terminalRef.value)
  fitAddon.fit()
  terminal.writeln('\x1b[32m终端已就绪，正在连接...\x1b[0m')
  terminal.onData((text) => {
    if (!ws || ws.readyState !== WebSocket.OPEN) return
    sendWs({ type: 'input', content: text })
  })
}

function attachResizeObserver() {
  if (resizeObserver) resizeObserver.disconnect()
  if (!terminalContainerRef.value) return
  resizeObserver = new ResizeObserver(() => {
    fitAddon?.fit()
    if (connected.value) {
      sendWs({
        type: 'resize',
        cols: terminal?.cols ?? 120,
        rows: terminal?.rows ?? 36,
      })
    }
  })
  resizeObserver.observe(terminalContainerRef.value)
}

function connect() {
  if (connected.value) {
    return
  }
  if (!props.serverId) {
    message.warning('无效的服务器ID')
    return
  }
  ensureTerminal()
  attachResizeObserver()
  connecting.value = true
  disconnect()
  ws = new WebSocket(wsUrl(props.serverId), [resolveWsTokenProtocol()])
  ws.onopen = () => {
    connecting.value = false
    connected.value = true
    terminal?.writeln('\x1b[32m[system] 终端连接成功\x1b[0m')
    fitAddon?.fit()
    sendWs({
      type: 'init',
      cols: terminal?.cols ?? 120,
      rows: terminal?.rows ?? 36,
      serverId: props.serverId,
    })
    startHeartbeat()
  }
  ws.onmessage = (event) => handleWsMessage(String(event.data ?? ''))
  ws.onerror = () => {
    connecting.value = false
    terminal?.writeln('\r\n\x1b[31m[error] WebSocket异常\x1b[0m')
  }
  ws.onclose = () => {
    stopHeartbeat()
    connecting.value = false
    connected.value = false
    terminal?.writeln('\r\n\x1b[33m[system] 连接已关闭\x1b[0m')
  }
}

function disconnect() {
  stopHeartbeat()
  if (ws) {
    ws.close()
    ws = null
  }
  connected.value = false
}

function reconnect() {
  connect()
}

function sendWs(payload: Record<string, any>) {
  if (!ws || ws.readyState !== WebSocket.OPEN) return
  ws.send(JSON.stringify(payload))
}

function handleWsMessage(raw: string) {
  try {
    const msg = JSON.parse(raw)
    const type = msg?.type
    const data = msg?.data || {}
    if (type === 'output') {
      terminal?.write(String(data.content ?? ''))
      return
    }
    if (type === 'error') {
      terminal?.writeln(`\r\n\x1b[31m[error] ${data.message || '终端异常'}\x1b[0m`)
      return
    }
    if (type === 'pong' || type === 'connected') {
      return
    }
    terminal?.write(raw)
  } catch {
    terminal?.write(raw)
  }
}

function clearTerminal() {
  terminal?.clear()
}

function toggleFullscreen() {
  fullscreen.value = !fullscreen.value
  nextTick(() => fitAddon?.fit())
}

function toggleMinimize() {
  minimized.value = true
}

function restoreWindow() {
  minimized.value = false
  nextTick(() => {
    ensureTerminal()
    attachResizeObserver()
    fitAddon?.fit()
  })
}

function startHeartbeat() {
  stopHeartbeat()
  heartbeatTimer = window.setInterval(() => {
    sendWs({ type: 'ping', ts: Date.now() })
  }, 15000)
}

function stopHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

function closeTerminal() {
  disconnect()
  minimized.value = false
  fullscreen.value = false
  innerOpen.value = false
}

function startDrag(e: MouseEvent) {
  if (fullscreen.value) return
  dragging.value = true
  dragOffset.x = e.clientX - win.x
  dragOffset.y = e.clientY - win.y
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', endDrag)
}

function onDragMove(e: MouseEvent) {
  if (!dragging.value) return
  const x = e.clientX - dragOffset.x
  const y = e.clientY - dragOffset.y
  const maxX = window.innerWidth - win.width
  const maxY = window.innerHeight - win.height
  win.x = Math.max(0, Math.min(x, maxX))
  win.y = Math.max(0, Math.min(y, maxY))
}

function endDrag() {
  dragging.value = false
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', endDrag)
}

watch(
  () => props.modelValue,
  (open) => {
    if (open && props.serverId) {
      minimized.value = false
      nextTick(() => {
        centerWindow()
        connect()
      })
    } else if (!open) {
      disconnect()
    }
  },
)

onBeforeUnmount(() => {
  disconnect()
  endDrag()
  resizeObserver?.disconnect()
  resizeObserver = null
  terminal?.dispose()
  terminal = null
  fitAddon = null
})
</script>

<style scoped>
.terminal-wrapper {
  position: fixed;
  inset: 0;
  z-index: 1100;
  pointer-events: none;
}

.terminal-window {
  position: fixed;
  background: #1f2329;
  border-radius: 8px;
  box-shadow: 0 10px 36px rgba(0, 0, 0, 0.35);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  pointer-events: auto;
  transition: box-shadow 0.2s ease;
}

.terminal-window.dragging {
  transition: none;
}

.terminal-header {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  background: #2b3138;
  color: #fff;
  cursor: move;
  user-select: none;
}

.terminal-title {
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 70%;
}

.terminal-controls {
  display: flex;
  align-items: center;
  gap: 2px;
}

.control-btn {
  color: #d1d5db !important;
}

.terminal-status {
  padding: 8px 10px;
  background: #0f172a;
  border-bottom: 1px solid #2d3748;
}

.terminal-container {
  flex: 1;
  padding: 8px;
  background: #0b1221;
}

.terminal-panel {
  width: 100%;
  height: 100%;
}

.terminal-minimized {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 1101;
  background: #1f2329;
  color: #fff;
  border-radius: 6px;
  padding: 6px 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.35);
}

.mini-title {
  max-width: 220px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
