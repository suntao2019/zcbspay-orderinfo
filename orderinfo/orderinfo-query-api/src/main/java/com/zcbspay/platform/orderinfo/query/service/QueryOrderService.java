/* 
 * QueryService.java  
 * 
 * version TODO
 *
 * 2016年10月17日 
 * 
 * Copyright (c) 2016,zlebank.All rights reserved.
 * 
 */
package com.zcbspay.platform.orderinfo.query.service;

import com.zcbspay.platform.orderinfo.exception.QueryOrderException;
import com.zcbspay.platform.orderinfo.query.bean.OrderResultBean;

/**
 * 交易订单查询接口
 *
 * @author guojia
 * @version
 * @date 2016年10月17日 上午10:03:21
 * @since 
 */
public interface QueryOrderService {

	/**
	 * 交易订单查询方法
	 * @param merchNo 商户号
	 * @param orderId 订单号
	 * @return 订单结果bean
	 */
	public OrderResultBean queryOrder(String merchNo,String orderId) throws QueryOrderException; 
	
	/**
	 *  交易订单查询方法
	 * @param tn 受理订单号
	 * @return
	 */
	public OrderResultBean queryOrderByTN(String tn) throws QueryOrderException ;
	
	/**
	 * 
	 * @param merchNo
	 * @param orderId
	 * @return
	 * @throws QueryOrderException
	 */
	public OrderResultBean queryInsteadPayOrder(String merchNo,String orderId)  throws QueryOrderException;
}
