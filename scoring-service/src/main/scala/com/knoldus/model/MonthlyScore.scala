package com.knoldus.model

case class MonthlyIndividualWithStudioScore(email : String, studioId : Int, score: Double, month : Int, year : Int)

case class MonthlyIndividualScore(email : String, score: Double, month : Int, year : Int)

case class MonthlyStudioScore(studioId : Int, score : Double , month :Int, year :Int )

case class AllTimeIndividualWithStudioScore(email : String, studioId : Int, score: Double)

case class AllTimeIndividualScore(email : String, score: Double)

case class AllTimeStudioScore(studioId : Int, score : Double )

case class DailyScore(email : String, fullName : String,  dailyScore: Double)


