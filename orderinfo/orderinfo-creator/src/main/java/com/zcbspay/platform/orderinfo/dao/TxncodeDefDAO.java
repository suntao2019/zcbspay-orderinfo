/* 
 * TxncodeDefDAO.java  
 * 
 * version TODO
 *
 * 2016年9月12日 
 * 
 * Copyright (c) 2016,zlebank.All rights reserved.
 * 
 */
package com.zcbspay.platform.orderinfo.dao;

import com.zcbspay.platform.orderinfo.common.dao.BaseDAO;
import com.zcbspay.platform.orderinfo.dao.pojo.PojoTxncodeDef;

/**
 * Class Description
 *
 * @author guojia
 * @version
 * @date 2016年9月12日 上午11:50:27
 * @since 
 */
public interface TxncodeDefDAO extends BaseDAO<PojoTxncodeDef>{

	/**
	 * 获取内部业务代码实体类
	 * @param txntype
	 * @param txnsubtype
	 * @param biztype
	 * @return
	 */
	public PojoTxncodeDef getBusiCode(String txntype,String txnsubtype,String biztype);
}
