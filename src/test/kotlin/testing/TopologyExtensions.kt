package testing

import bgp.*
import bgp.policies.shortestpath.ShortestPathExtender
import core.simulator.DelayGenerator
import core.simulator.ZeroDelayGenerator

/**
 * Created on 24-07-2017.
 *
 * @author David Fialho
 */

/**
 *
 */
object DummyBGPExtender : BGPExtender { override fun extend(route: BGPRoute, sender: BGPNode) = route }

/**
 *
 */
data class Link(val tail: Int, val head: Int, var extender: BGPExtender = DummyBGPExtender,
                var delayGenerator: DelayGenerator = ZeroDelayGenerator)

/**
 *
 */
fun bgpTopology(body: BGPTopologyBuilder.() -> Unit): BGPTopology {

    val builder = BGPTopologyBuilder()
    body(builder)
    return builder.build()
}

infix fun Int.to(head: Int) = Link(tail = this, head = head)

infix fun Link.using(extender: BGPExtender): Link {
    this.extender = extender
    return this
}

infix fun Link.delaysFrom(delayGenerator: DelayGenerator): Link {
    this.delayGenerator = delayGenerator
    return this
}

fun BGPTopologyBuilder.link(createLink: () -> Link) {

    val link = createLink()
    this.addNode(link.tail)
    this.addNode(link.head)
    this.addLink(link.tail, link.head, link.extender)
}

//region Extension functions specific to shortest-path routing

infix fun Link.withCost(cost: Int): Link {
    this.extender = ShortestPathExtender(cost)
    return this
}

//endregion