package sampler.abc.actor.main

import org.scalatest.FreeSpec
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import sampler.abc.Scored
import sampler.abc.Generation
import scala.collection.immutable.Queue
import sampler.abc.Weighted

class EvolvingGenerationTest extends FreeSpec with Matchers with MockitoSugar {
	type T = Int
	
	"EvolvingGeneration should" - {
		"Initialise" in {
			val previousGen = mock[Generation[T]]
			val result = EvolvingGeneration.init(previousGen)
			val expected = EvolvingGeneration(
					Double.MaxValue,
					previousGen,
					ScoredParticles(Seq.empty[Tagged[Scored[T]]]),
					WeighedParticles(Seq.empty[Tagged[Weighted[T]]]),
					Queue.empty[Long]
			)
			assert(result === expected)
		}
		
		"Emptying the weighing buffer" in {
	    val scored1 = Tagged(Scored(1, Seq(0.5)), 111111)
	    val scored2 = Tagged(Scored(2, Seq(0.5)), 111112)
	    val weighed1 = Tagged(WeighedParticles(Seq(null, null)))
	  
	    val prevGen = mock[Generation[T]]
	    val idsObserved = mock[Queue[Long]]
	    val scored = mock[ScoredParticles[T]]
	    val weighed = mock[WeighedParticles[T]]
	    val eGen = EvolvingGeneration[T](
				0.1,
				prevGen,
				scored,	//items in the weighing buffer
				weighed,
				idsObserved
			)
	  
	    val eGenNew = eGen.emptyWeighingBuffer
	  
	    val expected = EvolvingGeneration[T](
				0.1,
				prevGen,
				ScoredParticles.empty,
				weighed,
				idsObserved
			)
			
			eGenNew shouldBe expected
	  }
	}
}