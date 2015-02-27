package sampler.spike.farrington

import java.nio.file.{Files,Paths}
import java.time.YearMonth
import scala.annotation.elidable.ASSERTION
import scala.collection.immutable.TreeMap
import scala.io.Source
import scala.language.{postfixOps,implicitConversions}
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.native.JsonMethods._
import org.rosuda.REngine.Rserve.RConnection
import sampler.r.rserve.RServeHelper
import java.time._
import java.time.Year
import java.time.ZoneId
import java.time.temporal.ChronoUnit._
import java.time.temporal.ChronoField
import java.time.Duration.of
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import scala.collection.SortedMap
import org.json4s.JsonAST.JValue
import org.json4s.native.JsonMethods
import java.nio.charset.Charset
import java.nio.file.Path
import java.io.OutputStream

/*
  =========
  NOTES:
  
  
  Follows the method outlined in
  Farrington et al., ...
  Noufaily et al., Statist. Med. 2013 (32) 1206-1222
  
  =========
  AUTHOR:
  
  Author:    Teedah Saratoon (modified from EDS.scala by Oliver Tearne)
  Date:      26/02/2015
  Last edit: 26/02/2015
  
  ==========
  USER-DEFINED PARAMETERS:

  
  
  =========
  FUNCTIONS:
  
  
  
  =========  
  OUTPUTS:
    
  
  
  */

object EDS_simData extends App{
  
  //=======================
  // User-defined parameters
  
	// Number of months for which to simulate data:
	val nData = 462
	val endYear = 2014 
	
	// Choose "short" or "long" outbreaks
	// outbreakLength = "short"
	val outbreakLength = "long"
	
	// Define end of each period
	//Baseline -> Pre-outbreak -> Outbreak -> Post-outbreak
	val endBaseline = 146
	val endPreOutbreak = 182
	val endOutbreak = 282
			
  val resultsDir = "results/farrington"
  
  //=======================
  // Simulate outbreak data
    
  val countData = GenerateData.run(nData, outbreakLength, endPreOutbreak, endOutbreak)
  
  // Construct sequences of months and years
  val startYear = math.round(endYear - nData.toDouble/12)  
  val month = (1 to nData).map(i => (i-1) % 12 + 1)  
  val year = (1 to nData).map(i => (startYear + ((i-1) / 12)).toInt)
  
  // Print relevant information to console:
  println("No. of months = " + nData)
  println("Baseline period starts at " + year(0) + "-" + month(0))
  println("Pre-outbreak period starts at " + year(endBaseline) + "-" + month(endBaseline))
  println("Outbreak period starts at " + year(endPreOutbreak) + "-" + month(endPreOutbreak))
  println("Post-outbreak period starts at " + year(endOutbreak) + "-" + month(endOutbreak))
  
  // Create TreeMap with form (YearMonth,Count)
  val dataOutbreak_all = TreeMap{
    (0 until nData).map{ i => YearMonth.of(year(i), month(i)) -> countData(i) }: _*
  }
  
  // Exclude set of months corresponding to 2001
  val exclude2001 = (1 to 12).map{m => YearMonth.of(2001, m)}.to[Set]
  val dataOutbreak = dataOutbreak_all.--(exclude2001)
  
  //=======================
  // Run Farrington algorithm
  
  RServeHelper.ensureRunning()
  val rCon = new RConnection                                                                                                                                        
  val results = try{
    val indexedData = indexAndExclude(dataOutbreak, exclude2001)
    (0 to (nData - endBaseline)).map{i => 
      val series = extractWindow(indexedData.dropRight(i))
      Farrington.run(series, rCon)
    }
  } finally {
    rCon.close
    RServeHelper.shutdown
  }
  
  val timeSeriesJSON = 
    ("source" -> "Simulated data" ) ~
    ("month" -> results.map(_.date.yearMonth.toString)) ~
    ("monthId" -> results.map(_.date.idx)) ~
    ("expected" -> results.map(_.expected)) ~
    ("threshold" -> results.map(_.threshold)) ~
    ("actual" -> results.map(_.actual))
    
    println(timeSeriesJSON)
    
  FreeMarkerHelper.writeFile(
    Map("jsonData" -> pretty(render(timeSeriesJSON))),
    "plot.ftl",
    Paths.get(resultsDir).resolve("output.html") 
  )
  
  //=======================
  // Function definitions
  
  def indexAndExclude(
      obsByDate: SortedMap[YearMonth, Int], 
      exclusions: Set[YearMonth] = Set.empty
      ): SortedMap[Date, Int] = {
    assert(!obsByDate.exists{case (ym, _) => exclusions.contains(ym)})
    
    val removedExclusions = obsByDate.filterKeys{ym => !exclusions.contains(ym)}
    val firstDate = removedExclusions.firstKey
        
        implicit val dateOrdering = Ordering.by{d: Date => d.idx}
    
    removedExclusions.map{case (ym, count) => Date(ym, MONTHS.between(firstDate, ym)) -> count}
  }
  
  def extractWindow(timeSeries: SortedMap[Date, Int]): SortedMap[Date, Int] = {
    val lastObsDate = timeSeries.lastKey
        val window = List(-1, 0, 1).map(v => (v + 12) % 12)
        val windowLowerBound = lastObsDate.yearMonth.minus(12, YEARS).minus(1, MONTHS)
        
        def keep(date: Date) = {
      val monthRemainder = MONTHS.between(date.yearMonth, lastObsDate.yearMonth) % 12
          val inWindow = window.exists(_ == monthRemainder)
          
          val isAfterStartDate = windowLowerBound.compareTo(date.yearMonth) <= 0 
          val isBeforeEndDate = MONTHS.between(date.yearMonth, lastObsDate.yearMonth) > 2
          val isBaseline = inWindow && isAfterStartDate && isBeforeEndDate
          
          isBaseline || date == lastObsDate
    }
    val t = timeSeries.filterKeys(keep)
        t
  }
  
}