package controllers

import play.api.Play.current
import play.api.db.DB
import play.api.mvc.{AnyContent, Request, Action, Controller}
import java.sql.Connection
import models._
import java.util.Date
import scala.language.implicitConversions
import java.text.SimpleDateFormat
import models.RawInsertResult
import scala.Some
import models.RawTeam
import play.api.Logger

object Application extends Controller  {

  val Format = new SimpleDateFormat("yyyy-MM-dd")

  implicit def string2date(date: String): Date = Format.parse(date)

  def index = Action {
    Redirect(routes.Application.matches)
  }

  def redirect(wrong: String) = Action {
    Redirect(routes.Application.matches)
  }

  def matches = matchesResponse(getAllMatches)

  def history = historyResponse(getHistory)

  def options = Action {
    r =>
      acceptedContent(r) match {
        case "text/plain" => Ok(Options.methods.toString()).withHeaders(ALLOW -> "GET, PUT, OPTIONS")
        case "text/xml" => Ok(JAXBWrappers.raw2XMLString(RawList(Options.methods))).withHeaders(ALLOW -> "GET, PUT, OPTIONS")
        case "text/javascript" => Ok(JAXBWrappers.raw2JSONString(RawList(Options.methods))).withHeaders(ALLOW -> "GET, PUT, OPTIONS")
        case "text/html" => Ok(views.html.methods(Options.methods))
        case _ => MethodNotAllowed.withHeaders(ALLOW -> "GET, PUT, OPTIONS")
      }
  }


  def matchesResponse(matches: List[RawMatch]) = Action { r =>
    acceptedContent(r) match {
      case "text/plain" => Ok(matches.toString())
      case "text/xml" => Ok(JAXBWrappers.raw2XMLString(RawList(matches)))
      case "text/javascript" => Ok(JAXBWrappers.raw2JSONString(RawList(matches)))
      case "text/html" => Ok(views.html.matches(matches))
      case _ => MethodNotAllowed
    }
  }  
  
  def historyResponse(tasks: List[RawTask]) = Action { r =>
    acceptedContent(r) match {
      case "text/plain" => Ok(tasks.toString())
      case "text/xml" => Ok(JAXBWrappers.raw2XMLString(RawList(tasks)))
      case "text/javascript" => Ok(JAXBWrappers.raw2JSONString(RawList(tasks)))
      case "text/html" => Ok(views.html.tasks(tasks.map(RawTask.RawTask2Task)))
      case _ => MethodNotAllowed
    }
  }

  def acceptedContent(r: Request[AnyContent]) = {
    r.acceptedTypes.headOption.map(_.toString()).getOrElse("text/html")
  }


  def getAllMatches: List[RawMatch] = DB.withConnection { implicit c: Connection =>
    Match.select
  }

  def teamsResponse(teams: List[RawTeam]) = Action { r =>
    acceptedContent(r) match {
      case "text/plain" => Ok(teams.toString())
      case "text/xml" => Ok(JAXBWrappers.raw2XMLString(RawList(teams)))
      case "text/javascript" => Ok(JAXBWrappers.raw2JSONString(RawList(teams)))
      case "text/html" => Ok(views.html.teams(teams))
      case _ => MethodNotAllowed
    }
  }
  
  def insertResponse(insertResult: RawInsertResult, content: String) = {
    content match {
      case "text/plain" => Ok(insertResult.toString)
      case "text/xml" => Ok(JAXBWrappers.raw2XMLString(insertResult))
      case "text/javascript" => Ok(JAXBWrappers.raw2JSONString(insertResult))
      case "text/html" => Ok(insertResult.toString)
      case _ => MethodNotAllowed
    }
  }

  def teams = teamsResponse(getAllTeams)

  def getHistory = DB.withConnection {implicit c: Connection =>
    TaskQuery.select
  }

  def getAllTeams: List[RawTeam] = DB.withConnection { implicit c: Connection =>
    Team.select
  }

  def getTaskById(id: Int): List[RawMatch] = DB.withTransaction { implicit c: Connection =>
    TaskQuery.insert(TaskQuery(id))
    Task.selectTask(id)
  }

  def task(id: Int) = matchesResponse(getTaskById(id))

  def parseMatchBody(r: Request[AnyContent]) = {
    r.contentType.getOrElse("") match {
      case "text/xml" => RawMatch.unmarshallXML(getTextBody(r))
      case "text/javascript" => RawMatch.unmarshallJSON(getTextBody(r))
      case _ => None
    }
  }

  def parseTaskBody(r: Request[AnyContent]) = r.contentType.getOrElse("") match {
    case "text/xml" => RawTask.unmarshallXML(getTextBody(r))
    case "text/javascript" => RawTask.unmarshallJSON(getTextBody(r))
    case _ => None
  }


  def addMatch() = Action { r =>
    Logger.info(r.contentType.toString)
    parseMatchBody(r) match {
      case Some(rm) => createMatch(rm)
      case _ => NotAcceptable
    }
  }

  def getTextBody(r: Request[AnyContent]) = r.contentType.getOrElse("") match {
    case "text/xml" => r.body.asXml.map(_.toString()).getOrElse("")
    case "text/javascript" => r.body.asJson.map(_.toString()).getOrElse("")
    case _ => ""
  }

  def createMatch(rm: RawMatch) = DB.withTransaction { implicit c: Connection =>
    Match.insertRawMatch(rm)
    Ok
  }


  def addTask() = Action { r =>
    parseTaskBody(r) match {
      case Some(t)  => createTask(t).map(insertResponse(_, acceptedContent(r))).getOrElse(NotAcceptable)
      case _ => NotAcceptable
    }
  }

  def createTask(rt: RawTask): Option[RawInsertResult] = DB.withTransaction { implicit c: Connection =>
    Task.insert(rt).map(RawInsertResult)
  }


  object Options {
    val methods = List(
      RawMethod("GET", "/matches", null),
      RawMethod("GET", "/history", null),
      RawMethod("GET", "/task/", "taskId"),
      RawMethod("GET", "/teams", null),
      RawMethod("PUT", "/match/", "match"),
      RawMethod("PUT", "/task/", "task")
    )
  }

}