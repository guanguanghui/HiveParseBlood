package com.nd.common.hive.blood.graph;
import com.mxgraph.layout.*;
import com.mxgraph.swing.*;
import org.jgrapht.Graph;
import org.jgrapht.ext.*;

import javax.swing.*;
import java.awt.*;

/**
 * A demo applet that shows how to use JGraphX to visualize JGraphT graphs. Applet based on
 * JGraphAdapterDemo.
 *
 */
public class CustomJGraphXAdapter <Vertex, Edge>
        extends
        JApplet
{
    private static final long serialVersionUID = 2202072534703043194L;

    private static final Dimension DEFAULT_SIZE = new Dimension(800*2, 600*2);

    private JGraphXAdapter<Vertex, Edge> jgxAdapter;

    private Graph<Vertex, Edge> defaultDirectedGraph;


    public CustomJGraphXAdapter(Graph<Vertex, Edge> defaultDirectedGraph) throws HeadlessException {
        this.defaultDirectedGraph = defaultDirectedGraph;
    }

    @Override
    public void init()
    {

        // create a visualization using JGraph, via an adapter
        jgxAdapter = new JGraphXAdapter<Vertex, Edge>(defaultDirectedGraph);

        setPreferredSize(DEFAULT_SIZE);
        mxGraphComponent component = new mxGraphComponent(jgxAdapter);
        component.setConnectable(false);
        component.getGraph().setAllowDanglingEdges(false);
        getContentPane().add(component);
        resize(DEFAULT_SIZE);


        // positioning via jgraphx layouts
        mxCircleLayout layout = new mxCircleLayout(jgxAdapter);

        // center the circle
        int radius = 300;
        layout.setX0((DEFAULT_SIZE.width )/2 -250);
        layout.setY0((DEFAULT_SIZE.height )/2 -310);
        layout.setRadius(radius);
        layout.setMoveCircle(true);

        layout.execute(jgxAdapter.getDefaultParent());
        // that's all there is to it!...
    }
}