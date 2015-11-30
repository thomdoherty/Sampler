package sampler.abc.actor.main.helper

import sampler.math.Random
import sampler.abc.config.ABCConfig
import sampler.abc.actor.main.WeighedParticles
import sampler.abc.actor.main.ScoredParticles
import sampler.abc.actor.main.EvolvingGeneration

class Helper(
		particleMixer: ParticleMixer,
		getters: Getters,
		random: Random
	){
	
	def addWeightedParticles[P](
			incoming: WeighedParticles[P],
			eGen: EvolvingGeneration[P]
	): EvolvingGeneration[P] = {
		val weightedParticles = eGen.weighed
		eGen.copy(weighed = weightedParticles.add(incoming))
	}
		
	def filterAndQueueUnweighedParticles[P](
		taggedAndScoredParamSets: ScoredParticles[P],
		gen: EvolvingGeneration[P]
	): EvolvingGeneration[P] = {
		val observedIds = gen.idsObserved
		val particlesDueWeighting = gen.dueWeighing
		
		val filtered = taggedAndScoredParamSets.seq.filter(tagged => !observedIds.contains(tagged.id))
		
		gen.copy(
				dueWeighing = particlesDueWeighting.add(filtered),
				idsObserved = observedIds ++ filtered.map(_.id)
		)
	}
		
//	def flushGeneration[P](gen: EvolvingGeneration[P]): EvolvingGeneration[P] = 
//		generationFlusher(gen)
		
	def isEnoughParticles(gen: EvolvingGeneration[_], config: ABCConfig): Boolean =
		gen.weighed.size >= config.job.numParticles
	
	def emptyWeighingBuffer[P](gen: EvolvingGeneration[P]): EvolvingGeneration[P] = 
		gen.copy(dueWeighing = ScoredParticles.empty)
			
	//TODO can we simplify tagged and scored parm sets?
	def buildMixPayload[P](gen: EvolvingGeneration[P], abcParameters: ABCConfig): Option[ScoredParticles[P]] = {
		particleMixer.apply(gen, abcParameters)(random)
	}
}