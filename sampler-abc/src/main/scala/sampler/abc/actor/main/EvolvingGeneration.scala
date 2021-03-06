package sampler.abc.actor.main

import sampler.abc.Scored
import sampler.abc.Weighted
import scala.collection.immutable.Queue
import sampler.abc.Generation

case class EvolvingGeneration[P](
	currentTolerance: Double,
	previousGen: Generation[P],
	dueWeighing: ScoredParticles[P],
	weighed: WeighedParticles[P],
	idsObserved: Queue[Long]
){
	def emptyWeighingBuffer = copy(dueWeighing = ScoredParticles.empty)
	val buildingGeneration = previousGen.iteration + 1
}

object EvolvingGeneration {
	def init[P](completed: Generation[P]): EvolvingGeneration[P] = {
		EvolvingGeneration(
			Double.MaxValue,
			completed,
			ScoredParticles(Seq.empty[Tagged[Scored[P]]]),
			WeighedParticles(Seq.empty[Tagged[Weighted[P]]]),
			Queue.empty[Long]
		)
	}
}