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

package sampler.run

import scala.util.Try

class ParallelCollectionRunner(aborter: Aborter) extends JobRunner{
	def apply[T](jobs: Seq[Job[T]]): Seq[Try[T]] = {
		jobs.par.map(job => Try(job.run(aborter))).seq
	}
}
object ParallelCollectionRunner{
	def apply() = new ParallelCollectionRunner(new Aborter)
}