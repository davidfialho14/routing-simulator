package bgp2

import bgp2.notifications.BGPNotifier
import bgp2.notifications.DetectNotification
import core.routing2.Node
import core.routing2.RoutingTable
import core.simulator.Time

/**
 * Base class for the SS-BGP like protocols. Implements the deactivation of neighbors and leaves the detection
 * condition to the subclasses.
 */
abstract class BaseSSBGP(mrai: Time = 0, routingTable: RoutingTable<BGPRoute>): BaseBGP(mrai, routingTable) {

    /**
     * Invoked the the BaseBGP right after a routing loop is detected.
     *
     * An SS-BGP like protocol checks if the routing loop is recurrent and if so it deactivates the neighbor that
     * sent the route.
     */
    final override fun onLoopDetected(node: Node<BGPRoute>, sender: Node<BGPRoute>, route: BGPRoute) {

        // Ignore route learned from a disabled neighbor
        if (!routingTable.table.isEnabled(sender)) {
            return
        }

        // Since a loop routing was detected, the new route via the sender node is surely invalid

        // Set the route via the sender as invalid
        // This will force the selector to select the alternative route
        val updated = routingTable.update(sender, BGPRoute.invalid())
        wasSelectedRouteUpdated = wasSelectedRouteUpdated || updated

        val alternativeRoute = routingTable.getSelectedRoute()
        if (isRecurrent(node, route, alternativeRoute)) {
            disableNeighbor(sender)
            BGPNotifier.notifyDetect(DetectNotification(node, route, alternativeRoute, sender))
        }
    }

    /**
     * Checks if the routing loop detected is recurrent.
     * Subclasses must implement this method to define the detection condition.
     */
    abstract fun isRecurrent(node: Node<BGPRoute>, learnedRoute: BGPRoute, alternativeRoute: BGPRoute): Boolean

    /**
     * Enables the specified neighbor.
     *
     * May update the `wasSelectedRouteUpdated` property.
     *
     * @param neighbor the neighbor to enable
     */
    fun enableNeighbor(neighbor: Node<BGPRoute>) {
        val updated = routingTable.enable(neighbor)
        wasSelectedRouteUpdated = wasSelectedRouteUpdated || updated
    }

    /**
     * Disables the specified neighbor.
     *
     * May update the `wasSelectedRouteUpdated` property.
     *
     * @param neighbor the neighbor to disable
     */
    fun disableNeighbor(neighbor: Node<BGPRoute>) {
        val updated = routingTable.disable(neighbor)
        wasSelectedRouteUpdated = wasSelectedRouteUpdated || updated
    }

    override fun reset() {
        super.reset()
    }
}

/**
 * SS-BGP Protocol: when a loop is detected it tries to detect if the loop is recurrent using the WEAK detection
 * condition. If it determines the loop is recurrent, it disables the neighbor that exported the route.
 */
class SSBGP(mrai: Time = 0, routingTable: RoutingTable<BGPRoute> = RoutingTable.empty(BGPRoute.invalid()))
    : BaseSSBGP(mrai, routingTable) {

    override fun isRecurrent(node: Node<BGPRoute>, learnedRoute: BGPRoute, alternativeRoute: BGPRoute): Boolean {
        return learnedRoute.localPref > alternativeRoute.localPref
    }
}

/**
 * ISS-BGP: when a loop is detected it tries to detect if the loop is recurrent using the STRONG detection
 * condition. If it determines the loop is recurrent, it disables the neighbor that exported the route.
 */
class ISSBGP(mrai: Time = 0, routingTable: RoutingTable<BGPRoute> = RoutingTable.empty(BGPRoute.invalid()))
    : BaseSSBGP(mrai, routingTable) {

    override fun isRecurrent(node: Node<BGPRoute>, learnedRoute: BGPRoute, alternativeRoute: BGPRoute): Boolean {
        return learnedRoute.localPref > alternativeRoute.localPref &&
                alternativeRoute.asPath == learnedRoute.asPath.subPathBefore(node)
    }
}