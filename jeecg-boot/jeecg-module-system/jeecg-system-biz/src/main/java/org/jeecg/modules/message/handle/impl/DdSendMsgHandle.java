package org.jeecg.modules.message.handle.impl;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.dto.message.MessageDTO;
import org.jeecg.modules.message.handle.ISendMsgHandle;
import org.springframework.stereotype.Component;

@Slf4j
@Component("ddSendMsgHandle")
public class DdSendMsgHandle implements ISendMsgHandle {

	@Override
	public void sendMsg(String esReceiver, String esTitle, String esContent) {
		log.warn("钉钉消息推送功能已禁用（weixin4j已移除）");
	}

	@Override
	public void sendMessage(MessageDTO messageDTO) {
		log.warn("钉钉消息推送功能已禁用（weixin4j已移除）");
	}

}
