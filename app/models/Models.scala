package models


import java.sql.Connection
import java.util.Date
import anorm.SqlParser._
import anorm._
import scala.language.postfixOps

/**
 * @author stasstels
 * @since 1/5/14.
 */

case class Team(id: Int, name: String)

case class Match(id: Int, homeTeam: Int, awayTeam: Int, homeScore: Int, awayScore: Int, played: Date)

case class Task(team: String, result: Option[Int], since: Option[Date])

case class TaskQuery(taskId: Int)

object Team {

  val byName = SQL(
    """
      select * from teams t
      where t.name = {name}
    """
  )

  val selectSql = SQL(
    """
      select * from teams
    """
  )


  val insertSql = SQL(
    """
      insert into teams(name)
      values ({name})
    """
  )

  def select(implicit c: Connection) = selectSql().collect {
    case Row(id: Int, name: String) => RawTeam(name)
  }.toList

  def insert(name: String)(implicit c: Connection) {
    insertSql.on("name" -> name).executeInsert()
  }

  def getTeamByName(name: String)(implicit c: Connection) = byName.on("name" -> name)().collect {
    case Row(id: Int, name: String) => Team(id, name)
  }.toList.headOption

  def insertIfNotExist(name: String)(implicit c: Connection) {
    getTeamByName(name) match {
      case None => insert(name)
      case _ => ()
    }
  }

}

object Match {

  val selectSql = SQL(
    """
      select HOME.name, AWAY.name, home_score, away_score, played
      from matches
      inner join teams AS HOME ON home_id = HOME.id
      inner join teams AS AWAY ON away_id = AWAY.id
    """
  )


  val insertSql = SQL(
    """
      insert into matches(home_id, away_id, home_score, away_score, played)
      values ({ht}, {at}, {hs}, {as}, {d})
    """
  )


  def insert(m: Match)(implicit c: Connection) {
    insertSql.on(
      "ht" -> m.homeTeam,
      "at" -> m.awayTeam,
      "hs" -> m.homeScore,
      "as" -> m.awayScore,
      "d" -> m.played).executeInsert()
  }

  def insertRawMatch(rm: RawMatch)(implicit c: Connection) {
    Team.insertIfNotExist(rm.home)
    Team.insertIfNotExist(rm.away)
    val home: Team = Team.getTeamByName(rm.home).get
    val away: Team = Team.getTeamByName(rm.away).get
    insert(Match(0, home.id, away.id, rm.hs, rm.as, rm.played))
  }

  def select(implicit c: Connection) = selectSql().collect {
    case Row(homeTeam: String, awayTeam: String, homeScore: Int, awayScore: Int, played: Date) => RawMatch(homeTeam, awayTeam, homeScore, awayScore, played)
  }.toList
}


object Task {

  val selectSql = SQL(
    """
      select HOME.name, AWAY.name, home_score, away_score, played
      from matches
      inner join teams AS HOME ON home_id = HOME.id
      inner join teams AS AWAY ON away_id = AWAY.id
      inner join tasks AS TASK ON TASK.team LIKE HOME.name OR TASK.team LIKE AWAY.name
      WHERE
            TASK.id = {taskId}
          AND
            ( (TASK.result IS NULL)
            OR
              (TASK.team LIKE HOME.name AND TASK.result = SIGN(home_score - away_score))
            OR
              (TASK.team LIKE AWAY.name AND TASK.result = SIGN(away_score - home_score))
            )
          AND
            ( (TASK.since IS NULL)
            OR
              (TASK.since <= played)
            )
    """
  )

  val insertSql = SQL (
    """
      insert into tasks(team, result, since)
      values ({team}, {r}, {d})
    """
  )

  def insert(t: Task)(implicit c: Connection): Option[Long] = insertSql.on(
    "team" -> t.team,
    "r" -> t.result.map(math.signum),
    "d" -> t.since).executeInsert()

  def selectTask(taskId: Long)(implicit c: Connection) = {
    selectSql.on("taskId" -> taskId)().collect {
      case Row(home: String, away: String, hs: Int, as: Int, played: Date) => RawMatch(home, away, hs, as, played)
    }.toList
  }

}

object TaskQuery {

  val insertSql = SQL(
    """
      |insert into taskQueries(task_id)
      |values ({tid})
    """.stripMargin
  )

  val selectSql = SQL(
    """
      |select T.team, T.result, T.since
      |from taskQueries AS TQ
      |inner join tasks AS T ON TQ.task_id = T.id
    """.stripMargin
  )

  def insert(tq: TaskQuery)(implicit c: Connection) = insertSql.on(
    "tid" -> tq.taskId
  ).executeInsert()

  def select(implicit c: Connection) = {
    selectSql.as((str("team") ~ int("result").? ~ date("since").? map {case n~r~d => Task(n, r, d)}) *).map(RawTask.Task2RawTask)
  }

}