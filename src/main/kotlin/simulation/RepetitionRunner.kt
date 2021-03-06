package simulation

import core.routing.Node
import core.routing.NodeID
import core.routing.Route
import core.routing.Topology
import core.simulator.DelayGenerator
import core.simulator.Engine
import core.simulator.Time
import io.Metadata
import io.TopologyReaderHandler
import ui.Application
import java.io.File
import java.time.Instant

/**
 * Created on 29-08-2017
 *
 * @author David Fialho
 */
class RepetitionRunner<R: Route>(
        private val topologyFile: File,
        private val topologyReader: TopologyReaderHandler<R>,
        private val destinationID: NodeID,
        private val repetitions: Int,
        private val messageDelayGenerator: DelayGenerator,
        private val stubDB: StubDB<R>?,
        private val threshold: Time,
        private val metadataFile: File

): Runner {

    /**
     * Runs the specified execution the number of times specified in the [repetitions] property.
     *
     * The engine configurations may be modified during the run. At the end of this method the engine is always
     * reverted to its defaults.
     *
     * @param execution        the execution that will be executed in each run
     * @param application the application running that wants to monitor progress and handle errors
     */
    override fun run(execution: Execution, application: Application) {

        val startInstant = Instant.now()

        val topology: Topology<R> = application.loadTopology(topologyFile, topologyReader) {
            topologyReader.read()
        }

        val destination: Node<R> = application.findDestination(destinationID) {
            topology[destinationID] ?: stubDB?.getStub(destinationID, topology)
        }

        Engine.messageDelayGenerator = messageDelayGenerator

        application.run {

            try {
                repeat(times = repetitions) { repetition ->

                    application.execute(repetition + 1, destination, messageDelayGenerator.seed) {
                        execution.execute(topology, destination, threshold)
                    }

                    // Cleanup for next execution
                    topology.reset()
                    destination.reset()
                    Engine.messageDelayGenerator.generateNewSeed()
                }

            } finally {
                // Make sure that the engine is always reverted to the defaults after running
                Engine.resetToDefaults()
            }
        }

        // Output metadata
        Metadata(
                Engine.version(),
                startInstant,
                finishInstant = Instant.now(),
                topologyFilename = topologyFile.name,
                stubsFilename = stubDB?.stubsFile?.name,
                destinationID = destinationID,
                minDelay = messageDelayGenerator.min,
                maxDelay = messageDelayGenerator.max,
                threshold = threshold
        ).print(metadataFile)

    }
}