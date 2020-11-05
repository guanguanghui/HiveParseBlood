package com.nd.common.hive.blood.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.nd.common.hive.blood.parse.HiveSqlBloodFigureParser;
import com.nd.common.hive.blood.utils.HqlUtil;
import com.nd.common.hive.blood.utils.SplitUtil;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import com.nd.common.hive.blood.model.ColLine;
import com.nd.common.hive.blood.model.FieldVertex;
import com.nd.common.hive.blood.model.HiveField;
import com.nd.common.hive.blood.model.HiveTable;
import com.nd.common.hive.blood.model.SQLResult;
import com.nd.common.hive.blood.model.TableVertex;

public class HiveGraphEngineImpl implements HiveGraphEngine {
	
	@Override
	public List<SQLResult> parser(List<String> hqls) throws Exception {
		return new HiveSqlBloodFigureParser().parse(HqlUtil.ListToString(hqls));
	}


	@Override
	public Graph getTableGraph(List<String> hqls) throws Exception {
		//解析sql
		List<SQLResult> results = this.parser(hqls);
		//构建图
		DefaultDirectedGraph<TableVertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
		for(SQLResult result : results){
			//表节点信息
			for(String bd_table : result.getInputTables()) {
				TableVertex vertex = SplitUtil.splitDbTable(bd_table);
				if(vertex != null) {
					graph.addVertex(vertex);
				}
			}
			for(String bd_table : result.getOutputTables()) {
				TableVertex vertex = SplitUtil.splitDbTable(bd_table);
				if(vertex != null) {
					graph.addVertex(vertex);
				}
			}
			for(String start : result.getInputTables()) {
				TableVertex startVertex = SplitUtil.splitDbTable(start);
				if(startVertex != null) {
					for(String end : result.getOutputTables()) {
						TableVertex endVertex = SplitUtil.splitDbTable(end);
						if(endVertex != null) {
							graph.addEdge(startVertex, endVertex, new DefaultEdge());
						}
					}
				}
			}
		}
		return graph;
	}

	@Override
	public List<HiveField> getTableFields(List<String> hqls, HiveTable table) throws Exception {
		List<HiveField> fields = new ArrayList<>();
		//解析sql
		List<SQLResult> results = this.parser(hqls);
		for(SQLResult result : results){
			for(ColLine colLine : result.getColLineList()) {
				//处理来源字段
				Set<String> fromNameSet = colLine.getFromNameSet();
				for(String fromName : fromNameSet) {
					String temp[] = fromName.split("\\.");
					if(temp.length == 3 && temp[0].equals(table.getDbName()) && temp[1].equals(table.getTableName())) {
						HiveField field = new HiveField(table.getDbName(), table.getTableName(), temp[2]);
						if(!fields.contains(field)) {
							fields.add(field);
						}
					}
				}
				//处理目标字段
				String temp[] =colLine.getToTable().split("\\.");
				if(temp.length == 2 && temp[0].equals(table.getDbName()) && temp[1].equals(table.getTableName())) {
					HiveField field = new HiveField(table.getDbName(), table.getTableName(), colLine.getToNameParse());
					if(!fields.contains(field)) {
						fields.add(field);
					}
				}
			}
		}
		//返回
		return fields;
	}

	@Override
	public Graph getTableFieldsGraphByTable(List<String> hqls, HiveTable table) throws Exception {
		List<HiveField> fields = getTableFields(hqls, table);
		Graph<Object, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
		//关联表与字段
		for(HiveField field : fields) {
			TableVertex tableVertex = new TableVertex(field.getDbName(), field.getTableName());
			FieldVertex fieldVertex = new FieldVertex(field.getDbName(), field.getTableName(), field.getFieldName());
			graph.addVertex(tableVertex);
			graph.addVertex(fieldVertex);
			graph.addEdge(
					fieldVertex,
					tableVertex,
					new DefaultEdge());
		}
		return graph;
	}



	@Override
	public Graph getFieldGraph(List<String> hqls) throws Exception {
		//解析sql
		List<SQLResult> results = this.parser(hqls);
		//获取字段血缘图
		DefaultDirectedGraph<FieldVertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
		for(SQLResult result : results){
			for(ColLine colLine : result.getColLineList()) {
				//处理目标字段
				FieldVertex targetFieldVertex = null;
				String tempTarget[] =colLine.getToTable().split("\\.");
				if(tempTarget.length == 2) {
					//目标字段节点
					targetFieldVertex = new FieldVertex(tempTarget[0], tempTarget[1], colLine.getToNameParse());
					graph.addVertex(targetFieldVertex);
				}
				//处理来源字段
				Set<String> fromNameSet = colLine.getFromNameSet();
				for(String fromName : fromNameSet) {
					String tempFrom[] = fromName.split("\\.");
					if(tempFrom.length == 3) {
						//来源字段节点
						FieldVertex soruceFieldVertex = new FieldVertex(tempFrom[0], tempFrom[1], tempFrom[2]);
						graph.addVertex(soruceFieldVertex);
						//添加关系
						graph.addEdge(soruceFieldVertex, targetFieldVertex, new DefaultEdge());
					}
				}
			}
		}
		return graph;
	}

	@Override
	public Graph getFieldAcyclicGraphByTable(List<String> hqls, HiveTable table) throws Exception {
		DefaultDirectedGraph<FieldVertex, DefaultEdge> sourceGraph = (DefaultDirectedGraph<FieldVertex, DefaultEdge>) getFieldGraph(hqls);

		// 有向图转为有向无环图才能遍历父节点
		DirectedAcyclicGraph<FieldVertex, DefaultEdge> dag =
				new DirectedAcyclicGraph<>(DefaultEdge.class);
		for (DefaultEdge edge : sourceGraph.edgeSet()) {
			FieldVertex edgeSource = sourceGraph.getEdgeSource(edge);
			FieldVertex edgeTarget = sourceGraph.getEdgeTarget(edge);
			try {
				dag.addVertex(edgeSource);
				dag.addVertex(edgeTarget);
				dag.addEdge(edgeSource, edgeTarget);
			} catch (IllegalArgumentException e) {
				// okay, it did't add that edge
				// 有成环的异常
			}
		}

		List<HiveField> fields = this.getTableFields(hqls, table);

		DirectedAcyclicGraph<Object, DefaultEdge> targetDag =
				new DirectedAcyclicGraph<>(DefaultEdge.class);
		TableVertex tableVertex = new TableVertex(table.getDbName(), table.getTableName());
		targetDag.addVertex(tableVertex);
		for(HiveField field : fields) {
			FieldVertex fieldVertex = new FieldVertex(field.getDbName(), field.getTableName(), field.getFieldName());
			if(dag.containsVertex(fieldVertex)) {
				ancestorsFieldVertexIteror(fieldVertex, dag, targetDag);
			}
			targetDag.addEdge(fieldVertex, tableVertex);
		}

		return targetDag;
	}

	protected void ancestorsFieldVertexIteror(FieldVertex fieldVertex, DirectedAcyclicGraph<FieldVertex, DefaultEdge> sourceDag, DirectedAcyclicGraph<Object, DefaultEdge> targetDag){
		Set<FieldVertex> ancestors = sourceDag.getAncestors(fieldVertex);
		targetDag.addVertex(fieldVertex);
		for(FieldVertex e:ancestors){
			if(sourceDag.containsEdge(e, fieldVertex)){
				targetDag.addVertex(e);
				targetDag.addEdge(e, fieldVertex);
				ancestorsFieldVertexIteror(e, sourceDag, targetDag);
			}
		}
	}

}
