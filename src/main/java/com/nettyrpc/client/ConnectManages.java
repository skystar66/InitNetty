package com.nettyrpc.client;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nettyrpc.protocol.MarshallingCodeFactory;
import com.nettyrpc.server.RpcHandler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class ConnectManages {

	private static final Logger logger = LoggerFactory.getLogger(ConnectManages.class);
	private volatile static ConnectManages connectManage;

	private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
	private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16, 600L, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(65536)); //ArrayBlockingQueue 有界队列 也是一个阻塞队列 设置线程池

	private CopyOnWriteArrayList<RpcClientHandler> connectedHandlers = new CopyOnWriteArrayList<>();//并发设置处理器
	private Map<InetSocketAddress, RpcClientHandler> connectedServerNodes = new ConcurrentHashMap<>();
	// private Map<InetSocketAddress, Channel> connectedServerNodes = new
	// ConcurrentHashMap<>();

	private ReentrantLock lock = new ReentrantLock(); //线程对象锁
	private Condition connected = lock.newCondition();// 条件变量Condition
	private long connectTimeoutMillis = 6000;   //连接超时时间设置
	private AtomicInteger roundRobin = new AtomicInteger(0); //并发计数器技术处理器次数  保证原子性 
	private volatile boolean isRuning = true;

	private HashSet<InetSocketAddress> newAllServerNodeSet;

	private ChannelFuture channelFuture;

	private Bootstrap b;

	private ConnectManages() {
	}

	//采用单例模式  双重检查锁定  懒汉式单例
	public static ConnectManages getInstance() {
		if (connectManage == null) {
			synchronized (ConnectManages.class) {
				if (connectManage == null) {
					connectManage = new ConnectManages();
				}
			}
		}
		return connectManage;
	}

	public void updateConnectedServer(List<String> allServerAddress) {
		if (allServerAddress != null) {
			if (allServerAddress.size() > 0) { // Get available server node
				// update local serverNodes cache
				newAllServerNodeSet = new HashSet<InetSocketAddress>();
				for (int i = 0; i < allServerAddress.size(); ++i) {
					String[] array = allServerAddress.get(i).split(":");
					if (array.length == 2) { // Should check IP and port
						String host = array[0];
						int port = Integer.parseInt(array[1]);
						final InetSocketAddress remotePeer = new InetSocketAddress(host, port);
						newAllServerNodeSet.add(remotePeer);
					}
				}

				// 添加新的服务节点
				for (final InetSocketAddress serverNodeAddress : newAllServerNodeSet) {
					if (!connectedServerNodes.keySet().contains(serverNodeAddress)) {
						connectServerNode(serverNodeAddress);
					}
				}

				deleteNodeAndCloseServer();
			} else { // No available server node ( All server nodes are down )
				logger.error("没有可用的服务器节点。.所有服务器节点都已关闭 !!!");
				for (final RpcClientHandler connectedServerHandler : connectedHandlers) {
					SocketAddress remotePeer = connectedServerHandler.getRemotePeer();
					RpcClientHandler handler = connectedServerNodes.get(remotePeer);
					handler.close();
					connectedServerNodes.remove(connectedServerHandler);
				}
				connectedHandlers.clear();
			}
		}
	}

	public void deleteNodeAndCloseServer() {
		// 关闭并删除无效的服务器节点
		logger.info("关闭并删除无效的服务器节点");
		for (int i = 0; i < connectedHandlers.size(); ++i) {
			RpcClientHandler connectedServerHandler = connectedHandlers.get(i);
			SocketAddress remotePeer = connectedServerHandler.getRemotePeer();
			if (!newAllServerNodeSet.contains(remotePeer)) {
				logger.info("Remove invalid server node " + remotePeer);
				RpcClientHandler handler = connectedServerNodes.get(remotePeer);
				if (handler != null) {
					handler.close();
				}
				connectedServerNodes.remove(remotePeer);
				connectedHandlers.remove(connectedServerHandler);
			}
		}
		connectedHandlers.clear();
		logger.info("删除完成，处理器数量：" + connectedHandlers.size());
	}

	public void reconnect(final RpcClientHandler handler, final SocketAddress remotePeer) {
		if (handler != null) {
			connectedHandlers.remove(handler);
			connectedServerNodes.remove(handler.getRemotePeer());
		}
		connectServerNode((InetSocketAddress) remotePeer);
	}

	/*
	 * 关于同步的阻塞和异步的非阻塞可以打一个很简单的比方，A向B打电话，通知B做一件事。
	 * 
	 * 在同步模式下，A告诉B做什么什么事，然后A依然拿着电话，等待B做完，才可以做下一件事；
	 * 
	 * 在异步模式下，A告诉B做什么什么事，A挂电话，做自己的事。B做完后，打电话通知A做完了。
	 */

	private void connectServerNode(final InetSocketAddress remotePeer) {
		logger.info("初始化客户端请求服务端添加一个线程方法：connectServerNode");
		threadPoolExecutor.submit(new Runnable() {
			@Override
			public void run() {
				logger.info("执行客户端线程========================");
				// 这段是异步非阻塞的代码
				b = new Bootstrap();
				b.group(eventLoopGroup).channel(NioSocketChannel.class)
						// .option(ChannelOption.SO_KEEPALIVE, true)// 保持连接
						// .option(ChannelOption.SO_BACKLOG, 1024).option(ChannelOption.TCP_NODELAY,
						// true)

						.handler(new RpcClientInitializer());
				channelFuture = getChannelFuture(remotePeer);
				// channelFuture的监听函数 监听到connect连接处理成功完成
				channelFuture.addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(final ChannelFuture channelFuture) throws Exception {
						if (channelFuture.isSuccess()) {
							logger.info("Successfully connect to remote server. remote peer = " + remotePeer);
							// channelFuture.channel() 得到该请求通道
							RpcClientHandler handler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
							addHandler(handler);
						}
					}
				});
				try {
					channelFuture.channel().closeFuture().sync();
					logger.info("服务端通讯断开。。。。。重新建立连接");
					// connect(remotePeer);
					connectAgain(remotePeer);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

	}

	// 建立一个子线程重新连接

	public void connectAgain(InetSocketAddress remotePeer) {
		try {
			logger.info("再次发送数据。。。");
			b = new Bootstrap();
			b.group(eventLoopGroup).channel(NioSocketChannel.class).handler(new RpcClientInitializer());

			logger.info("旧channel=" + getChannelFuture(remotePeer).channel().isActive() + "");
			logger.info("旧channe=" + getChannelFuture(remotePeer).channel().isOpen() + "");
			ChannelFuture cf = getChannelFuture(remotePeer);
			logger.info("新channel=" + cf.channel().isActive() + "");
			logger.info("新channe=" + cf.channel().isOpen() + "");
			logger.info("连接成功。。。");
			// channelFuture的监听函数 监听到connect连接处理成功完成
			cf.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(final ChannelFuture channelFuture) throws Exception {
					if (channelFuture.isSuccess()) {
						logger.info("Successfully connect to remote server. remote peer = " + remotePeer);
						// channelFuture.channel() 得到该请求通道
						RpcClientHandler handler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
						addHandler(handler);
					}
				}
			});
			cf.channel().closeFuture().sync();
			logger.info("服务端通讯断开。。。。。重新建立连接");
			connectAgain(remotePeer);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void connect(InetSocketAddress remotePeer) {
		try {
			this.channelFuture = b.connect(remotePeer).sync();
			logger.info("服务器已建立连接进行通讯。。。");
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	public ChannelFuture getChannelFuture(InetSocketAddress remotePeer) {
		if (channelFuture == null) {
			// 建立连接
			connect(remotePeer);
		}
		// 如果通道没有存活 重新建立连接
		if (!channelFuture.channel().isActive()) {
			connect(remotePeer);
		}
		return this.channelFuture;
	}

	private void addHandler(RpcClientHandler handler) {
		connectedHandlers.add(handler);
		InetSocketAddress remoteAddress = (InetSocketAddress) handler.getChannel().remoteAddress();
		connectedServerNodes.put(remoteAddress, handler);
		signalAvailableHandler();
	}

	private void signalAvailableHandler() {
		lock.lock();// 获得线程锁
		try {
			logger.info("获得线程锁，响应唤醒所有等待线程========================");
			connected.signalAll();// 响应所有等待 对应notifyALL
		} finally {
			logger.info("释放线程锁，（唤醒）========================");
			lock.unlock();// 释放锁
		}
	}

	private boolean waitingForHandler() throws InterruptedException {
		lock.lock();// 获得线程锁
		try {
			logger.info("获得线程锁，等待线程========================");
			return connected.await(this.connectTimeoutMillis, TimeUnit.MILLISECONDS);// 等待响应 对应wait
		} finally {
			logger.info("释放线程锁，（等待）========================");
			lock.unlock();// 释放锁
		}
	}

	public RpcClientHandler chooseHandler() {
		CopyOnWriteArrayList<RpcClientHandler> handlers = (CopyOnWriteArrayList<RpcClientHandler>) this.connectedHandlers
				.clone();
		int size = handlers.size();
		logger.info("当前处理器数量：" + handlers.size());
		while (isRuning && size <= 0) {
			try {
				logger.info("获得相应处理器，等待线程waitingForHandler========================");
				boolean available = waitingForHandler();
				logger.info("线程返回结果========================" + available);
				if (available) {
					handlers = (CopyOnWriteArrayList<RpcClientHandler>) this.connectedHandlers.clone();
					size = handlers.size();
				}
			} catch (InterruptedException e) {
				logger.error("Waiting for available node is interrupted! ", e);
				throw new RuntimeException("Can't connect any servers!", e);
			}
		}
		logger.info("当前计数器+1返回值："+String.valueOf(roundRobin.getAndAdd(1)));
		
		int index = (roundRobin.getAndAdd(1) + size) % size;
		logger.info("选择list中控制器：" + index);
		return handlers.get(index);
	}

	public void stop() {
		isRuning = false;
		for (int i = 0; i < connectedHandlers.size(); ++i) {
			RpcClientHandler connectedServerHandler = connectedHandlers.get(i);
			connectedServerHandler.close();
		}
		signalAvailableHandler();
		threadPoolExecutor.shutdown();
		eventLoopGroup.shutdownGracefully();
	}

}
