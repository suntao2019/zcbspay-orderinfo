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
package com.zcbspay.platform.orderinfo.dao;

import com.zcbspay.platform.orderinfo.bean.CardBin;
import com.zcbspay.platform.orderinfo.common.dao.BaseDAO;
import com.zcbspay.platform.orderinfo.dao.pojo.PojoTxnsLog;

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
	 * 保存交易流水
	 * @param txnsLog
	 */
	public void saveTxnsLog(PojoTxnsLog txnsLog);
	/**
	 * 查询卡bin信息
	 * @param cardNo
	 * @return
	 */
	public CardBin getCard(String cardNo) ;
	
	/**
	 * 通过交易序列号获取交易流水
	 * @param txnseqno 交易序列号
	 * @return 交易流水pojo
	 */
	public PojoTxnsLog getTxnsLogByTxnseqno(String txnseqno);
	
	/**
	 * 更新交易手续费
	 * @param txnseqno
	 * @param fee
	 */
	public void updateTradeFee(String txnseqno,long fee);
}
