/* 
 * OrderException.java  
 * 
 * version TODO
 *
 * 2016年10月11日 
 * 
 * Copyright (c) 2016,zlebank.All rights reserved.
 * 
 */
package com.zcbspay.platform.orderinfo.exception;


/**
 * 统一支付服务订单异常
 *
 * @author guojia
 * @version
 * @date 2016年10月11日 下午4:29:09
 * @since 
 */
public class QueryOrderException extends AbstractDescException{

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -5825227604243819693L;
	private String code;
	public QueryOrderException(String code,Object ... para ) {
        this.params = para;
        this.code = code;
    }
	
	public QueryOrderException(String code) {
        this.code = code;
    }
	
    /**
     *
     * @return
     */
    @Override
    public String getCode() {
        return code;
    }
	/**
	 * 
	 */
	public QueryOrderException() {
		super();
		// TODO Auto-generated constructor stub
	}
    
}
