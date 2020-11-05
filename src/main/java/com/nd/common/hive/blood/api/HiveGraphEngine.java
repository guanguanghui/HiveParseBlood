package com.nd.common.hive.blood.api;

import java.util.List;
import com.nd.common.hive.blood.model.HiveField;
import com.nd.common.hive.blood.model.HiveTable;
import com.nd.common.hive.blood.model.SQLResult;
import org.jgrapht.Graph;


/**
 * 血缘接口
 * 方便与spring注入
 * @author nd
 */
public interface HiveGraphEngine {

	/**
	 * 根据sql获取解析结果
	 * @param hqls
	 * @return	hsql解析结果
	 * @throws Exception
	 */
	public List<SQLResult> parser(List<String> hqls) throws Exception;
	
	/**
	 * 获取当前sql的表血缘
	 * @param hqls hive sql 语句
	 * @return	表血缘
	 */
	public Graph getTableGraph(List<String> hqls) throws Exception;

	/**
	 * 根据血缘图获取指定表的属性字段
	 * @param hqls	hive sql 语句
	 * @param table	指定的表
	 * @return	属性字段列表
	 */
	public List<HiveField> getTableFields(List<String> hqls, HiveTable table) throws Exception;

	public Graph getTableFieldsGraphByTable(List<String> hqls, HiveTable table) throws Exception;


	public Graph getFieldGraph(List<String> hqls) throws Exception;

	/**
	 * 根据血缘图获取指定表的字段血缘
	 * @param hqls	hive sql 语句
	 * @param table	指定的表
	 */
	public Graph getFieldAcyclicGraphByTable(List<String> hqls, HiveTable table) throws Exception;

}
