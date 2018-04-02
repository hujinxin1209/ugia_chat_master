package org.ugia_chat_server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.ugia_chat_server.exception.handler.InterruptedExceptionhandler;
import org.ugia_chat_server.handler.message.MessageHandler;
import org.ugia_chat_server.util.SpringContextUtil;

import com.ugia.common.domain.*;
import com.ugia.common.util.ProtoStuffUtil;

import lombok.extern.slf4j.Slf4j;


//@Slf4j
public class ChatServer {
	public static final int DEFAULT_BUFFER_SIZE = 1024;
	public static final int PORT = 9000;
	public static final String QUIT = "QUIT";
	private AtomicInteger onlineUsers;
	
	private ServerSocketChannel serverSocketChannel;
	private Selector selector;
	private ExecutorService readPool;
	private BlockingQueue<Task> downloadTaskQueue;
	private ListenerThread listenerThread;
	//private TaskManagerThread taskManagerThread;
	private InterruptedExceptionhandler exceptionHandler;
	
	public ChatServer() {
		//log.info("服务器启动");
		initServer();
	}
	
	private void initServer() {
		try {
			serverSocketChannel = ServerSocketChannel.open();
			// 切换为非租塞模式
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.bind(new InetSocketAddress(PORT));
			// 获得选择器
			selector = Selector.open();
			//将channel注册到selector上
            //第二个参数是选择键，用于说明selector监控channel的状态
            //可能的取值：SelectionKey.OP_READ OP_WRITE OP_CONNECT OP_ACCEPT
            //监控的是channel的接收状态
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
			this.readPool = new ThreadPoolExecutor(5, 10, 1000, TimeUnit.MILLISECONDS,
					new ArrayBlockingQueue<Runnable>(10), new ThreadPoolExecutor.CallerRunsPolicy());
			this.downloadTaskQueue = new ArrayBlockingQueue<Task>(20);
			//this.taskManagerThread = new TaskManagerThread(downloadTaskQueue);
			this.listenerThread = new ListenerThread();
            this.onlineUsers = new AtomicInteger(0);
            //this.exceptionHandler = SpringContextUtil.getBean("interruptedExceptionHandler");
		} catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	/**
     * 启动方法，线程最好不要在构造函数中启动，应该作为一个单独方法，或者使用工厂方法来创建实例
     * 避免构造未完成就使用成员变量
     */
    public void launch() {
        new Thread(listenerThread).start();
        //new Thread(taskManagerThread).start();
    }
    
    /**
     * 关闭服务器
     */
    public void shutdownServer() {
        try {
            //taskManagerThread.shutdown();
            listenerThread.shutdown();
            readPool.shutdown();
            serverSocketChannel.close();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
	private class ListenerThread extends Thread{
		@Override
		public void interrupt(){
			try {
				selector.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		@Override
		public void run() {
			try {
				while(!Thread.currentThread().isInterrupted()) {
					selector.select();
					Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
					while(keyIterator.hasNext()) {
						SelectionKey key = keyIterator.next();
						keyIterator.remove();
						if (key.isAcceptable()) {
                            //交由接收事件的处理器处理
                            handleAcceptRequest();
                        } else if (key.isReadable()) {
                            //如果"读取"事件已就绪
                            //取消可读触发标记，本次处理完后才打开读取事件标记
                            key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));
                            //交由读取事件的处理器处理
                            readPool.execute(new ReadEventHandler(key));
                        }
					}
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		public void shutdown() {
			Thread.currentThread().interrupt();
		}
	}
	private void handleAcceptRequest() {
		try {
			SocketChannel client = serverSocketChannel.accept();
			client.configureBlocking(false);
			// 监控客户端的读操作是否就绪
			client.register(selector, SelectionKey.OP_READ);
			//log.info("服务器连接客户端：{}", client);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	private class ReadEventHandler implements Runnable{
		private ByteBuffer buf;
		private SocketChannel client;
		private ByteArrayOutputStream baos;
		private SelectionKey key;
		
		public ReadEventHandler(SelectionKey key) {
			this.key = key;
			this.client = (SocketChannel)key.channel();
			this.buf = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
			this.baos = new ByteArrayOutputStream();
		}
		
		public void run() {
			try {
				int size;
				while((size = client.read(buf)) > 0) {
					buf.flip();
					baos.write(buf.array(), 0, size);
					buf.clear();
				}
				if(size == -1) {
					return ;
				}
				//log.info("读完了，继续侦听");
				// 继续侦听
				key.interestOps(key.interestOps() | SelectionKey.OP_READ);
				key.selector().wakeup();
				byte[] bytes = baos.toByteArray();
				baos.close();
				Message message = ProtoStuffUtil.deserialize(bytes, Message.class);
				MessageHandler messageHandler = SpringContextUtil.getBean("MessageHandler", 
						message.getHeader().getType().toString().toLowerCase());
				try {
					messageHandler.handle(message, selector, key, downloadTaskQueue, onlineUsers);
				} catch(InterruptedException e) {
					//log.error("服务器线程中断");
					exceptionHandler.handle(client, message);
					e.printStackTrace();
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	public static void main(String[] args) {
        System.out.println("Initialing...");
        ChatServer chatServer = new ChatServer();
        chatServer.launch();
        Scanner scanner = new Scanner(System.in, "UTF-8");
        while (scanner.hasNext()) {
            String next = scanner.next();
            if (next.equalsIgnoreCase(QUIT)) {
                System.out.println("服务器准备关闭");
                chatServer.shutdownServer();
                System.out.println("服务器已关闭");
            }
        }
    }
}
