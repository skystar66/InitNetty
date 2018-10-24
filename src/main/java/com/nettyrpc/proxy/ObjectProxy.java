package com.nettyrpc.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nettyrpc.client.ConnectManages;
import com.nettyrpc.client.RPCFuture;
import com.nettyrpc.client.RpcClientHandler;
import com.nettyrpc.protocol.RpcRequest;

public class ObjectProxy<T> implements InvocationHandler, IAsyncObjectProxy {
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectProxy.class);
	private Class<T> clazz;

	public ObjectProxy(Class<T> clazz) {
		this.clazz = clazz;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		LOGGER.info("进入代理方法:invoke");
		if (Object.class == method.getDeclaringClass()) {
			String name = method.getName();
			if ("equals".equals(name)) {
				return proxy == args[0];
			} else if ("hashCode".equals(name)) {
				return System.identityHashCode(proxy);
			} else if ("toString".equals(name)) {
				return proxy.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(proxy))
						+ ", with InvocationHandler " + this;
			} else {
				throw new IllegalStateException(String.valueOf(method));
			}
		}

		RpcRequest request = new RpcRequest();
		request.setRequestId(UUID.randomUUID().toString());
		request.setClassName(method.getDeclaringClass().getName());
		request.setMethodName(method.getName());
		request.setParameterTypes(method.getParameterTypes());
		request.setParameters(args);
		// Debug
		LOGGER.info("请求类名称:" + method.getDeclaringClass().getName());
		LOGGER.info("请求方法名:" + method.getName());
		for (int i = 0; i < method.getParameterTypes().length; ++i) {
			LOGGER.info("参数类型:" + method.getParameterTypes()[i].getName());
		}
		for (int i = 0; i < args.length; ++i) {
			LOGGER.info("请求参数：" + args[i].toString());
		}
		RpcClientHandler handler = ConnectManages.getInstance().chooseHandler();
		RPCFuture rpcFuture = handler.sendRequest(request);
		ConnectManages.getInstance().deleteNodeAndCloseServer();
		return rpcFuture.get();
	}

	@Override
	public RPCFuture call(String funcName, Object... args) {
		LOGGER.info("请求方法：call" );
		RpcClientHandler handler = ConnectManages.getInstance().chooseHandler();
		RpcRequest request = createRequest(this.clazz.getName(), funcName, args);
		RPCFuture rpcFuture = handler.sendRequest(request);
		return rpcFuture;
	}

	private RpcRequest createRequest(String className, String methodName, Object[] args) {
		LOGGER.info("请求方法：createRequest" );
		RpcRequest request = new RpcRequest();
		request.setRequestId(UUID.randomUUID().toString());
		request.setClassName(className);
		request.setMethodName(methodName);
		request.setParameters(args);

		Class[] parameterTypes = new Class[args.length];
		// Get the right class type
		for (int i = 0; i < args.length; i++) {
			parameterTypes[i] = getClassType(args[i]);
		}
		request.setParameterTypes(parameterTypes);
		// Method[] methods = clazz.getDeclaredMethods();
		// for (int i = 0; i < methods.length; ++i) {
		// // Bug: if there are 2 methods have the same name
		// if (methods[i].getName().equals(methodName)) {
		// parameterTypes = methods[i].getParameterTypes();
		// request.setParameterTypes(parameterTypes); // get parameter types
		// break;
		// }
		// }

		LOGGER.debug(className);
		LOGGER.debug(methodName);
		for (int i = 0; i < parameterTypes.length; ++i) {
			LOGGER.debug(parameterTypes[i].getName());
		}
		for (int i = 0; i < args.length; ++i) {
			LOGGER.debug(args[i].toString());
		}

		return request;
	}

	private Class<?> getClassType(Object obj) {
		Class<?> classType = obj.getClass();
		String typeName = classType.getName();
		switch (typeName) {
		case "java.lang.Integer":
			return Integer.TYPE;
		case "java.lang.Long":
			return Long.TYPE;
		case "java.lang.Float":
			return Float.TYPE;
		case "java.lang.Double":
			return Double.TYPE;
		case "java.lang.Character":
			return Character.TYPE;
		case "java.lang.Boolean":
			return Boolean.TYPE;
		case "java.lang.Short":
			return Short.TYPE;
		case "java.lang.Byte":
			return Byte.TYPE;
		}

		return classType;
	}
}
