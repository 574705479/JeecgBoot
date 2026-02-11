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

  function buildChatUrl(baseUrl, token, externalUserId, userName, source, key) {
    var url = baseUrl.replace(/\/$/, '') + '/cs/userChat'
    var params = []
    if (key) params.push('key=' + encodeURIComponent(key))
    if (token) params.push('token=' + encodeURIComponent(token))
    if (externalUserId) params.push('externalUserId=' + encodeURIComponent(externalUserId))
    if (userName) params.push('userName=' + encodeURIComponent(userName))
    if (source) params.push('source=' + encodeURIComponent(source))
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
    var position = (this.options.position || {}).right != null
      ? this.options.position
      : { right: 24, bottom: 24 }
    this.button = createEl('div', {
      className: 'jeecg-cs-button',
      style: {
        position: 'fixed',
        right: position.right + 'px',
        bottom: position.bottom + 'px',
        width: '56px',
        height: '56px',
        borderRadius: '50%',
        background: '#4c6ef5',
        color: '#fff',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        cursor: 'pointer',
        boxShadow: '0 6px 18px rgba(0,0,0,.2)',
        zIndex: (this.options.zIndex || 9999).toString()
      }
    }, ['客服'])
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
    this.opened = true
    this.minimized = false
    if (!this.overlay) {
      this.overlay = createEl('div', {
        className: 'jeecg-cs-overlay',
        style: {
          position: 'fixed',
          inset: '0',
          background: 'rgba(0,0,0,.25)',
          zIndex: (this.options.zIndex || 9999).toString()
        }
      })
      this.overlay.addEventListener('click', function () {
        _this.close()
      })
    }
    if (!this.panel) {
      var width = this.options.width || 420
      var height = this.options.height || 640
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
          zIndex: (this.options.zIndex || 9999).toString()
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
          background: '#4c6ef5',
          color: '#fff',
          fontSize: '14px'
        }
      }, [
        createEl('span', null, [this.options.title || '在线客服']),
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

    var tokenPromise = Promise.resolve(this.options.token || '')
    if (typeof this.options.getToken === 'function') {
      tokenPromise = Promise.resolve(this.options.getToken())
    }
    tokenPromise.then(function (token) {
      _this.options.token = token
      var url = buildChatUrl(
        _this.options.baseUrl || '',
        token,
        _this.options.externalUserId || '',
        _this.options.userName || '',
        _this.options.source || '',
        _this.options.key || ''
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
