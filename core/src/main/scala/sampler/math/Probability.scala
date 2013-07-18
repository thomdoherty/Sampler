/*
 * Copyright (c) 2012-2013 Crown Copyright 
 *                    Animal Health and Veterinary Laboratories Agency
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

package sampler.math

import scala.language.implicitConversions
import scala.math.Numeric.DoubleIsFractional

case class Probability(val value: Double){
	assert(value <= 1 && value >= 0, value + "is not a valid probability")
}

object Probability{
	val zero = Probability(0)

	implicit def toDouble(p: Probability): Double = p.value
	
	implicit object ProbabilityIsFractional extends Fractional[Probability]{
		def compare(x: Probability, y: Probability): Int = DoubleIsFractional.compare(x, y)
		def plus(x: Probability, y: Probability): Probability = Probability(x + y)
		def minus(x: Probability, y: Probability): Probability = Probability(x - y)
		def times(x: Probability, y: Probability): Probability = Probability(x * y)
		def negate(x: Probability): Probability = Probability(-x)
		def fromInt(x: Int): Probability = Probability(x)
		def toInt(x: Probability): Int = x.toInt
		def toLong(x: Probability): Long = x.toLong
		def toFloat(x: Probability): Float = x.toFloat
		def toDouble(x: Probability): Double = x
		def div(x: Probability, y: Probability): Probability = Probability(x / y)
	}
}
