package com.nettyrpc.client;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nettyrpc.protocol.RpcRequest;
import com.nettyrpc.protocol.RpcResponse;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @Title: file_name
 * @Package package_name
 * @Description: TODO(SimpleChannelInboundHandler处理器适配模式)
 * @author xuliang
 * @date 2017年12月14日 下午7:24:39
 * @version V1.0
 */

public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {
	private static final Logger logger = LoggerFactory.getLogger(RpcClientHandler.class);

	private ConcurrentHashMap<String, RPCFuture> pendingRPC = new ConcurrentHashMap<>();

	private volatile Channel channel;
	private SocketAddress remotePeer;

	public Channel getChannel() {
		return channel;
	}

	public SocketAddress getRemotePeer() {
		return remotePeer;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		logger.info("激活客户端");
		this.remotePeer = this.channel.remoteAddress();
		logger.info(this.channel.isActive() + "   is   channelActive() isActive");
		logger.info(this.channel.isOpen() + "	  is   channelActive() isOpen");
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		super.channelRegistered(ctx);
		logger.info("=====================================获得通讯服务端通道channel");
		if (this.channel != null) {
			logger.info("channel  不为空");
		}
		this.channel = ctx.channel();
		logger.info(this.channel.isActive() + "   is   channelRegistered() isActive");
		logger.info(this.channel.isOpen() + "	  is   channelRegistered() isOpen");
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
		logger.info("=====================================channelRead0================");
		String requestId = response.getRequestId();
		RPCFuture rpcFuture = pendingRPC.get(requestId);
		logger.info("获得服务端响应信息,请求ID：" + response.getRequestId() + "====响应内容:" + response.getResult().toString());
		if (rpcFuture != null) {
			pendingRPC.remove(requestId);
			rpcFuture.done(response);
		}
	}

	/*
	 * exceptionCaught()事件处理方法是当出现Throwable对象才会被调用，
	 * 即当Netty由于IO错误或者处理器在处理事件时抛出的异常时。在大部分情况下
	 * ，捕获的异常应该被记录下来并且把关联的channel给关闭掉。然而这个方法的处理 方式会在遇到不同异常的情况下有不同的实现，比如你可能想在关闭连接
	 * 之前发送一个错误码的响应消息。
	 */

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.info("client caught exception", cause);
		ctx.close();
	}

	public void close() {
		logger.info("关闭通道close：");
		channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
	}

	public RPCFuture sendRequest(RpcRequest request) {
		logger.info("发起服务端请求：sendRequest" + request.toString());
		final CountDownLatch latch = new CountDownLatch(1);
		RPCFuture rpcFuture = new RPCFuture(request);
		pendingRPC.put(request.getRequestId(), rpcFuture);
		logger.info(" sendRequest de channel isActive "+channel.isActive());
		logger.info(" sendRequest de channel isOpen "+channel.isOpen());
		channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) {
				latch.countDown();
				logger.info("客户端收到服务端通知，已处理完成。");
			}
		});
		logger.info("客户端收到服务端通知");
		try {
			latch.await();
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}

		return rpcFuture;
	}
}
