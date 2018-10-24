package com.nettyrpc.responseModel;

import java.io.Serializable;

import com.nettyrpc.protocol.RpcResponse;

public class HelloResponseModel extends RpcResponse {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String requestid;

	private String result;

	public String getRequestid() {
		return requestid;
	}

	public void setRequestid(String requestid) {
		this.requestid = requestid;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	@Override
	public String toString() {
		return "HelloResponseModel [requestid=" + requestid + ", result=" + result + "]";
	}

}
