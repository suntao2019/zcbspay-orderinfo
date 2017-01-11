/* 
 * WithdrawOrderService.java  
 * 
 * version TODO
 *
 * 2016年11月14日 
 * 
 * Copyright (c) 2016,zlebank.All rights reserved.
 * 
 */
package com.zcbspay.platform.orderinfo.service;

import com.zcbspay.platform.orderinfo.bean.WithdrawBean;
import com.zcbspay.platform.orderinfo.exception.CommonException;
import com.zcbspay.platform.orderinfo.exception.WithdrawOrderException;

/**
 * 提现订单service
 *
 * @author guojia
 * @version
 * @date 2016年11月14日 下午3:26:08
 * @since 
 */
@Deprecated
public interface WithdrawOrderService {

	/**
	 * 创建个人用户提现订单
	 * @param withdrawBean 提现订单bean 
	 * @return 受理订单号
	 * @throws WithdrawOrderException
	 * @throws CommonException
	 */
	public String createIndividualWithdrawOrder(WithdrawBean withdrawBean) throws WithdrawOrderException,CommonException;
	
	
}
