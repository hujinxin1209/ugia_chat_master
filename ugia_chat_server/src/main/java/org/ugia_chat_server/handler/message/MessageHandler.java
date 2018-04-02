package org.ugia_chat_server.handler.message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.ugia.common.domain.Message;
import com.ugia.common.domain.Task;

public abstract class MessageHandler {
	public static final String SYSTEM_SENDER = "系统提示";
	abstract public void handle(Message message, Selector selector, SelectionKey selectionKey,
			BlockingQueue<Task> queue, AtomicInteger onlineUsers) throws InterruptedException;
	protected void broadcast(byte[] data, Selector server) throws IOException{
		for(SelectionKey selectionKey : server.keys()) {
			Channel channel = selectionKey.channel();
			if(channel instanceof SocketChannel) {
				SocketChannel deSocketChannel = (SocketChannel) channel;
				if(deSocketChannel.isConnected()) {
					deSocketChannel.write(ByteBuffer.wrap(data));
				}
			}
		}
	}
	
}
