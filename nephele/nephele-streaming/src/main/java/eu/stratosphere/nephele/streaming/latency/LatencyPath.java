package eu.stratosphere.nephele.streaming.latency;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import eu.stratosphere.nephele.managementgraph.ManagementEdge;
import eu.stratosphere.nephele.managementgraph.ManagementVertex;
import eu.stratosphere.nephele.managementgraph.ManagementVertexID;

/**
 * A latency path is a path through the ManagementGraph, defined by a sequence
 * of {@link ManagementVertex} objects that are connected in the order in which they appear in
 * the sequence.
 * 
 * @author Bjoern Lohrmann
 */
public class LatencyPath implements Iterable<ManagementVertex> {

	private LinkedList<ManagementVertex> pathVertices;

	private HashMap<ManagementVertexID, ManagementEdge> ingoingEdges;

	private LatencySubgraph graph;

	private long pathLatencyInMillis;

	@SuppressWarnings("unchecked")
	public LatencyPath(LatencyPath toClone) {
		this.graph = toClone.graph;
		this.pathVertices = (LinkedList<ManagementVertex>) toClone.pathVertices.clone();
		this.ingoingEdges = (HashMap<ManagementVertexID, ManagementEdge>) toClone.ingoingEdges.clone();
	}

	public LatencyPath(LatencySubgraph graph, ManagementVertex firstVertex) {
		this.graph = graph;
		this.pathVertices = new LinkedList<ManagementVertex>();
		this.ingoingEdges = new HashMap<ManagementVertexID, ManagementEdge>();
		this.pathVertices.add(firstVertex);
	}

	public void appendVertex(ManagementVertex vertex, ManagementEdge ingoingEdge) {
		pathVertices.add(vertex);
		ingoingEdges.put(vertex.getID(), ingoingEdge);
	}

	public ManagementVertex getBegin() {
		return pathVertices.getFirst();
	}

	public ManagementVertex getEnd() {
		return pathVertices.getLast();
	}

	public ManagementEdge getIngoingEdge(ManagementVertex vertex) {
		return ingoingEdges.get(vertex.getID());
	}

	public void removeLastVertex() {
		ManagementVertex removed = pathVertices.removeLast();
		ingoingEdges.remove(removed);
	}

	/**
	 * Returns whether we have latency values for all parts (vertices and edges) of this
	 * path.
	 * 
	 * @return Whether we have latency values for all parts of this path
	 */
	public boolean isActive() {
		// FIXME inefficient, naive implementation. This may need to be precomputed.

		for (ManagementVertex vertex : pathVertices) {
			if (((VertexLatency) vertex.getAttachment()).getLatencyInMillis() == -1) {
				return false;
			}
		}

		for (ManagementEdge edge : ingoingEdges.values()) {
			if (((EdgeLatency) edge.getAttachment()).getLatencyInMillis() == -1) {
				return false;
			}
		}

		return true;
	}

	@Override
	public Iterator<ManagementVertex> iterator() {
		return pathVertices.iterator();
	}

	public long refreshPathLatency() {
		this.pathLatencyInMillis = 0;
		for (ManagementVertex vertex : pathVertices) {
			ManagementEdge ingoingEdge = ingoingEdges.get(vertex.getID());

			if (ingoingEdge != null) {
				this.pathLatencyInMillis += ((EdgeLatency) ingoingEdge.getAttachment()).getLatencyInMillis();
			}
			this.pathLatencyInMillis += ((VertexLatency) vertex.getAttachment()).getLatencyInMillis();
		}
		return this.pathLatencyInMillis;
	}

	public long getPathLatencyInMillis() {
		return this.pathLatencyInMillis;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LatencyPath[");
		ManagementVertex previous = null;
		for (ManagementVertex vertex : pathVertices) {
			if (previous != null) {
				builder.append("->");
			}
			builder.append(vertex);
			previous = vertex;
		}
		builder.append("]");

		return builder.toString();
	}
}
