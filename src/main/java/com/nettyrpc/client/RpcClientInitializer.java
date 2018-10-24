package com.nettyrpc.client;

import com.nettyrpc.protocol.MarshallingCodeFactory;
import com.nettyrpc.protocol.RpcDecoder;
import com.nettyrpc.protocol.RpcEncoder;
import com.nettyrpc.protocol.RpcRequest;
import com.nettyrpc.protocol.RpcResponse;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.ReadTimeoutHandler;

public class RpcClientInitializer extends ChannelInitializer<SocketChannel>{
	  @Override
	    protected void initChannel(SocketChannel socketChannel) throws Exception {
	        ChannelPipeline cp = socketChannel.pipeline();
	        cp.addLast(new RpcEncoder(RpcRequest.class));//编码器
	        cp.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0)); //解码器
	        cp.addLast(new RpcDecoder(RpcResponse.class)); //解码器
//	        cp.addLast(MarshallingCodeFactory.buildMarshallingDecoder());//解码器
//			cp.addLast(MarshallingCodeFactory.buildMarshallingEncoder());//编码器Ïßß
	        //超时handler（当客户端与服务端没哟任何响应链接时，则关闭响应通道，主要是维拉减少服务端资源占用）
	        cp.addLast(new ReadTimeoutHandler(5));
	        cp.addLast(new RpcClientHandler()); //具体处理的handler
	    }
}
