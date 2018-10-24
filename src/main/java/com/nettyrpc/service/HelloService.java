package com.nettyrpc.service;

import com.nettyrpc.requestModel.HelloRequestModel;
import com.nettyrpc.responseModel.HelloResponseModel;

public interface HelloService {

	
	HelloResponseModel hello(HelloRequestModel model);
	
	
}
