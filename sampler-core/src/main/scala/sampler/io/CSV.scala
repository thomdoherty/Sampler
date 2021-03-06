package sampler.io

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import scala.Array.canBuildFrom
import scala.Array.fallbackCanBuildFrom
import scala.io.Source

/** Facilitates the reading and writing of csv files
 *  
 *  <h3>Reading files</h3>
 *  This object provides methods to read in data from csv files and also query the header line, 
 *  including the ability to match the header to expected content
 *  {{{
 *  val file = java.nio.file.Paths.get("test.csv")
 *  CSV.header(file)
 *  }}}
 *  Running the above code in the Scala interpreter would yield:
 *  {{{res: Map[String,Int] = Map(Col1 -> 0, Col2 -> 1, Col3 -> 2)}}}
 *     
 *  Files can be read in either in their entirety, or part thereof based on requested column headers. 
 *  {{{
 *  val file = java.nio.file.Paths.get("test.csv")
 *  val wholeFile = CSV.read(file)
 *  val selectedColumns = CSV.readByHeader(file, "Col1", "Col3")
 *  }}}
 *  Running the above in the Scala interpreter would yield:
 *  {{{
 *  > wholeFile.toIndexedSeq
 *      res: IndexedSeq[IndexedSeq[String]] = Vector(ArraySeq(Col1, Col2, Col3), ArraySeq(1, 2, 3), ArraySeq(4, 5, 6))
 *  
 *  > selectedColumns.toIndexedSeq
 *      res: IndexedSeq[IndexedSeq[String]] = Vector(ArraySeq(Col1, Col3), ArraySeq(1, 3), ArraySeq(4, 6))
 *  }}}
 *  
 *  <h3>Writing files</h3>
 *  Data, in the form of a Traversable of Traversables, can be written to disk given a path to a file. The
 *  file writing methods use the Java.nio StandardOpenOptions to define how a file should be written.
 *  {{{
 *  val outFile = java.nio.file.Paths.get("testOut.csv")
 *  val outData = Seq(Seq("Col1", "Col2", "Col3"),Seq(1,2,3),Seq(4,5,6))
 *  CSV.writeLines(outFile, outData, StandardOpenOption.CREATE)
 *  }}}
 *  
 *  <h3>Transposing</h3>
 *  Often data you might want to write to a csv is handled more naturally in the code as columns, i.e. a data structure with one set of
 *  data that would want to be written to a column. The CSV object writes data row-by-row. There are two options for writing the correct
 *  csv in these situations.
 *  
 *  1) Transpose in the code and then write to CSV as in the example below. The method has the benefit of requiring no writing of temporary
 *  files and that it is likely to be faster to execute. However, all the data is required to be present up front.
 *  {{{
 *  val names = Seq("Tom", "Dick", "Harry")
 *  val ages = Seq(25, 32, 18)
 *  val colours = Seq("Red", "Blue", "Green")
 *  
 *  val columns = Seq(
 *      "Name" +: names,
 *      "Age" +: ages,
 *      "FavouriteColour" +: colours
 *  )
 *  val transposed = columns.transpose
 *  
 *  CSV.writeLines(java.nio.file.Paths.get("transposed.csv"), transposed)
 *  }}}
 *  
 *  2) Writing a row based temporary file and then using the CSV methods to transpose the file. The advantage of this methdo being that 
 *  you don't need all the data up front and can build up the rows in the temporary file as the data becomes available.
 *  {{{
 *  val col1 = Seq(1,2,3)
 *  val col2 = Seq(4,5,6)
 *  
 *  val rowsFile = java.nio.file.Files.createTempFile(null, null)
 *  
 *  CSV.writeLine(rowsFile, col1, StandardOpenOption.CREATE)
 *  CSV.writeLine(rowsFile, col2, StandardOpenOption.APPEND)
 *  
 *  CSV.transpose(rowsFile, java.nio.file.Paths.get("transposed.csv"), StandardOpenOption.CREATE)
 *  }}}
 */
object CSV {
  
  /** Reads in the first line of a csv file and returns a map of the header to its column index
   * 
   * @param filePath The path to the csv file of interest
   * @return Map from a String (the header) to an Integer (the column index of that header)
   */
	def header(filePath: Path): Map[String, Int] = {
		val strings = Source.fromFile(filePath.toFile).getLines.next.split(',').map(_.trim)
		strings.zipWithIndex.map{case (name, idx) => name -> idx}.toMap
	}
	
	/** Read in the first line of a csv file and checks it matches the headers of interest
	 * 
	 * @param filePath The path to the csv file of interest
	 * @param expected The strings expected in the header
	 */
	def assertHeader(filePath: Path, expected: String*) {
		val strings = Source.fromFile(filePath.toFile).getLines.next.split(',').map(_.trim).toSeq
		assert(strings == expected, {
			val newLine = System.getProperty("line.separator") 
s"""Headers in ${filePath.toAbsolutePath()} don't match. 
	expected: 	${expected.toString}
	found:		${strings.toString}
"""})
	}
	
	/** Write a single line to a csv file
	 * 
	 * @param filePath The path to the csv file of interest
	 * @param line The data to be written to the file
	 * @param openOptions Options specifying how the file is opened
	 */
	def writeLine(filePath: Path, line: Traversable[Any], openOptions: OpenOption*) {
		val writer = getWriter(filePath, openOptions: _*)
		writer.write(toStrings(line).mkString(","))
		writer.newLine()
		writer.close()
	}
	
	/** Write multiples lines to a csv file
	 * 
	 * @param filePath The path to the csv file of interest
	 * @param lines The data to be written to the file
	 * @param openOptions Options specifying how the file is opened
	 */
	def writeLines(filePath: Path, lines: Traversable[Traversable[Any]], openOptions: OpenOption*) {
		val writer = getWriter(filePath, openOptions: _*)
		lines.foreach{line => 
			writer.write(toStrings(line).mkString(","))
			writer.newLine()
		}
		writer.close()
	}
	
	private def toStrings(line: Traversable[Any]): Traversable[String] = {
		val strings = line.map(_.toString)
		assert(!strings.exists(_.contains(',')), "CSV lines may not contain commas: "+strings.mkString(" | "," | "," | "))
		strings
	}
	
	/** Reads in a csv file, transposes the data and writes to another csv
	 * 
	 * @param inPath The path to the csv file containing the data to be transposed
	 * @param outFile The path to the csv file to write the transposed data to
	 * @param removeHeader Set to true if the data file contains a header line that needs removing before transposition
	 * @param openOptions Options specifying how the file is opened
	 */
	def transpose(inPath: Path, outFile: Path, outputOpenOptions: OpenOption*) {
		val matrix = Source.fromFile(inPath.toFile)
				.getLines
				.map(line => line.split(',').map(_.trim).toList)
				.toList
		writeLines(outFile, matrix.transpose, outputOpenOptions: _*)
	}
	
	/** Reads in the data in a csv file
	 *  
	 *  @param filePath The path to the csv file to be read in
	 *  @return An iterator which contains each line of data as a IndexedSeq of Strings
	 */
	def read(filePath: Path): Iterator[IndexedSeq[String]] = {
		Source.fromFile(filePath.toFile()).getLines.map{line =>
			line.split(',').map(_.trim)
		}
	}
	
	private def getWriter(filePath: Path, openOptions: OpenOption*) = {
		Files.newBufferedWriter(
				filePath, 
				Charset.defaultCharset(), 
				openOptions:_*
		)
	}
}