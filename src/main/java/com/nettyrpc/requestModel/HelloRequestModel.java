package com.nettyrpc.requestModel;

import com.nettyrpc.protocol.RpcRequest;

public class HelloRequestModel extends RpcRequest {

	public HelloRequestModel(String name, String age) {
		super();
		this.name = name;
		this.age = age;
	}

	public HelloRequestModel() {
		super();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String name;

	private String age;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAge() {
		return age;
	}

	public void setAge(String age) {
		this.age = age;
	}

	@Override
	public String toString() {
		return "HelloRequestModel [name=" + name + ", age=" + age + "]";
	}

}
