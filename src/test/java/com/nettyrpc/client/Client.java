package com.nettyrpc.client;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.nettyrpc.requestModel.HelloRequestModel;
import com.nettyrpc.responseModel.HelloResponseModel;
import com.nettyrpc.service.HelloService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:client-spring.xml")
public class Client {

	@Autowired
	private RpcClient rpcClient;

	@Test
	public void helloTest1() throws InterruptedException {
		long start = System.currentTimeMillis();
		HelloService helloService = rpcClient.create(HelloService.class);
		// HelloRequestModel request = new HelloRequestModel();
		// request.setName("jack");
		// request.setAge("11");
		// HelloResponseModel result = helloService.hello(request);
		// System.out.println(result.toString());
		// HelloRequestModel request1 = new HelloRequestModel();
		// request1.setName("mac");
		// request1.setAge("12");
		// HelloResponseModel result1 = helloService.hello(request1);
		// System.out.println(result1.toString());
		// HelloRequestModel request2 = new HelloRequestModel();
		// request2.setName("mary");
		// request2.setAge("13");
		// HelloResponseModel result2 = helloService.hello(request2);
		// System.out.println(result2.toString());
		// HelloRequestModel request3 = new HelloRequestModel();
		// request3.setName("lisa");
		// request3.setAge("14");
		// HelloResponseModel result3 = helloService.hello(request3);
		// System.out.println(result3.toString());
		// HelloRequestModel request4 = new HelloRequestModel();
		// request4.setName("jun");
		// request4.setAge("15");
		// HelloResponseModel result4 = helloService.hello(request4);
		// System.out.println(result4.toString());

		for (int i = 0; i < 5; i++) {
			HelloRequestModel request = new HelloRequestModel();
			request.setName("a" + i);
			request.setAge("" + i);
			helloService.hello(request);
			// 睡眠4秒再次发起请求
			Thread.sleep(5000);//休眠一分钟
		}
		Thread.sleep(1000);
		HelloRequestModel request = new HelloRequestModel();
		request.setName("b" );
		request.setAge("9");
		helloService.hello(request);
//
//		long end = System.currentTimeMillis();
//
//		System.out.println("共耗时" + (end - start) + "ms");

	}

}
