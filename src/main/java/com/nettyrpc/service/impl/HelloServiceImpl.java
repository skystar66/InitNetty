package com.nettyrpc.service.impl;

import com.nettyrpc.requestModel.HelloRequestModel;
import com.nettyrpc.responseModel.HelloResponseModel;
import com.nettyrpc.server.RpcService;
import com.nettyrpc.service.HelloService;

@RpcService(HelloService.class)
public class HelloServiceImpl implements HelloService {

	@Override
	public HelloResponseModel hello(HelloRequestModel request) {
		// TODO Auto-generated method stub
		HelloResponseModel response = new HelloResponseModel();
		response.setResult("您好：" + request.getName() + "，我今年" + request.getAge());
		return response;
	}

}
