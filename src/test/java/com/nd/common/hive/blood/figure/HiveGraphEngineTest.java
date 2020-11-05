package com.nd.common.hive.blood.figure;

import java.util.Arrays;
import com.nd.common.hive.blood.api.HiveGraphEngine;
import com.nd.common.hive.blood.api.HiveGraphEngineImpl;
import com.nd.common.hive.blood.graph.GraphUI;
import com.nd.common.hive.blood.model.HiveTable;
import junit.framework.TestCase;
import org.jgrapht.Graph;

public class HiveGraphEngineTest extends TestCase {

	static String hqls [] = {
			"create table temp.b1(id string, name string) row format delimited fields terminated by ',';",
			"create table temp.b2(id string, age int) row format delimited fields terminated by ',';",
			"create table temp.c1(id string, name string) row format delimited fields terminated by ',';",
			"create table temp.c2(id string, age int) row format delimited fields terminated by ',';" ,
			"create table temp.d1(id string, name string, age int) row format delimited fields terminated by ',';",
			"insert into table temp.b1 select id, name from temp.a1;",
			"insert into table temp.b2 select id, age from temp.a1;",
			"insert overwrite table temp.c1 select id, name from temp.b1;",
			"insert overwrite table temp.c2 select id, age from temp.b2;" ,
			"insert overwrite table temp.d1 select t1.id, t1.name, t2.age from temp.c1 t1 join temp.c2 t2 on t1.id = t2.id;",
			"insert overwrite table temp.e1 select id,name,age from (select t1.id, t1.name, t2.age from temp.b1 t1 join temp.b2 t2 on t1.id = t2.id union all select id,name,age from temp.d1) t;",
			"create table temp.f1 as select id,name,age from (select t1.id, t1.name, t2.age from temp.b1 t1 join temp.b2 t2 on t1.id = t2.id union all select id,name,age from temp.d1) t;"
	};
	
	private HiveGraphEngine bloodEngine = new HiveGraphEngineImpl();

	/**
	 * 获取当前sql的表血缘
	 */
	public void testGetTableGraph() throws Exception{
		Graph graph = bloodEngine.getTableGraph(Arrays.asList(hqls));
		GraphUI.show(graph);
		System.in.read();
	}

	/**
	 * 根据血缘图获取指定表的属性字段
	 */
	public void testGetTableFields() throws Exception{
		Graph graph = bloodEngine.getTableFieldsGraphByTable(Arrays.asList(hqls), new HiveTable("temp", "d1"));
		GraphUI.show(graph);
		System.in.read();
	}


	/**
	 * 根据血缘图获取指定表的字段血缘
	 */
	public void testGetFieldGraph() throws Exception{
		Graph graph = bloodEngine.getFieldGraph(Arrays.asList(hqls));
		GraphUI.show(graph);
		System.in.read();
	}


	/**
	 * 根据血缘图获取指定表的字段血缘
	 */
	public void testGetFieldAcyclicGraphByTable() throws Exception{
		Graph graph = bloodEngine.getFieldAcyclicGraphByTable(Arrays.asList(hqls), new HiveTable("temp", "f1"));
		GraphUI.show(graph);
		System.in.read();
	}
	
}
