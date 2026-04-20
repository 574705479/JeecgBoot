;(function (w) {
  function createEl(tag, attrs, children) {
    var el = document.createElement(tag)
    if (attrs) {
      Object.keys(attrs).forEach(function (key) {
        if (key === 'style') {
          Object.assign(el.style, attrs.style)
        } else if (key === 'className') {
          el.className = attrs.className
        } else {
          el.setAttribute(key, attrs[key])
        }
      })
    }
    if (children) {
      children.forEach(function (child) {
        if (typeof child === 'string') {
          el.appendChild(document.createTextNode(child))
        } else if (child) {
          el.appendChild(child)
        }
      })
    }
    return el
  }

  function buildChatUrl(baseUrl, token, externalUserId, userName, source, key, agentId) {
    var url = baseUrl.replace(/\/$/, '') + '/cs/userChat'
    var params = []
    if (key) params.push('key=' + encodeURIComponent(key))
    if (token) params.push('token=' + encodeURIComponent(token))
    if (externalUserId) params.push('externalUserId=' + encodeURIComponent(externalUserId))
    if (userName) params.push('userName=' + encodeURIComponent(userName))
    if (source) params.push('source=' + encodeURIComponent(source))
    if (agentId) params.push('agentId=' + encodeURIComponent(agentId))
    return url + (params.length ? '?' + params.join('&') : '')
  }

  function Widget(opts) {
    this.options = opts || {}
    this.button = null
    this.overlay = null
    this.panel = null
    this.iframe = null
    this.opened = false
    this.minimized = false
  }

  Widget.prototype.init = function () {
    var _this = this
    if (this.button) return
    var opts = this.options

    // 按钮位置（支持上下左右边距）
    var position = opts.position || {}
    var btnRight = position.right != null ? position.right : 24
    var btnBottom = position.bottom != null ? position.bottom : 24
    var btnLeft = position.left != null ? position.left : null
    var btnTop = position.top != null ? position.top : null

    // 按钮外观
    var btnSize = opts.buttonSize || 56
    var btnColor = opts.buttonColor || '#4c6ef5'
    var btnIcon = opts.buttonIcon || ''
    var btnText = opts.buttonText || ''
    var btnBorderRadius = opts.buttonBorderRadius != null ? opts.buttonBorderRadius : btnSize / 2
    var btnShadow = opts.buttonShadow != null ? opts.buttonShadow : '0 6px 18px rgba(0,0,0,.2)'

    var btnStyle = {
      position: 'fixed',
      width: btnSize + 'px',
      height: btnSize + 'px',
      borderRadius: btnBorderRadius + 'px',
      background: btnColor,
      color: '#fff',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      cursor: 'pointer',
      boxShadow: btnShadow,
      zIndex: (opts.zIndex || 9999).toString(),
      border: 'none',
      padding: '0',
      overflow: 'hidden',
      transition: 'transform .2s, box-shadow .2s'
    }
    // 定位：支持四个方向
    if (btnRight != null && btnLeft == null) btnStyle.right = btnRight + 'px'
    if (btnLeft != null) btnStyle.left = btnLeft + 'px'
    if (btnBottom != null && btnTop == null) btnStyle.bottom = btnBottom + 'px'
    if (btnTop != null) btnStyle.top = btnTop + 'px'

    // 按钮内容：优先自定义图标，其次文字，最后默认
    var btnChildren = []
    // 嵌入端 cse:// 自动转匿名代理 URL：
    //  baseUrl 是站点根域名（如 https://kefu.com），后端 API 在 /jeecg-boot/ 上下文下，
    //  所以默认拼 ${baseUrl}/jeecg-boot/cs/brand/file/{fid}（标准 jeecg 部署）。
    //  自定义 server.servlet.context-path 部署可通过 options.apiBase 覆盖。
    if (btnIcon && typeof btnIcon === 'string' && btnIcon.indexOf('cse://') === 0) {
      var widgetBaseUrl = (opts.baseUrl || '').replace(/\/$/, '')
      var widgetApiBase = opts.apiBase || (widgetBaseUrl ? widgetBaseUrl + '/jeecg-boot' : '')
      var widgetFidMatch = btnIcon.substring('cse://'.length).match(/^[a-zA-Z0-9]{20,40}/)
      if (widgetFidMatch && widgetApiBase) {
        btnIcon = widgetApiBase + '/cs/brand/file/' + widgetFidMatch[0]
      } else {
        try {
          console.warn('[cs-widget] cse:// 转匿名代理失败：缺少 baseUrl 或 fid 格式异常，回退默认按钮。')
        } catch (_e) {}
        btnIcon = ''
      }
    }
    if (btnIcon) {
      btnChildren.push(createEl('img', {
        style: {
          width: Math.round(btnSize * 0.6) + 'px',
          height: Math.round(btnSize * 0.6) + 'px',
          objectFit: 'contain',
          borderRadius: '0',
          pointerEvents: 'none'
        },
        src: btnIcon
      }))
    } else if (btnText) {
      btnChildren.push(createEl('span', {
        style: { fontSize: Math.max(12, Math.round(btnSize * 0.25)) + 'px', fontWeight: '500' }
      }, [btnText]))
    } else {
      // 默认 SVG 图标
      var svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
      svg.setAttribute('viewBox', '0 0 24 24')
      svg.setAttribute('fill', 'none')
      svg.setAttribute('stroke', 'currentColor')
      svg.setAttribute('stroke-width', '2')
      svg.style.width = Math.round(btnSize * 0.5) + 'px'
      svg.style.height = Math.round(btnSize * 0.5) + 'px'
      svg.innerHTML = '<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" stroke-linecap="round" stroke-linejoin="round"/>'
      btnChildren.push(svg)
    }

    this.button = createEl('div', {
      className: 'jeecg-cs-button',
      style: btnStyle
    }, btnChildren)

    // hover 效果
    this.button.addEventListener('mouseenter', function () {
      _this.button.style.transform = 'scale(1.08)'
    })
    this.button.addEventListener('mouseleave', function () {
      _this.button.style.transform = 'scale(1)'
    })

    this.button.addEventListener('click', function () {
      if (_this.opened) {
        _this.close()
      } else {
        _this.open()
      }
    })
    document.body.appendChild(this.button)
  }

  Widget.prototype.open = function () {
    var _this = this
    var opts = this.options
    this.opened = true
    this.minimized = false
    if (!this.overlay) {
      this.overlay = createEl('div', {
        className: 'jeecg-cs-overlay',
        style: {
          position: 'fixed',
          inset: '0',
          background: 'rgba(0,0,0,.25)',
          zIndex: (opts.zIndex || 9999).toString()
        }
      })
      this.overlay.addEventListener('click', function () {
        _this.close()
      })
    }
    if (!this.panel) {
      var width = opts.width || 420
      var height = opts.height || 640
      var panelColor = opts.panelColor || '#4c6ef5'
      this.panel = createEl('div', {
        className: 'jeecg-cs-panel',
        style: {
          position: 'fixed',
          right: '24px',
          bottom: '90px',
          width: width + 'px',
          height: height + 'px',
          background: '#fff',
          borderRadius: '12px',
          overflow: 'hidden',
          boxShadow: '0 12px 30px rgba(0,0,0,.2)',
          zIndex: (opts.zIndex || 9999).toString()
        }
      })
      var header = createEl('div', {
        className: 'jeecg-cs-header',
        style: {
          height: '40px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0 12px',
          background: panelColor,
          color: '#fff',
          fontSize: '14px'
        }
      }, [
        createEl('span', null, [opts.title || '在线客服']),
        createEl('div', {
          style: {
            display: 'flex',
            alignItems: 'center'
          }
        }, [
          createEl('button', {
            className: 'jeecg-cs-min',
            style: {
              marginRight: '8px',
              width: '20px',
              height: '20px',
              border: '0',
              background: 'rgba(255,255,255,.3)',
              color: '#fff',
              borderRadius: '4px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '0',
              lineHeight: '20px',
              fontSize: '14px'
            }
          }, ['—']),
          createEl('button', {
            className: 'jeecg-cs-close',
            style: {
              width: '20px',
              height: '20px',
              border: '0',
              background: 'rgba(255,255,255,.3)',
              color: '#fff',
              borderRadius: '4px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '0',
              lineHeight: '20px',
              fontSize: '14px'
            }
          }, ['×'])
        ])
      ])
      this.iframe = createEl('iframe', {
        style: {
          width: '100%',
          height: 'calc(100% - 40px)',
          border: '0'
        }
      })
      header.querySelector('.jeecg-cs-min').addEventListener('click', function (e) {
        e.stopPropagation()
        _this.minimize()
      })
      header.querySelector('.jeecg-cs-close').addEventListener('click', function (e) {
        e.stopPropagation()
        _this.close()
      })
      this.panel.appendChild(header)
      this.panel.appendChild(this.iframe)
    }
    document.body.appendChild(this.overlay)
    document.body.appendChild(this.panel)

    var tokenPromise = Promise.resolve(opts.token || '')
    if (typeof opts.getToken === 'function') {
      tokenPromise = Promise.resolve(opts.getToken())
    }
    tokenPromise.then(function (token) {
      _this.options.token = token
      var url = buildChatUrl(
        _this.options.baseUrl || '',
        token,
        _this.options.externalUserId || '',
        _this.options.userName || '',
        _this.options.source || '',
        _this.options.key || '',
        _this.options.agentId || ''
      )
      _this.iframe.setAttribute('src', url)
    })
  }

  Widget.prototype.close = function () {
    this.opened = false
    this.minimized = false
    if (this.overlay && this.overlay.parentNode) {
      this.overlay.parentNode.removeChild(this.overlay)
    }
    if (this.panel && this.panel.parentNode) {
      this.panel.parentNode.removeChild(this.panel)
    }
  }

  Widget.prototype.minimize = function () {
    this.minimized = true
    if (this.overlay && this.overlay.parentNode) {
      this.overlay.parentNode.removeChild(this.overlay)
    }
    if (this.panel && this.panel.parentNode) {
      this.panel.parentNode.removeChild(this.panel)
    }
  }

  Widget.prototype.destroy = function () {
    this.close()
    if (this.button && this.button.parentNode) {
      this.button.parentNode.removeChild(this.button)
    }
    this.button = null
  }

  w.JeecgCsWidget = {
    init: function (options) {
      var widget = new Widget(options || {})
      widget.init()
      return widget
    }
  }
})(window)
