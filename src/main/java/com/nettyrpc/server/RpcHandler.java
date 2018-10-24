package com.nettyrpc.server;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nettyrpc.protocol.RpcRequest;
import com.nettyrpc.protocol.RpcResponse;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;

public class RpcHandler extends SimpleChannelInboundHandler<RpcRequest> {
	private static final Logger logger = LoggerFactory.getLogger(RpcHandler.class);

	private final Map<String, Object> handlerMap;

	public RpcHandler(Map<String, Object> handlerMap) {
		this.handlerMap = handlerMap;
	}

	@Override
	public void channelRead0(final ChannelHandlerContext ctx, final RpcRequest request) throws Exception {
		logger.info("进入服务端。。。。");
		RpcServer.submit(new Runnable() {
			@Override
			public void run() {
				logger.info("服务端收到 request " + request.getRequestId() + "请求参数信息：" + request.toString());
				final RpcResponse response = new RpcResponse();
				response.setRequestId(request.getRequestId());
				try {
					Object result = handle(request);
					response.setResult(result);
				} catch (Throwable t) {
					response.setError(t.toString());
					logger.error("服务端收到请求，但发生错误：", t);
				}
				ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture channelFuture) throws Exception {
						logger.info("成功处理此请求：Send response for request " + request.getRequestId() + "返回结果："
								+ response.toString());
					}
				});
			}
		});
	}

	private Object handle(RpcRequest request) throws Throwable {
		String className = request.getClassName();
		Object serviceBean = handlerMap.get(className);

		Class<?> serviceClass = serviceBean.getClass();
		String methodName = request.getMethodName();
		Class<?>[] parameterTypes = request.getParameterTypes();
		Object[] parameters = request.getParameters();

		logger.debug(serviceClass.getName());
		logger.debug(methodName);
		for (int i = 0; i < parameterTypes.length; ++i) {
			logger.info(parameterTypes[i].getName());
		}
		for (int i = 0; i < parameters.length; ++i) {
			logger.info(parameters[i].toString());
		}

		// JDK reflect
		/*
		 * Method method = serviceClass.getMethod(methodName, parameterTypes);
		 * method.setAccessible(true); return method.invoke(serviceBean, parameters);
		 */

		// Cglib reflect
		FastClass serviceFastClass = FastClass.create(serviceClass);
		FastMethod serviceFastMethod = serviceFastClass.getMethod(methodName, parameterTypes);
		return serviceFastMethod.invoke(serviceBean, parameters);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		logger.info("server caught exception", cause);
//		logger.info("关闭通道。");
		ctx.close();
	}
}
