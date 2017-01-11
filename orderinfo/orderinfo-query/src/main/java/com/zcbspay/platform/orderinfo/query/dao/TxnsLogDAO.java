/* 
 * TxnsLogDAO.java  
 * 
 * version TODO
 *
 * 2016年9月13日 
 * 
 * Copyright (c) 2016,zlebank.All rights reserved.
 * 
 */
package com.zcbspay.platform.orderinfo.query.dao;

import com.zcbspay.platform.orderinfo.common.dao.BaseDAO;
import com.zcbspay.platform.orderinfo.query.pojo.PojoTxnsLog;

/**
 * Class Description
 *
 * @author guojia
 * @version
 * @date 2016年9月13日 下午5:31:41
 * @since 
 */
public interface TxnsLogDAO extends BaseDAO<PojoTxnsLog>{

	
	
	/**
	 * 根据交易流水号获取交易流水数据
	 * @param txnseqno 交易流水号
	 * @return 交易流水数据pojo
	 */
	public PojoTxnsLog getTxnsLogByTxnseqno(String txnseqno);
	
	
	
	
	
}
