package org.ugia_chat_server.exception.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.springframework.stereotype.Component;

import org.ugia_chat_server.property.PromptMsgProperty;
import com.ugia.common.domain.Message;
import com.ugia.common.domain.Response;
import com.ugia.common.domain.ResponseHeader;
import com.ugia.common.enumeration.ResponseType;
import com.ugia.common.util.ProtoStuffUtil;

@Component("interruptedExceptionHandler")
public class InterruptedExceptionhandler {
	public void handle(SocketChannel channel,Message message) {
        try {
            byte[] response = ProtoStuffUtil.serialize(
                    new Response(
                            ResponseHeader.builder()
                                    .type(ResponseType.PROMPT)
                                    .sender(message.getHeader().getSender())
                                    .timestamp(message.getHeader().getTimestamp()).build(),
                            PromptMsgProperty.SERVER_ERROR.getBytes(PromptMsgProperty.charset)));
            channel.write(ByteBuffer.wrap(response));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
