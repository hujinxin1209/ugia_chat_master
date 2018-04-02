package org.ugia_chat_server.handler.message.impl;

import org.ugia_chat_server.handler.message.*;
import com.ugia.common.domain.*;
import com.ugia.common.domain.Message;
import com.ugia.common.domain.Response;
import com.ugia.common.domain.ResponseHeader;
import com.ugia.common.enumeration.ResponseType;
import com.ugia.common.util.ProtoStuffUtil;
import com.ugia.common.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by SinjinSong on 2017/5/23.
 */
@Component("MessageHandler.broadcast")
public class BroadcastMessageHandler extends MessageHandler {
    @Override
    public void handle(Message message, Selector server, SelectionKey client, BlockingQueue<Task> queue, AtomicInteger onlineUsers) {
        try {
            byte[] response = ProtoStuffUtil.serialize(
                    new Response(
                            ResponseHeader.builder()
                                    .type(ResponseType.NORMAL)
                                    .sender(message.getHeader().getSender())
                                    .timestamp(message.getHeader().getTimestamp()).build(),
                                    message.getBody()));
            super.broadcast(response,server);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
