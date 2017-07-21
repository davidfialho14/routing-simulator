package bgp

import core.routing.NodeID

/**
 * Created on 21-07-2017
 *
 * @author David Fialho
 */
class BGPTopologyBuilder {

    private val ids = mutableSetOf<NodeID>()

    /**
     * Indicates to the builder that the topology to be built must include a node with the given ID.
     *
     * @return true if the ID was not added yet to the builder or false if otherwise
     */
    fun  addNode(id: NodeID): Boolean {
        return ids.add(id)
    }

    /**
     * Returns a BGPTopology containing the nodes and relationships defined in the builder at the time the method is
     * called.
     */
    fun build(): BGPTopology {
        return BGPTopology(ids.map { BGPNodeWith(id = it) }.toList())
    }

}