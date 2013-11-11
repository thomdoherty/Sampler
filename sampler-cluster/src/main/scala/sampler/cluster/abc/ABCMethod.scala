/*
 * Copyright (c) 2012-13 Crown Copyright 
 *                       Animal Health and Veterinary Laboratories Agency
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sampler.cluster.abc

import scala.concurrent.Await
import scala.concurrent.duration._
import akka.util.Timeout
import akka.actor.Props
import sampler.cluster.abc.actor.worker.AbortableModelRunner
import sampler.cluster.abc.actor.root.RootActor
import sampler.cluster.actor.PortFallbackSystemFactory
import sampler.abc.ABCModel
import sampler.abc.ABCParameters
import com.typesafe.config.ConfigFactory
import sampler.io.Logging
import sampler.cluster.abc.actor.Start

object ABCMethod extends Logging{
	def apply(model: ABCModel, abcParameters: ABCParameters) = {
		val system = PortFallbackSystemFactory("ABCSystem")
		
		val terminateAtTargetGeneration = ConfigFactory.load.getBoolean("sampler.abc.terminate-at-target-generation")
		
		val modelRunner = AbortableModelRunner(model)
		val abcActor = system.actorOf(
				Props(new RootActor(model, abcParameters, modelRunner)), 
				"abcrootactor"
		)
		
		import akka.pattern.ask
		implicit val timeout = Timeout(20.second)
		val future = (abcActor ? Start).mapTo[Seq[model.ParameterSet]]
		val result = Await.result(future, Duration.Inf)
		
		if(terminateAtTargetGeneration){
			log.info("Terminating actor system")
			system.shutdown
		}
		
		result
	}
}