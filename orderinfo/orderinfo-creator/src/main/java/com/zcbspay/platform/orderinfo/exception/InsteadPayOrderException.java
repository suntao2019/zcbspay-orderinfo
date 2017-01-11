/* 
 * InsteadPayOrderException.java  
 * 
 * version TODO
 *
 * 2016年10月20日 
 * 
 * Copyright (c) 2016,zlebank.All rights reserved.
 * 
 */
package com.zcbspay.platform.orderinfo.exception;

import java.util.ResourceBundle;

/**
 * Class Description
 *
 * @author guojia
 * @version
 * @date 2016年10月20日 上午10:36:29
 * @since
 */
public class InsteadPayOrderException extends Exception {
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -3598757186736680558L;
	private static final ResourceBundle RESOURCE = ResourceBundle.getBundle("customer_exception");
	private String code;
	private String message;

	public InsteadPayOrderException(String code, String message) {
		super();
		this.code = code;
		this.message = message;
	}
	
	public InsteadPayOrderException(String code) {
		super();
		this.code = code;
		this.message = RESOURCE.getString(code);
	}

	/**
	 * 
	 */
	public InsteadPayOrderException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public String getCode() {
		return code;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
