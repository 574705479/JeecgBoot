import { request } from '../utils/request'

export interface PageQuery {
  page?: number
  size?: number
  keyword?: string
}

export function listServerInfo(params: PageQuery) {
  return request.get('/admin/serverinfo/htServerInfo/list', { params })
}

export function getServerTree(keyword?: string) {
  return request.get('/admin/serverinfo/htServerInfo/queryTreeList', { params: { keyword } })
}

export function saveServerInfo(payload: any, isEdit: boolean) {
  return request[isEdit ? 'post' : 'post'](`/admin/serverinfo/htServerInfo/${isEdit ? 'edit' : 'add'}`, payload)
}

export function deleteServerInfo(id: number) {
  return request.delete('/admin/serverinfo/htServerInfo/delete', { params: { id } })
}

export function batchDeleteServerInfo(ids: number[]) {
  return request.delete('/admin/serverinfo/htServerInfo/deleteBatch', { params: { ids: ids.join(',') } })
}

export function connectServer(id: number) {
  return request.post('/admin/serverinfo/htServerInfo/connectToServer', { id })
}

export function generateSshKey(id: number) {
  return request.post('/admin/serverinfo/htServerInfo/generateAndConfigureSshKeyById', { id })
}

export function executeSql(serverIds: number[], sqlContent: string) {
  return request.post('/admin/serverinfo/htServerInfo/executeSql', { serverIds, sqlContent })
}

export function executeShellCommand(serverIds: number[], shellCommand: string) {
  return request.post('/admin/serverinfo/htServerInfo/executeShellCommand', { serverIds, shellCommand })
}

export function uploadCompose(payload: { serverId: number; fileName?: string; fileContent: string; filePath?: string }) {
  return request.post('/admin/serverinfo/htServerInfo/uploadDockerCompose', payload)
}

export function syncDockerStatus(serverId: number) {
  return request.post('/admin/serverinfo/htServerInfo/syncDockerServices', { serverId })
}

export function listDockerService(params: { page?: number; size?: number; serverId?: number }) {
  return request.get('/admin/dockerservice/htDockerService/list', { params })
}

export function listDockerServiceByServerId(serverId: number) {
  return request.get('/admin/dockerservice/htDockerService/listByServerId', { params: { serverId } })
}

export function saveDockerService(payload: any, isEdit: boolean) {
  return request[isEdit ? 'post' : 'post'](`/admin/dockerservice/htDockerService/${isEdit ? 'edit' : 'add'}`, payload)
}

export function deleteDockerService(id: number) {
  return request.delete('/admin/dockerservice/htDockerService/delete', { params: { id } })
}

export function updateDockerVersion(id: number, targetVersion: string) {
  return request.post('/admin/dockerservice/htDockerService/updateVersion', { id, targetVersion })
}

export function executeDockerCommand(payload: { serviceId: number; commandType: string; composePath?: string; serverId?: number }) {
  return request.post('/admin/dockerservice/htDockerService/executeDockerCommand', payload)
}

export function executeDockerAsync(payload: { serviceIds: number[]; commandType: string; composePath?: string; serverId: number }) {
  return request.post('/admin/dockerservice/htDockerService/executeDockerAsync', payload)
}

export function queryDockerTask(taskId: number) {
  return request.get('/admin/dockerservice/htDockerService/queryTaskProgress', { params: { taskId } })
}

export function getRunningDockerTask(serverId: number) {
  return request.get('/admin/dockerservice/htDockerService/getRunningTask', { params: { serverId } })
}

export function getRecentDockerTasks(serverId: number, limit = 10) {
  return request.get('/admin/dockerservice/htDockerService/getRecentTasks', { params: { serverId, limit } })
}

export function exportComposeFile(serverId: number) {
  return request.get('/admin/dockerservice/htDockerService/exportComposeFile', {
    params: { serverId },
    responseType: 'blob',
  })
}

export function listServerLogs(params: PageQuery & { status?: number }) {
  return request.get('/admin/serverinfolog/htServerInfoLog/list', { params })
}

export function deleteServerLog(id: number) {
  return request.delete('/admin/serverinfolog/htServerInfoLog/delete', { params: { id } })
}
