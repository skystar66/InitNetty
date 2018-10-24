package com.nettyrpc.client;

import java.lang.reflect.Proxy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.nettyrpc.proxy.IAsyncObjectProxy;
import com.nettyrpc.proxy.ObjectProxy;
import com.nettyrpc.registry.ServerDiscover;

public class RpcClient {

	private String serverAddress;
	private ServerDiscover serviceDiscovery;
	private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16, 600L, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(65536));
	private static Logger logger = Logger.getLogger(RpcClient.class);
	
	public RpcClient(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public RpcClient(ServerDiscover serviceDiscovery) {
		this.serviceDiscovery = serviceDiscovery;
	}

	@SuppressWarnings("unchecked")
	public static <T> T create(Class<T> interfaceClass) {
		logger.info("创建对象代理接口");
		return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[] { interfaceClass },
				new ObjectProxy<T>(interfaceClass));
	}

	public static <T> IAsyncObjectProxy createAsync(Class<T> interfaceClass) {
		logger.info("创建异步操作");
		return new ObjectProxy<T>(interfaceClass);
	}

	public static void submit(Runnable task) {
		logger.info("RpcClient的submit添加线程方法");
		threadPoolExecutor.submit(task);
	}

	public void stop() {
		logger.info("关掉线程");
		threadPoolExecutor.shutdown();
		serviceDiscovery.stop();
		ConnectManages.getInstance().stop();
	}

}
