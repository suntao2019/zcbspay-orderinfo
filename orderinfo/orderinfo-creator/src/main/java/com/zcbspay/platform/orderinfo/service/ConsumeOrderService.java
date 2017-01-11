/* 
 * ConsumeOrderService.java  
 * 
 * version TODO
 *
 * 2016年9月8日 
 * 
 * Copyright (c) 2016,zlebank.All rights reserved.
 * 
 */
package com.zcbspay.platform.orderinfo.service;

import com.zcbspay.platform.orderinfo.bean.OrderBean;
import com.zcbspay.platform.orderinfo.exception.CommonException;

/**
 * Class Description
 *
 * @author guojia
 * @version
 * @date 2016年9月8日 下午3:34:34
 * @since 
 */
@Deprecated
public interface ConsumeOrderService {

	/**
	 * 检查订单信息
	 * @param orderBean
	 * @throws CommonException
	 */
	public void checkOrderInfo(OrderBean orderBean) throws CommonException;
	
	/**
	 * 创建消费订单
	 * @param orderBean
	 * @return
	 */
	public String createConsumeOrder(OrderBean orderBean) throws CommonException;
}
