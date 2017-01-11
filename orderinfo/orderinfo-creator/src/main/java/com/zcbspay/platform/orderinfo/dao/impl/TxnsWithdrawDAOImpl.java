/* 
 * TxnsWithdrawDAOImpl.java  
 * 
 * version TODO
 *
 * 2016年11月14日 
 * 
 * Copyright (c) 2016,zlebank.All rights reserved.
 * 
 */
package com.zcbspay.platform.orderinfo.dao.impl;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.zcbspay.platform.orderinfo.common.dao.impl.HibernateBaseDAOImpl;
import com.zcbspay.platform.orderinfo.dao.TxnsWithdrawDAO;
import com.zcbspay.platform.orderinfo.dao.pojo.PojoTxnsWithdraw;

/**
 * Class Description
 *
 * @author guojia
 * @version
 * @date 2016年11月14日 下午4:11:03
 * @since 
 */
@Repository("txnsWithdrawDAO")
public class TxnsWithdrawDAOImpl extends HibernateBaseDAOImpl<PojoTxnsWithdraw> implements
		TxnsWithdrawDAO {

	/**
	 *
	 * @param txnsWithdraw
	 */
	@Override
	@Transactional(propagation=Propagation.REQUIRED,rollbackFor=Throwable.class)
	public void saveTxnsWithdraw(PojoTxnsWithdraw txnsWithdraw) {
		// TODO Auto-generated method stub
		saveEntity(txnsWithdraw);
	}

	

}
