/*
 * Copyright (c) 2012 Crown Copyright 
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

package sampler.cluster.deploy

import java.nio.file.Path
import sampler.io.Logging
import scala.language.postfixOps

class SSH(keyFile: Path) extends Logging{
	private val keyFileArgs = List("-i", keyFile.toString)
	private val noHostFileArgs = List(
			"-o", "StrictHostKeyChecking=no",
			"-o","UserKnownHostsFile=/dev/null",
			"-o", "LogLevel=quiet"
	)
	
	def forgroundCommand(username: String, host: String, command: String): String = {
		val sshCommand = "ssh" ::
			List("-t","-t") ::: 
			keyFileArgs :::
			noHostFileArgs :::
			List(
				s"$username@$host",
				command
			)
		
		sshCommand.mkString(" ")
	}
	
	def backgroundCommand(username: String, host: String, command: String): String = {
		val sshCommand = "ssh" ::
			List("-f","-n") ::: 
			keyFileArgs ::: 
			noHostFileArgs :::
			List(
				s"$username@$host",
				"""sh -c 'nohup """+command+""" > /dev/null 2>&1 &'"""
			)
		
		sshCommand.mkString(" ")
	}
	
	def scpCommand(username: String, host: String, localPath: Path, remoteDestination: String): String = {
		val scpCommand = 
			"scp" ::
			keyFileArgs ::: 
			noHostFileArgs ::: 
			List(s"$localPath", s"$username@$host:$remoteDestination")
		
		scpCommand.mkString(" ")
	}
}