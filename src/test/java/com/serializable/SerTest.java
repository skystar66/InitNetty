package com.serializable;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import com.nettyrpc.requestModel.HelloRequestModel;

//序列化性能测试

public class SerTest {

	public static void main(String[] args) throws IOException, ClassNotFoundException {

		long start = System.currentTimeMillis();
		setSeriali();
		System.out.println("写文件共花费："+(System.currentTimeMillis()-start)+"ms");
		
		start = System.currentTimeMillis();
		getSeriali();
		System.out.println("读文件共花费："+(System.currentTimeMillis()-start)+"ms");
		
	}

	// 写

	public static void setSeriali() throws IOException {

		FileOutputStream fo = new FileOutputStream("/Users/xuliang/Documents/ser.txt");
		ObjectOutputStream so = new ObjectOutputStream(fo);
		for (int i = 0; i < 100000; i++) {
			so.writeObject(new HelloRequestModel("xuliang" + i, "" + i));
		}
		so.flush();
		so.close();

	}

	// 读

	public static void getSeriali() throws IOException, ClassNotFoundException {

		FileInputStream fi = new FileInputStream("/Users/xuliang/Documents/ser.txt");

		ObjectInputStream oi = new ObjectInputStream(fi);

		HelloRequestModel request = null;

		while ((request = (HelloRequestModel) oi.readObject()) != null) {

		}

		fi.close();
		oi.close();

	}

}
