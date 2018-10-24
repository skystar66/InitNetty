package com.nettyrpc.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.nettyrpc.protocol.MarshallingCodeFactory;
import com.nettyrpc.protocol.RpcDecoder;
import com.nettyrpc.protocol.RpcEncoder;
import com.nettyrpc.protocol.RpcRequest;
import com.nettyrpc.protocol.RpcResponse;
import com.nettyrpc.registry.ServerRegistry;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.marshalling.MarshallingDecoder;
import io.netty.handler.timeout.ReadTimeoutHandler;

public class RpcServer implements ApplicationContextAware, InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

	private String serverAddress;
	private ServerRegistry serviceRegistry;

	private Map<String, Object> handlerMap = new HashMap<>();

	private static ThreadPoolExecutor threadPoolExecutor;

	public RpcServer(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public RpcServer(String serverAddress, ServerRegistry serviceRegistry) {
		this.serverAddress = serverAddress;
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		Map<String, Object> serviceBeanMap = ctx.getBeansWithAnnotation(RpcService.class);
		if (MapUtils.isNotEmpty(serviceBeanMap)) {
			for (Object serviceBean : serviceBeanMap.values()) {
				String interfaceName = serviceBean.getClass().getAnnotation(RpcService.class).value().getName();
				handlerMap.put(interfaceName, serviceBean);
			}
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup(); // 用于处理器端接受客户端连接的
		EventLoopGroup workerGroup = new NioEventLoopGroup(); // 一个是进行网络通信的（网络读写）
		try {
			ServerBootstrap bootstrap = new ServerBootstrap(); // 创建服务辅助工具类，用于服务器通道的配置
			bootstrap.group(bossGroup, workerGroup) // 绑定两个线程组
					.channel(NioServerSocketChannel.class) // 指定NIO模式
					.option(ChannelOption.SO_BACKLOG, 1024) // 设置TCP缓冲区
					.option(ChannelOption.SO_SNDBUF, 32 * 1024) // 设置发送缓冲区大小
					.option(ChannelOption.SO_RCVBUF, 32 * 1024) // 设置接受缓冲区大小
//					.option(ChannelOption.SO_KEEPALIVE, true) // 保持连接
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel channel) throws Exception {
							// 配置具体数据接收处理及处理器
							channel.pipeline()
									.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0)) // 解码器
									.addLast(new RpcDecoder(RpcRequest.class)) // 解码器
									.addLast(new RpcEncoder(RpcResponse.class))// 编码器
//									.addLast(MarshallingCodeFactory.buildMarshallingDecoder())//解码器
//									.addLast(MarshallingCodeFactory.buildMarshallingEncoder())//编码器Ïßß
									.addLast(new ReadTimeoutHandler(5)) //如何在超时（及服务器和客户端没有任何通信）关闭通道，关闭通道后 我们又如何建立连接？
									.addLast(new RpcHandler(handlerMap));
							// Unpooled
						}
					});// 保持连接

			String[] array = serverAddress.split(":");
			String host = array[0];
			int port = Integer.parseInt(array[1]);

			ChannelFuture future = bootstrap.bind(host, port).sync();
			logger.info("Server started on port {}", port);

			if (serviceRegistry != null) {
				serviceRegistry.register(serverAddress);
			}

			future.channel().closeFuture().sync(); // 做一个阻塞 监听关闭 同步销毁
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}

	public static void submit(Runnable task) {
		if (threadPoolExecutor == null) {
			synchronized (RpcServer.class) {
				if (threadPoolExecutor == null) {
					threadPoolExecutor = new ThreadPoolExecutor(16, 16, 600L, TimeUnit.SECONDS,
							new ArrayBlockingQueue<Runnable>(65536));
				}
			}
		}
		threadPoolExecutor.submit(task);
	}

}
