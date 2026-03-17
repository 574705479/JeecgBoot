<template>
  <div class="chat-window-settings">
    <div class="settings-layout">
      <!-- 左侧配置表单 -->
      <div class="settings-form">
        <a-card title="聊天窗口设置" :bordered="false">
          <a-form layout="vertical">
            <!-- 外观 -->
            <a-divider orientation="left">外观</a-divider>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="主题色（头部背景）">
                  <div class="color-picker-row">
                    <input type="color" v-model="config.themeColor" class="color-input" />
                    <a-input v-model:value="config.themeColor" size="small" style="width:120px" />
                  </div>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="页面标题">
                  <a-input v-model:value="config.pageTitle" placeholder="在线客服" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="Logo">
                  <CropperUpload v-model:value="config.logo" :uploadApi="uploadImg" btnText="上传Logo" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="背景图片（聊天区域）">
                  <CropperUpload v-model:value="config.backgroundImage" :uploadApi="uploadImg" btnText="上传背景" />
                </a-form-item>
              </a-col>
              <a-col :span="6">
                <a-form-item label="隐藏页面标题">
                  <a-switch v-model:checked="config.hidePageTitle" />
                </a-form-item>
              </a-col>
              <a-col :span="6">
                <a-form-item label="隐藏在线状态">
                  <a-switch v-model:checked="config.hideOnlineStatus" />
                </a-form-item>
              </a-col>
              <a-col :span="6">
                <a-form-item label="隐藏AI/人工标签">
                  <a-switch v-model:checked="config.hideAiHumanLabel" />
                </a-form-item>
              </a-col>
              <a-col :span="6">
                <a-form-item label="隐藏Logo">
                  <a-switch v-model:checked="config.hideLogo" />
                </a-form-item>
              </a-col>
            </a-row>

            <!-- 头部 -->
            <a-divider orientation="left">头部</a-divider>
            <a-row :gutter="16">
              <a-col :span="24">
                <a-form-item label="显示头部">
                  <a-switch v-model:checked="config.headerVisible" />
                  <span class="switch-hint">关闭后访客端不显示头部</span>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="头部背景图">
                  <CropperUpload v-model:value="config.headerBgImage" :uploadApi="uploadImg" btnText="上传背景图" />
                  <div class="upload-hint">上传后头部将使用背景图替代纯色</div>
                </a-form-item>
                <a-form-item v-if="config.headerBgImage" label="背景图显示模式">
                  <a-select v-model:value="config.headerBgImageMode" style="width: 100%">
                    <a-select-option value="cover">铺满（裁剪）</a-select-option>
                    <a-select-option value="contain">完整显示（留白）</a-select-option>
                    <a-select-option value="stretch">拉伸填充</a-select-option>
                    <a-select-option value="repeat">平铺重复</a-select-option>
                    <a-select-option value="center">居中（原始尺寸）</a-select-option>
                  </a-select>
                </a-form-item>
                <a-form-item v-if="config.headerBgImage" label="背景图位置">
                  <a-select v-model:value="config.headerBgPosition" style="width: 100%">
                    <a-select-option value="center">居中</a-select-option>
                    <a-select-option value="left center">左对齐</a-select-option>
                    <a-select-option value="right center">右对齐</a-select-option>
                    <a-select-option value="center top">顶部居中</a-select-option>
                    <a-select-option value="center bottom">底部居中</a-select-option>
                    <a-select-option value="left top">左上角</a-select-option>
                    <a-select-option value="right top">右上角</a-select-option>
                    <a-select-option value="left bottom">左下角</a-select-option>
                    <a-select-option value="right bottom">右下角</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="手机端头部背景图">
                  <CropperUpload v-model:value="config.mobileHeaderBgImage" :uploadApi="uploadImg" btnText="上传手机端背景图" />
                  <div class="upload-hint">未设置时使用通用头部背景图</div>
                </a-form-item>
                <a-form-item v-if="config.mobileHeaderBgImage" label="手机端背景图显示模式">
                  <a-select v-model:value="config.mobileHeaderBgImageMode" style="width: 100%">
                    <a-select-option value="cover">铺满（裁剪）</a-select-option>
                    <a-select-option value="contain">完整显示（留白）</a-select-option>
                    <a-select-option value="stretch">拉伸填充</a-select-option>
                    <a-select-option value="repeat">平铺重复</a-select-option>
                    <a-select-option value="center">居中（原始尺寸）</a-select-option>
                  </a-select>
                </a-form-item>
                <a-form-item v-if="config.mobileHeaderBgImage" label="手机端背景图位置">
                  <a-select v-model:value="config.mobileHeaderBgPosition" style="width: 100%">
                    <a-select-option value="center">居中</a-select-option>
                    <a-select-option value="left center">左对齐</a-select-option>
                    <a-select-option value="right center">右对齐</a-select-option>
                    <a-select-option value="center top">顶部居中</a-select-option>
                    <a-select-option value="center bottom">底部居中</a-select-option>
                    <a-select-option value="left top">左上角</a-select-option>
                    <a-select-option value="right top">右上角</a-select-option>
                    <a-select-option value="left bottom">左下角</a-select-option>
                    <a-select-option value="right bottom">右下角</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="24">
                <a-form-item label="滚动文字（跑马灯）">
                  <a-input v-model:value="config.scrollText" placeholder="填入后头部下方展示滚动文字" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="滚动时长（秒）">
                  <a-input-number v-model:value="config.scrollDuration" :min="10" :max="120" placeholder="15" style="width:100%" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="滚动文字颜色">
                  <div class="color-picker-row">
                    <input type="color" v-model="config.scrollTextColor" class="color-input" />
                    <a-input v-model:value="config.scrollTextColor" size="small" style="width:100px" />
                  </div>
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="滚动文字背景">
                  <div class="color-picker-row">
                    <input type="color" v-model="config.scrollTextBgColor" class="color-input" />
                    <a-input v-model:value="config.scrollTextBgColor" size="small" style="width:100px" />
                  </div>
                </a-form-item>
              </a-col>
            </a-row>

            <!-- 头部图标（PC端显示在头部右侧） -->
            <a-divider orientation="left">头部图标（PC端）</a-divider>
            <div class="upload-hint" style="margin-bottom:12px">PC端头部右侧显示的图标链接，点击可跳转。最多配置5个。</div>
            <div class="header-icons-section">
              <div v-if="config.headerIcons.length === 0" class="header-icons-empty">
                <span style="color:#bbb">暂无图标，点击下方按钮添加</span>
              </div>
              <div v-for="(item, idx) in config.headerIcons" :key="idx" class="header-icon-row">
                <div class="header-icon-row-num">{{ idx + 1 }}</div>
                <div class="header-icon-row-fields">
                  <div class="header-icon-field">
                    <span class="header-icon-label">图标</span>
                    <CropperUpload v-model:value="item.icon" :uploadApi="uploadImg" :aspectRatio="1" btnText="上传" inputWidth="140px" />
                  </div>
                  <div class="header-icon-field">
                    <span class="header-icon-label">昵称</span>
                    <a-input v-model:value="item.name" placeholder="图标名称" size="small" style="width:120px" />
                  </div>
                  <div class="header-icon-field">
                    <span class="header-icon-label">链接</span>
                    <a-input v-model:value="item.link" placeholder="点击跳转URL" size="small" style="width:200px" />
                  </div>
                  <div class="header-icon-field">
                    <span class="header-icon-label">尺寸</span>
                    <a-input-number v-model:value="item.size" :min="16" :max="80" placeholder="32" size="small" style="width:80px" />
                    <span style="color:#999;font-size:12px;margin-left:2px">px</span>
                  </div>
                  <div class="header-icon-field">
                    <span class="header-icon-label">透明</span>
                    <a-switch v-model:checked="item.transparent" size="small" />
                    <span style="color:#999;font-size:11px;margin-left:4px">去掉边框背景</span>
                  </div>
                </div>
                <a-button type="text" size="small" danger @click="removeHeaderIcon(idx)"><DeleteOutlined /></a-button>
              </div>
              <a-button type="dashed" size="small" :disabled="config.headerIcons.length >= 5" @click="addHeaderIcon" style="margin-top:8px">
                <PlusOutlined /> 添加图标
              </a-button>
              <span v-if="config.headerIcons.length >= 5" class="faq-limit-hint" style="margin-left:8px">已达上限</span>
            </div>

            <!-- 客服气泡 -->
            <a-divider orientation="left">客服气泡</a-divider>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="背景色">
                  <div class="color-picker-row">
                    <input type="color" v-model="config.agentBubbleBgColor" class="color-input" />
                    <a-input v-model:value="config.agentBubbleBgColor" size="small" style="width:120px" />
                  </div>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="字体颜色">
                  <div class="color-picker-row">
                    <input type="color" v-model="config.agentBubbleFontColor" class="color-input" />
                    <a-input v-model:value="config.agentBubbleFontColor" size="small" style="width:120px" />
                  </div>
                </a-form-item>
              </a-col>
            </a-row>

            <!-- 访客气泡 -->
            <a-divider orientation="left">访客气泡</a-divider>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="背景色">
                  <div class="color-picker-row">
                    <input type="color" v-model="config.visitorBubbleBgColor" class="color-input" />
                    <a-input v-model:value="config.visitorBubbleBgColor" size="small" style="width:120px" />
                  </div>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="字体颜色">
                  <div class="color-picker-row">
                    <input type="color" v-model="config.visitorBubbleFontColor" class="color-input" />
                    <a-input v-model:value="config.visitorBubbleFontColor" size="small" style="width:120px" />
                  </div>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="访客头像">
                  <CropperUpload v-model:value="config.visitorAvatar" :uploadApi="uploadImg" :aspectRatio="1" btnText="上传头像" inputWidth="160px" />
                  <div class="upload-hint">建议尺寸 80×80 px</div>
                </a-form-item>
              </a-col>
            </a-row>

            <!-- 功能开关 -->
            <a-divider orientation="left">功能开关</a-divider>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="访客历史记录">
                  <a-switch v-model:checked="config.visitorHistory" />
                  <span class="switch-hint">开启后访客可查看历史消息</span>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="访客填写指定内容接通">
                  <a-switch v-model:checked="config.humanAgentEnabled" />
                  <span class="switch-hint">开启后访客需点击人工客服按钮并填写信息才接入人工</span>
                </a-form-item>
              </a-col>
              <a-col :span="24" v-if="config.humanAgentEnabled">
                <a-form-item>
                  <template #label>
                    <span>自定义字段 <span style="color:#999;font-weight:normal;font-size:12px">（配置访客转人工时需填写的字段）</span></span>
                  </template>
                  <div class="human-agent-fields-section">
                    <a-button type="dashed" size="small" @click="addHumanAgentField" style="margin-bottom:8px">
                      <PlusOutlined /> 添加字段
                    </a-button>
                    <div v-if="!config.humanAgentFields.length" style="color:#bbb;font-size:12px;padding:8px 0">暂未配置字段</div>
                    <div v-for="(field, fIdx) in config.humanAgentFields" :key="fIdx" class="human-agent-field-row">
                      <a-input v-model:value="field.label" placeholder="字段名称" style="width:140px" size="small" />
                      <a-select v-model:value="field.type" style="width:110px" size="small">
                        <a-select-option value="text">文本</a-select-option>
                        <a-select-option value="phone">手机号</a-select-option>
                        <a-select-option value="email">邮箱</a-select-option>
                      </a-select>
                      <a-checkbox v-model:checked="field.required">必填</a-checkbox>
                      <a-button type="text" size="small" danger @click="config.humanAgentFields.splice(fIdx, 1)"><DeleteOutlined /></a-button>
                    </div>
                  </div>
                </a-form-item>
              </a-col>
              <a-col :span="6">
                <a-form-item label="发送表情">
                  <a-switch v-model:checked="config.sendEmoji" />
                </a-form-item>
              </a-col>
              <a-col :span="6">
                <a-form-item label="发送图片">
                  <a-switch v-model:checked="config.sendImage" />
                </a-form-item>
              </a-col>
              <a-col :span="6">
                <a-form-item label="发送视频">
                  <a-switch v-model:checked="config.sendVideo" />
                </a-form-item>
              </a-col>
              <a-col :span="6">
                <a-form-item label="发送PDF">
                  <a-switch v-model:checked="config.sendPdf" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="文件大小限制">
                  <div style="display:flex;align-items:center;gap:8px">
                    <a-input-number v-model:value="config.maxFileSize" :min="1" :max="50" :step="1" style="width:120px" />
                    <span style="color:#999">MB（最大50MB）</span>
                  </div>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="访客端时区">
                  <a-select v-model:value="config.visitorTimezone" style="width:100%">
                    <a-select-option value="Asia/Shanghai">北京时间</a-select-option>
                    <a-select-option value="auto">自动跟随访客时区</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>

            <!-- PC广告位 -->
            <a-divider orientation="left">PC右侧广告位</a-divider>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="广告链接">
                  <a-input v-model:value="config.pcAdLink" placeholder="点击广告图片跳转的链接" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="广告图片">
                  <CropperUpload v-model:value="config.pcAdImage" :uploadApi="uploadImg" btnText="上传广告图" inputWidth="160px" />
                  <div class="upload-hint">建议尺寸 200×540 px</div>
                </a-form-item>
              </a-col>
            </a-row>

            <!-- 常见问题 -->
            <a-divider orientation="left">常见问题</a-divider>
            <a-row :gutter="16">
              <a-col :span="24">
                <a-form-item label="启用常见问题">
                  <a-switch v-model:checked="config.faqEnabled" />
                  <span class="switch-hint">开启后将替代 AI 预设问题，访客端优先展示此处配置的常见问题，点击后直接返回预设答案（不触发 AI 回复）</span>
                </a-form-item>
              </a-col>
              <a-col :span="24" v-if="config.faqEnabled">
                <a-form-item label="FAQ引导语">
                  <a-input v-model:value="config.faqHeaderText" placeholder="您好，请问有什么可以帮助您的？" allow-clear />
                  <span class="switch-hint">访客端首次进入时，智能助手发送的FAQ引导语文本，留空默认"您好，请问有什么可以帮助您的？"</span>
                </a-form-item>
              </a-col>
              <a-col :span="12" v-if="config.faqEnabled">
                <a-form-item label="问题链接颜色">
                  <div class="color-picker-row">
                    <input type="color" v-model="config.faqLinkColor" class="color-input" />
                    <a-input v-model:value="config.faqLinkColor" size="small" style="width:120px" />
                  </div>
                  <span class="switch-hint">访客端FAQ问题超链接颜色</span>
                </a-form-item>
              </a-col>
              <a-col :span="12" v-if="config.faqEnabled">
                <a-form-item label="功能栏颜色">
                  <div class="color-picker-row">
                    <input type="color" v-model="config.faqNavColor" class="color-input" />
                    <a-input v-model:value="config.faqNavColor" size="small" style="width:120px" />
                  </div>
                  <span class="switch-hint">"返回第一层"、"返回上一层"、"人工客服"链接颜色</span>
                </a-form-item>
              </a-col>
              <a-col :span="24" v-if="config.faqEnabled">
                <div class="faq-section">
                  <div class="faq-header">
                    <a-button type="primary" size="small" @click="openFaqEditor([], -1)">
                      <PlusOutlined /> 添加问题
                    </a-button>
                    <span class="faq-count">{{ config.faqList.length }} 条</span>
                  </div>
                  <div v-if="config.faqList.length === 0" class="faq-empty">
                    <QuestionCircleOutlined style="font-size:28px;color:#d9d9d9;margin-bottom:8px" />
                    <div>暂无常见问题</div>
                    <div style="font-size:12px;color:#bbb;margin-top:4px">点击上方"添加问题"按钮开始配置</div>
                  </div>
                  <template v-for="(faq, idx) in config.faqList" :key="'faq-' + idx">
                    <div class="faq-tree-node" :style="{ paddingLeft: '0px' }">
                      <div class="faq-tree-main" @click="openFaqEditor([], idx)">
                        <div class="faq-tree-body">
                          <div class="faq-tree-question">{{ faq.question }}</div>
                          <div class="faq-tree-answer">{{ stripHtml(faq.answer) }}</div>
                          <div v-if="faq.keywords?.length" class="faq-tree-keywords">
                            <a-tag v-for="kw in faq.keywords" :key="kw" size="small" color="blue">{{ kw }}</a-tag>
                          </div>
                        </div>
                        <div class="faq-tree-actions" @click.stop>
                          <a-button type="text" size="small" @click="openFaqEditor([], idx)"><EditOutlined /></a-button>
                          <a-button type="text" size="small" danger @click="removeFaq([], idx)"><DeleteOutlined /></a-button>
                          <a-button type="link" size="small" @click="openFaqEditor([idx], -1)"><PlusOutlined /> 添加下级</a-button>
                        </div>
                      </div>
                    </div>
                    <template v-if="faq.children?.length">
                      <template v-for="(c1, i1) in faq.children" :key="'faq-' + idx + '-' + i1">
                        <div class="faq-tree-node" :style="{ paddingLeft: '24px' }">
                          <div class="faq-tree-main" @click="openFaqEditor([idx], i1)">
                            <span class="faq-tree-prefix">--</span>
                            <div class="faq-tree-body">
                              <div class="faq-tree-question">{{ c1.question }}</div>
                              <div class="faq-tree-answer">{{ stripHtml(c1.answer) }}</div>
                            </div>
                            <div class="faq-tree-actions" @click.stop>
                              <a-button type="text" size="small" @click="openFaqEditor([idx], i1)"><EditOutlined /></a-button>
                              <a-button type="text" size="small" danger @click="removeFaq([idx], i1)"><DeleteOutlined /></a-button>
                              <a-button type="link" size="small" @click="openFaqEditor([idx, i1], -1)"><PlusOutlined /> 添加下级</a-button>
                            </div>
                          </div>
                        </div>
                        <template v-if="c1.children?.length">
                          <template v-for="(c2, i2) in c1.children" :key="'faq-' + idx + '-' + i1 + '-' + i2">
                            <div class="faq-tree-node" :style="{ paddingLeft: '48px' }">
                              <div class="faq-tree-main" @click="openFaqEditor([idx, i1], i2)">
                                <span class="faq-tree-prefix">--</span>
                                <div class="faq-tree-body">
                                  <div class="faq-tree-question">{{ c2.question }}</div>
                                  <div class="faq-tree-answer">{{ stripHtml(c2.answer) }}</div>
                                </div>
                                <div class="faq-tree-actions" @click.stop>
                                  <a-button type="text" size="small" @click="openFaqEditor([idx, i1], i2)"><EditOutlined /></a-button>
                                  <a-button type="text" size="small" danger @click="removeFaq([idx, i1], i2)"><DeleteOutlined /></a-button>
                                  <a-button type="link" size="small" @click="openFaqEditor([idx, i1, i2], -1)"><PlusOutlined /> 添加下级</a-button>
                                </div>
                              </div>
                            </div>
                            <template v-if="c2.children?.length">
                              <template v-for="(c3, i3) in c2.children" :key="'faq-' + idx + '-' + i1 + '-' + i2 + '-' + i3">
                                <div class="faq-tree-node" :style="{ paddingLeft: '72px' }">
                                  <div class="faq-tree-main" @click="openFaqEditor([idx, i1, i2], i3)">
                                    <span class="faq-tree-prefix">--</span>
                                    <div class="faq-tree-body">
                                      <div class="faq-tree-question">{{ c3.question }}</div>
                                      <div class="faq-tree-answer">{{ stripHtml(c3.answer) }}</div>
                                    </div>
                                    <div class="faq-tree-actions" @click.stop>
                                      <a-button type="text" size="small" @click="openFaqEditor([idx, i1, i2], i3)"><EditOutlined /></a-button>
                                      <a-button type="text" size="small" danger @click="removeFaq([idx, i1, i2], i3)"><DeleteOutlined /></a-button>
                                      <a-button type="link" size="small" @click="openFaqEditor([idx, i1, i2, i3], -1)"><PlusOutlined /> 添加下级</a-button>
                                    </div>
                                  </div>
                                </div>
                              </template>
                            </template>
                          </template>
                        </template>
                      </template>
                    </template>
                  </template>
                </div>
              </a-col>
            </a-row>

            <a-button type="primary" @click="handleSave" :loading="saving" style="margin-top:16px">保存设置</a-button>
          </a-form>
        </a-card>
      </div>

      <!-- 右侧预览区 -->
      <div class="settings-preview">
        <a-card :bordered="false" :bodyStyle="{ padding: '12px' }">
          <a-tabs v-model:activeKey="previewTab" centered>
            <a-tab-pane key="pc" tab="电脑端预览" />
            <a-tab-pane key="mobile" tab="手机端预览" />
          </a-tabs>
          <div class="preview-container" :class="previewTab">
            <div class="preview-wrapper" :style="previewWrapperStyle">
              <!-- 全宽头部（独立于 preview-chat，横跨整个 preview-wrapper） -->
              <div v-if="config.headerVisible" class="p-header-full" :style="previewHeaderStyle">
                <LeftOutlined v-if="previewTab === 'mobile'" class="p-back-btn" />
                <div class="p-header-content">
                  <img v-if="config.logo && !config.hideLogo" :src="resolveUrl(config.logo)" class="p-logo" alt="logo" />
                  <div class="p-header-info">
                    <span v-if="!config.hidePageTitle" class="p-title">{{ config.pageTitle || '在线客服' }}</span>
                    <span class="p-status-line">
                      <template v-if="!config.hideOnlineStatus">
                        <span class="p-status-dot"></span>
                        <span class="p-status-text">在线</span>
                      </template>
                      <a-tag v-if="!config.hideAiHumanLabel" color="blue" size="small" style="margin-left: 4px;">AI客服</a-tag>
                    </span>
                  </div>
                </div>
                <div v-if="previewTab === 'pc' && config.headerIcons?.length" class="p-header-icons">
                  <a v-for="(item, idx) in config.headerIcons" :key="idx" class="p-header-icon-item" :href="item.link || '#'" target="_blank" rel="noopener">
                    <img v-if="item.icon" :src="resolveUrl(item.icon)"
                         :class="['p-header-icon-img', { 'p-header-icon-transparent': item.transparent }]"
                         :style="{ width: (item.size || 32) + 'px', height: (item.size || 32) + 'px' }" />
                    <span v-else class="p-header-icon-placeholder">📎</span>
                    <span class="p-header-icon-name"
                          :style="{ fontSize: Math.max(9, Math.round((item.size || 32) * 0.3)) + 'px', maxWidth: Math.max(36, (item.size || 32) * 1.6) + 'px' }">{{ item.name || '图标' }}</span>
                  </a>
                </div>
              </div>
              <!-- 内容区域（聊天 + 侧边栏水平排列） -->
              <div class="preview-body">
                <!-- 聊天窗口 -->
                <div class="preview-chat" :style="previewChatStyle">
                  <!-- 滚动文字 -->
                  <div v-if="config.scrollText" class="p-scroll-bar"
                       :style="{ background: config.scrollTextBgColor || '#1890ff', color: config.scrollTextColor || '#fff' }">
                    <div class="p-scroll-text" :style="{ animationDuration: (config.scrollDuration || 15) + 's' }">
                      {{ config.scrollText }}
                    </div>
                  </div>
                  <!-- 消息区域 -->
                  <div class="p-messages" :style="msgAreaStyle">
                    <!-- 客服消息 -->
                    <div class="p-msg p-msg-agent">
                      <div class="p-avatar">🤖</div>
                      <div class="p-bubble" :style="{ background: config.agentBubbleBgColor || '#f5f5f5', color: config.agentBubbleFontColor || '#333' }">
                        你好，有什么可以帮助您的？
                      </div>
                    </div>
                    <!-- 访客消息 -->
                    <div class="p-msg p-msg-visitor">
                      <div class="p-bubble" :style="{ background: config.visitorBubbleBgColor || '#667eea', color: config.visitorBubbleFontColor || '#fff' }">
                        我想咨询一下产品信息
                      </div>
                      <div class="p-avatar">
                        <img v-if="config.visitorAvatar" :src="resolveUrl(config.visitorAvatar)" class="p-avatar-img" />
                        <span v-else>👤</span>
                      </div>
                    </div>
                    <!-- 客服回复 -->
                    <div class="p-msg p-msg-agent">
                      <div class="p-avatar">🤖</div>
                      <div class="p-bubble" :style="{ background: config.agentBubbleBgColor || '#f5f5f5', color: config.agentBubbleFontColor || '#333' }">
                        好的，请问您想了解哪款产品？
                      </div>
                    </div>
                  </div>
                  <!-- 输入区 -->
                  <div class="p-input-area">
                    <div class="p-toolbar">
                      <span v-if="config.sendEmoji" class="p-tool-icon">😊</span>
                      <span v-if="config.sendImage" class="p-tool-icon">🖼</span>
                      <span v-if="config.sendVideo" class="p-tool-icon">🎬</span>
                      <span v-if="config.sendPdf" class="p-tool-icon">📄</span>
                      <QuestionCircleOutlined v-if="config.faqEnabled && config.faqList?.length > 0" class="p-tool-icon" style="font-size: 14px;" />
                    </div>
                    <div class="p-input-box">
                      <span class="p-input-placeholder">输入消息...</span>
                      <span class="p-send-btn" :style="{ background: config.themeColor || '#667eea' }">发送</span>
                    </div>
                  </div>
                </div>
                <!-- PC右侧区域（广告+FAQ） -->
                <div v-if="previewTab === 'pc' && hasSidebar" class="preview-sidebar">
                  <div v-if="config.pcAdImage" class="preview-ad">
                    <a :href="config.pcAdLink || '#'" target="_blank" rel="noopener">
                      <img :src="resolveUrl(config.pcAdImage)" class="preview-ad-img" alt="ad" />
                    </a>
                  </div>
                  <div v-if="config.faqEnabled && config.faqList.length > 0" class="preview-faq">
                    <div class="preview-faq-title"><QuestionCircleOutlined /> 常见问题</div>
                    <div class="preview-faq-list">
                      <div v-for="(faq, idx) in config.faqList" :key="idx" class="preview-faq-item">{{ faq.question }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </a-card>
      </div>
    </div>

    <!-- FAQ编辑弹窗 -->
    <a-modal
      v-model:open="faqEditorVisible"
      :title="faqEditIdx >= 0 ? '编辑常见问题' : '添加常见问题'"
      :width="780"
      :maskClosable="false"
      :bodyStyle="{ padding: '20px 24px' }"
      okText="保存"
      cancelText="取消"
      @ok="saveFaqItem"
      @cancel="faqEditorVisible = false"
    >
      <a-form layout="vertical" style="margin-top:8px">
        <a-form-item label="问题" :required="true" style="margin-bottom:16px">
          <a-input v-model:value="faqEditForm.question" placeholder="请输入访客常见问题，例如：你们的产品有哪些？" :maxlength="100" showCount size="large" />
        </a-form-item>
        <a-form-item v-if="faqEditPath.length === 0" style="margin-bottom:16px">
          <template #label>
            <span>匹配关键词 <span style="color:#999;font-weight:normal;font-size:12px">（可选，用户消息包含任一关键词时自动匹配此问题）</span></span>
          </template>
          <a-select
            v-model:value="faqEditForm.keywords"
            mode="tags"
            placeholder="输入关键词后按回车添加，支持多个（至少2个字符）"
            :token-separators="[',', '，']"
            style="width:100%"
          />
        </a-form-item>
        <a-form-item style="margin-bottom:0">
          <template #label>
            <span>答案 <span style="color:#999;font-weight:normal;font-size:12px">（支持富文本格式，可插入图片、链接等）</span></span>
          </template>
          <Tinymce
            v-model:modelValue="faqEditForm.answer"
            :height="300"
            :showImageUpload="true"
            :toolbar="faqEditorToolbar"
            :plugins="faqEditorPlugins"
            :menubar="''"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts" name="ChatWindowSettingsPage">
import { computed, onMounted, reactive, ref } from 'vue';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';
import { uploadImg } from '/@/api/sys/upload';
import { DeleteOutlined, EditOutlined, LeftOutlined, PlusOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue';
import { Tinymce } from '/@/components/Tinymce/index';
import { CropperUpload } from '/@/components/Cropper';
import { getFileAccessHttpUrl } from '/@/utils/common/compUtils';
import { useGlobSetting } from '/@/hooks/setting';

defineOptions({ name: 'ChatWindowSettingsPage' });
const globSetting = useGlobSetting();

function normalizeImgUrls(html: string): string {
  if (!html) return html;
  try {
    const origin = new URL(globSetting.domainUrl).origin;
    return html.replace(
      /(<img[^>]*?\ssrc=["'])(\/[^"']+)(["'])/gi,
      (_match, pre, path, suf) => `${pre}${origin}${path}${suf}`
    );
  } catch { return html; }
}

const { createMessage } = useMessage();
const saving = ref(false);
const previewTab = ref('pc');

interface FaqItem {
  question: string;
  answer: string;
  keywords?: string[];
  children?: FaqItem[];
}

const config = reactive({
  themeColor: '#667eea',
  headerVisible: true,
  pageTitle: '在线客服',
  logo: '',
  agentBubbleBgColor: '#f5f5f5',
  agentBubbleFontColor: '#333333',
  visitorBubbleBgColor: '#667eea',
  visitorBubbleFontColor: '#ffffff',
  visitorAvatar: '',
  visitorHistory: true,
  visitorMessageConnect: false,
  sendEmoji: true,
  sendImage: true,
  sendVideo: true,
  sendPdf: true,
  maxFileSize: 10,
  visitorTimezone: 'Asia/Shanghai',
  scrollText: '',
  scrollDuration: 15,
  scrollTextColor: '#ffffff',
  scrollTextBgColor: '#1890ff',
  backgroundImage: '',
  pcAdLink: '',
  pcAdImage: '',
  headerBgImage: '',
  headerIcons: [] as Array<{ icon: string; name: string; link: string; size: number; transparent: boolean }>,
  faqEnabled: false,
  faqHeaderText: '',
  faqList: [] as FaqItem[],
  hidePageTitle: false,
  hideOnlineStatus: false,
  hideAiHumanLabel: false,
  hideLogo: false,
  headerBgImageMode: 'cover' as string,
  headerBgPosition: 'center' as string,
  mobileHeaderBgImage: '',
  mobileHeaderBgImageMode: 'cover' as string,
  mobileHeaderBgPosition: 'center' as string,
  humanAgentEnabled: false,
  humanAgentFields: [] as Array<{ label: string; type: string; required: boolean }>,
  faqLinkColor: '#e8453c',
  faqNavColor: '#1890ff',
});

// FAQ编辑器状态
const faqEditorVisible = ref(false);
const faqEditPath = ref<number[]>([]);
const faqEditIdx = ref(-1); // -1=新增
const faqEditForm = reactive({ question: '', answer: '', keywords: [] as string[] });
const faqEditorPlugins = 'lists image link media fullscreen paste';
const faqEditorToolbar = 'bold italic underline strikethrough | bullist numlist | alignleft aligncenter alignright | fontsize forecolor backcolor | link image media | removeformat';

// 判断富文本内容是否实质为空（去掉HTML标签后检查）
function isHtmlEmpty(html: string): boolean {
  if (!html) return true;
  const text = html.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').trim();
  return text.length === 0;
}

// 去除HTML标签，用于列表摘要显示
function stripHtml(html: string): string {
  if (!html) return '';
  return html.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').trim();
}

function resolveFaqArray(path: number[]): FaqItem[] | null {
  let arr: FaqItem[] = config.faqList;
  for (const idx of path) {
    if (idx < 0 || idx >= arr.length) return null;
    if (!arr[idx].children) {
      arr[idx].children = [];
    }
    arr = arr[idx].children!;
  }
  return arr;
}

function openFaqEditor(path: number[], idx: number) {
  faqEditPath.value = [...path];
  faqEditIdx.value = idx;
  if (idx >= 0) {
    const arr = resolveFaqArray(path);
    const item = arr?.[idx];
    if (item) {
      faqEditForm.question = item.question;
      faqEditForm.answer = item.answer;
      faqEditForm.keywords = Array.isArray(item.keywords) ? [...item.keywords] : [];
    } else {
      faqEditForm.question = '';
      faqEditForm.answer = '';
      faqEditForm.keywords = [];
    }
  } else {
    faqEditForm.question = '';
    faqEditForm.answer = '';
    faqEditForm.keywords = [];
  }
  faqEditorVisible.value = true;
}

function saveFaqItem() {
  if (!faqEditForm.question.trim()) {
    createMessage.warning('请输入问题');
    return;
  }
  if (isHtmlEmpty(faqEditForm.answer)) {
    createMessage.warning('请输入答案');
    return;
  }
  const isTopLevel = faqEditPath.value.length === 0;
  const validKeywords = isTopLevel
    ? (faqEditForm.keywords || []).filter(k => k.trim().length >= 2).map(k => k.trim())
    : [];
  if (isTopLevel && faqEditForm.keywords.length > 0 && validKeywords.length < faqEditForm.keywords.length) {
    createMessage.warning('关键词长度至少为2个字符，已自动过滤无效关键词');
  }
  const arr = resolveFaqArray(faqEditPath.value);
  if (!arr) return;
  if (faqEditIdx.value >= 0 && faqEditIdx.value < arr.length) {
    const existing = arr[faqEditIdx.value];
    existing.question = faqEditForm.question.trim();
    existing.answer = faqEditForm.answer;
    existing.keywords = isTopLevel ? validKeywords : undefined;
  } else {
    const newItem: FaqItem = {
      question: faqEditForm.question.trim(),
      answer: faqEditForm.answer,
      keywords: isTopLevel ? validKeywords : undefined,
      children: [],
    };
    arr.push(newItem);
  }
  faqEditorVisible.value = false;
}

function removeFaq(path: number[], idx: number) {
  const arr = resolveFaqArray(path);
  if (arr && idx >= 0 && idx < arr.length) {
    arr.splice(idx, 1);
  }
}

// ==================== 人工客服字段 ====================
function addHumanAgentField() {
  config.humanAgentFields.push({ label: '', type: 'text', required: false });
}

// ==================== 头部图标 ====================
function addHeaderIcon() {
  if (config.headerIcons.length >= 5) {
    createMessage.warning('最多添加5个头部图标');
    return;
  }
  config.headerIcons.push({ icon: '', name: '', link: '', size: 32, transparent: false });
}

function removeHeaderIcon(idx: number) {
  config.headerIcons.splice(idx, 1);
}

// 是否有右侧内容
const hasSidebar = computed(() => {
  return !!config.pcAdImage || (config.faqEnabled && config.faqList.length > 0);
});

function resolveUrl(url: string) {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url;
  return getFileAccessHttpUrl(url);
}

async function fetchConfig() {
  try {
    const res = await defHttp.get({ url: '/cs/agent/global/chat-window-settings' }, { isTransformResponse: false });
    const data = res?.result || res;
    let parsed: any = null;
    if (data && typeof data === 'string') {
      try { parsed = JSON.parse(data); } catch {}
    } else if (data && typeof data === 'object') {
      parsed = data;
    }
    if (parsed) {
      Object.keys(parsed).forEach((k) => {
        if (k in config) {
          (config as any)[k] = parsed[k];
        }
      });
      // 确保 faqList 和 headerIcons 是数组
      if (!Array.isArray(config.faqList)) {
        config.faqList = [];
      }
      // 旧数据兼容：确保每个FAQ项都有keywords字段，并标准化答案中的图片URL
      config.faqList.forEach((faq: any) => {
        if (!Array.isArray(faq.keywords)) {
          faq.keywords = [];
        }
        if (!Array.isArray(faq.children)) {
          faq.children = [];
        }
        if (faq.answer) {
          faq.answer = normalizeImgUrls(faq.answer);
        }
        faq.children.forEach((child: any) => {
          if (child.answer) {
            child.answer = normalizeImgUrls(child.answer);
          }
        });
      });
      if (!Array.isArray(config.humanAgentFields)) {
        config.humanAgentFields = [];
      }
      // visitorMessageConnect 旧值兼容映射（仅 humanAgentEnabled 从未设置时生效）
      if (parsed.visitorMessageConnect === true && parsed.humanAgentEnabled === undefined) {
        config.humanAgentEnabled = true;
      }
      if (!config.headerBgImageMode) {
        config.headerBgImageMode = 'cover';
      }
      if (!Array.isArray(config.headerIcons)) {
        config.headerIcons = [];
      }
    }
  } catch (e) {
    console.error('获取聊天窗口设置失败', e);
  }
}

async function handleSave() {
  saving.value = true;
  try {
    await defHttp.put({
      url: '/cs/agent/global/chat-window-settings',
      data: { ...config },
    }, { isTransformResponse: false });
    createMessage.success('保存成功');
  } catch (e) {
    createMessage.error('保存失败');
  } finally {
    saving.value = false;
  }
}

const previewWrapperStyle = computed(() => {
  if (previewTab.value === 'pc') {
    const needSidebar = hasSidebar.value;
    return { width: needSidebar ? '680px' : '480px', height: '600px' };
  }
  return { width: '375px', height: '667px' };
});

const previewChatStyle = computed(() => ({
  flex: '1',
  minWidth: '0',
}));

function applyBgImageStyle(s: any, bgImage: string, bgMode: string, bgPosition: string) {
  if (!bgImage) return;
  s.backgroundImage = `url(${resolveUrl(bgImage)})`;
  const pos = bgPosition || 'center';
  const mode = bgMode || 'cover';
  switch (mode) {
    case 'contain':
      s.backgroundSize = 'contain';
      s.backgroundRepeat = 'no-repeat';
      s.backgroundPosition = pos;
      break;
    case 'stretch':
      s.backgroundSize = '100% 100%';
      s.backgroundPosition = pos;
      break;
    case 'repeat':
      s.backgroundSize = 'auto';
      s.backgroundRepeat = 'repeat';
      s.backgroundPosition = pos;
      break;
    case 'center':
      s.backgroundSize = 'auto';
      s.backgroundPosition = pos;
      s.backgroundRepeat = 'no-repeat';
      break;
    default:
      s.backgroundSize = 'cover';
      s.backgroundPosition = pos;
  }
}

const previewHeaderStyle = computed(() => {
  const s: any = { background: config.themeColor || '#667eea' };
  if (previewTab.value === 'mobile' && config.mobileHeaderBgImage) {
    applyBgImageStyle(s, config.mobileHeaderBgImage, config.mobileHeaderBgImageMode, config.mobileHeaderBgPosition);
  } else {
    applyBgImageStyle(s, config.headerBgImage, config.headerBgImageMode, config.headerBgPosition);
  }
  return s;
});

const msgAreaStyle = computed(() => {
  const s: any = {};
  if (config.backgroundImage) {
    s.backgroundImage = `url(${resolveUrl(config.backgroundImage)})`;
    s.backgroundSize = 'cover';
    s.backgroundPosition = 'center';
  }
  return s;
});

onMounted(() => {
  fetchConfig();
});
</script>

<style scoped>
.chat-window-settings {
  padding: 16px;
}
.settings-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.settings-form {
  flex: 1;
  min-width: 0;
  max-width: 640px;
}
.settings-preview {
  flex: 0 0 auto;
  position: sticky;
  top: 16px;
}
.color-picker-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.color-input {
  width: 36px;
  height: 32px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  padding: 2px;
  cursor: pointer;
  background: transparent;
}
.switch-hint {
  margin-left: 8px;
  color: #999;
  font-size: 12px;
}
.upload-hint {
  color: #999;
  font-size: 12px;
  margin-top: 4px;
}

/* 图片预览缩略图 + 删除按钮 */
.preview-thumb {
  margin-top: 8px;
  width: 60px;
  height: 60px;
  border: 1px solid #eee;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
}
.preview-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.preview-thumb--wide {
  width: 120px;
  height: 60px;
}
.thumb-delete {
  position: absolute;
  top: 2px;
  right: 2px;
  background: rgba(0,0,0,0.5);
  color: #fff;
  border-radius: 50%;
  padding: 3px;
  font-size: 12px;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
}
.preview-thumb:hover .thumb-delete {
  opacity: 1;
}

/* 头部图标配置区 */
.header-icons-section {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
}
.header-icons-empty {
  text-align: center;
  padding: 16px 0;
}
.header-icon-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 8px;
}
.header-icon-row:hover {
  border-color: #d9d9d9;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.header-icon-row-num {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #667eea;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 500;
  flex-shrink: 0;
  margin-top: 4px;
}
.header-icon-row-fields {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.header-icon-field {
  display: flex;
  align-items: flex-start;
  gap: 6px;
}
.header-icon-label {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
  line-height: 32px;
}

/* FAQ配置区 */
.faq-section {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
  max-height: 600px;
  overflow-y: auto;
}
.faq-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.faq-count {
  color: #666;
  font-size: 13px;
}
.faq-limit-hint {
  color: #ff4d4f;
  font-size: 12px;
}
.faq-empty {
  text-align: center;
  color: #bbb;
  padding: 32px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.faq-tree-node {
  margin-bottom: 4px;
}
.faq-tree-main {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}
.faq-tree-main:hover {
  border-color: #d9d9d9;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.faq-tree-prefix {
  color: #bbb;
  margin-right: 8px;
  font-family: monospace;
  flex-shrink: 0;
}
.faq-tree-body {
  flex: 1;
  min-width: 0;
}
.faq-tree-question {
  font-size: 13px;
  color: #333;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 2px;
}
.faq-tree-answer {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 380px;
}
.faq-tree-keywords {
  margin-top: 4px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.faq-tree-keywords .ant-tag {
  margin: 0;
  font-size: 11px;
  line-height: 18px;
  padding: 0 6px;
}
.faq-tree-actions {
  flex-shrink: 0;
  display: flex;
  gap: 2px;
  align-items: center;
  opacity: 0;
  transition: opacity 0.2s;
}
.faq-tree-main:hover .faq-tree-actions {
  opacity: 1;
}

/* ========== 人工客服字段 ========== */
.human-agent-fields-section {
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 12px;
  background: #fafafa;
}
.human-agent-field-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  padding: 6px 8px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
}
.human-agent-field-row:last-child { margin-bottom: 0; }

/* ========== 预览区 ========== */
.preview-container {
  display: flex;
  justify-content: center;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 8px;
  min-height: 650px;
}
.preview-wrapper {
  display: flex;
  flex-direction: column;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  transition: all 0.3s;
}
.preview-body {
  display: flex;
  flex: 1;
  min-height: 0;
}
.preview-chat {
  display: flex;
  flex-direction: column;
  background: #fff;
  overflow: hidden;
}
/* 全宽头部 */
.p-header-full {
  padding: 12px 16px;
  color: #fff;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.p-back-btn {
  font-size: 16px;
  color: #fff;
  margin-right: 8px;
  flex-shrink: 0;
  cursor: default;
}
.p-header-content {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}
.p-header-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.p-status-line {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  opacity: 0.9;
}
.p-status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #52c41a;
  box-shadow: 0 0 3px #52c41a;
  flex-shrink: 0;
}
.p-status-text {
  font-size: 11px;
}
.p-header-icons {
  display: flex;
  align-items: center;
  gap: 10px;
}
.p-header-icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-decoration: none;
  color: #fff;
  gap: 2px;
}
.p-header-icon-img {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid rgba(255,255,255,0.3);
}
.p-header-icon-img.p-header-icon-transparent {
  border: none;
  border-radius: 0;
  background: transparent;
}
.p-header-icon-placeholder {
  font-size: 18px;
}
.p-header-icon-name {
  font-size: 10px;
  opacity: 0.9;
  max-width: 48px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.p-logo {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}
.p-title {
  font-size: 15px;
  font-weight: 500;
}
.p-scroll-bar {
  overflow: hidden;
  white-space: nowrap;
  padding: 4px 0;
  font-size: 12px;
  flex-shrink: 0;
}
.p-scroll-text {
  display: inline-block;
  animation: marquee 15s linear infinite;
  padding-left: 100%;
}
@keyframes marquee {
  0% { transform: translateX(0); }
  100% { transform: translateX(-100%); }
}
.p-messages {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #fff;
}
.p-msg {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.p-msg-visitor {
  justify-content: flex-end;
}
.p-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  overflow: hidden;
}
.p-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.p-bubble {
  padding: 8px 12px;
  border-radius: 12px;
  font-size: 13px;
  max-width: 70%;
  line-height: 1.5;
  word-break: break-word;
}
.p-input-area {
  border-top: 1px solid #f0f0f0;
  padding: 8px 12px;
  flex-shrink: 0;
  background: #fff;
}
.p-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 6px;
  font-size: 16px;
}
.p-tool-icon {
  cursor: default;
  opacity: 0.7;
}
.p-input-box {
  display: flex;
  align-items: center;
  background: #f5f5f5;
  border-radius: 20px;
  padding: 6px 12px;
}
.p-input-placeholder {
  flex: 1;
  color: #bbb;
  font-size: 13px;
}
.p-send-btn {
  color: #fff;
  padding: 4px 14px;
  border-radius: 16px;
  font-size: 12px;
  cursor: default;
}

/* 手机端FAQ预览（输入框上方） */

/* PC右侧区域 */
.preview-sidebar {
  width: 200px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #fafafa;
  border-left: 1px solid #f0f0f0;
  overflow: hidden;
}
.preview-ad {
  flex-shrink: 0;
  display: flex;
  align-items: flex-start;
  justify-content: center;
}
.preview-ad-img {
  width: 100%;
  object-fit: cover;
  display: block;
}
.preview-faq {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px;
  border-top: 1px solid #f0f0f0;
}
.preview-faq-title {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
}
.preview-faq-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.preview-faq-item {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 12px;
  color: #333;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.preview-faq-item:hover {
  background: #e6f7ff;
  border-color: #91d5ff;
}
</style>
