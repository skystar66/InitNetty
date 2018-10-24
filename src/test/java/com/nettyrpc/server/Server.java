package com.nettyrpc.server;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Server {

	@Test
	public void connecServer() {
		new ClassPathXmlApplicationContext("server-spring.xml");
		System.out.println("服务端开启成功");
	}

}
