package com.nd.common.hive.blood.graph;

import javax.swing.JFrame;
import org.jgrapht.Graph;
import org.jgrapht.ext.JGraphXAdapter;
import com.mxgraph.layout.mxParallelEdgeLayout;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.swing.mxGraphComponent;

public class GraphUI {

	/**
	 * 	展示血缘图
	 * @param graph
	 */
	public static <Vertex, Edge> void showCustomJGraph(Graph<Vertex, Edge> graph) {
		CustomJGraphXAdapter applet= new CustomJGraphXAdapter<Vertex, Edge>(graph);
		applet.init();
		JFrame frame = new JFrame();
		frame.getContentPane().add(applet);
		frame.setTitle("JGraphT Adapter to JGraphX Demo");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
	
	/**
	 * 	展示血缘图
	 * @param graph
	 */
	public static <Vertex, Edge> void show(Graph<Vertex, Edge> graph) {
		JGraphXAdapter<Vertex, Edge> graphx= new JGraphXAdapter<>(graph);
		mxGraphComponent graphComponent = new mxGraphComponent(graphx);
        JFrame frame = new JFrame();
        frame.getContentPane().add(graphComponent);
        new mxHierarchicalLayout(graphx).execute(graphx.getDefaultParent());
        new mxParallelEdgeLayout(graphx).execute(graphx.getDefaultParent());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(500, 500);
		frame.setVisible(true);
	}

	
}
