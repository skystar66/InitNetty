package com.nettyrpc.protocol;

import org.apache.log4j.Logger;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;

import io.netty.handler.codec.marshalling.DefaultMarshallerProvider;
import io.netty.handler.codec.marshalling.DefaultUnmarshallerProvider;
import io.netty.handler.codec.marshalling.MarshallerProvider;
import io.netty.handler.codec.marshalling.MarshallingDecoder;
import io.netty.handler.codec.marshalling.MarshallingEncoder;
import io.netty.handler.codec.marshalling.UnmarshallerProvider;


/**
 * @Title: MarshallingCodeFactory
 * @Package MarshallingCodeFactory
 * @Description: TODO(netty通信的编码和转码)
 * @author xuliang
 * @date 2018年1月9日 下午7:12:44
 * @version V1.0
 */
public final class MarshallingCodeFactory {

	private static Logger logger = Logger.getLogger(MarshallingCodeFactory.class);
	
	/**
	 * 创解码器
	 */

	public static MarshallingDecoder buildMarshallingDecoder() {
		logger.info("进入解码器========================");
		// 首先通过Marshalling的工具类的精通方法获取Marshalling实例对象 参数Java实例对象序列化实例标识
		final MarshallerFactory marshallerFactory = Marshalling.getProvidedMarshallerFactory("serial");
		// 创建MarshallingConfiguration 独享，配置版本号
		MarshallingConfiguration config = new MarshallingConfiguration();
		config.setVersion(5);
		// 根据MarshallerFactory和MarshallingConfiguration创建provider对象
		UnmarshallerProvider provide = new DefaultUnmarshallerProvider(marshallerFactory, config);
		// 构建netty的marshalling对象，两个参数分别为provide和消息序列化的最大长度
		MarshallingDecoder decode = new MarshallingDecoder(provide, 1024 * 1024);
		return decode;
	}

	/**
	 * 创编码器
	 */

	public static MarshallingEncoder buildMarshallingEncoder() {
		logger.info("进入编码器========================");
		// 首先通过Marshalling的工具类的精通方法获取Marshalling实例对象 参数Java实例对象序列化实例标识
		final MarshallerFactory marshallerFactory = Marshalling.getProvidedMarshallerFactory("serial");
		// 创建MarshallingConfiguration 独享，配置版本号
		MarshallingConfiguration config = new MarshallingConfiguration();
		config.setVersion(5);
		// 根据MarshallerFactory和MarshallingConfiguration创建provider对象
		MarshallerProvider provide = new DefaultMarshallerProvider(marshallerFactory, config);
		// 构建netty的marshalling对象，两个参数分别为provide和消息序列化的最大长度
		MarshallingEncoder encode = new MarshallingEncoder(provide);
		return encode;
	}

}
