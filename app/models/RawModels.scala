package models

/**
 * @author stasstels
 * @since 1/7/14.
 */
import java.util.Date
import javax.xml.bind.{Unmarshaller, Marshaller, JAXBContext}
import java.io.{StringWriter, StringReader}
import scala.collection.JavaConverters._
import com.sun.jersey.api.json._
import javax.xml.bind.annotation.{XmlAnyElement, XmlAccessorType, XmlAccessType, XmlRootElement}
import scala.language.implicitConversions
import play.Logger


@XmlRootElement(name = "rawtask")
@XmlAccessorType(XmlAccessType.FIELD)
case class RawTask(name: String, result: Integer, since: Date) {
  private def this() = this("", null, null)
}

@XmlRootElement(name = "rawinsert")
@XmlAccessorType(XmlAccessType.FIELD)
case class RawInsertResult(id: Long) {
  private def this() = this(-1)
}

@XmlRootElement(name = "rawteam")
@XmlAccessorType(XmlAccessType.FIELD)
case class RawTeam(name: String) {
  private def this() = this("")
}

@XmlRootElement(name = "rawmatch")
@XmlAccessorType(XmlAccessType.FIELD)
case class RawMatch(home: String, away: String, hs: Int, as: Int, played: Date) {
  private def this() = this("", "", -1, -1, new Date)
}

@XmlRootElement(name = "rawlist")
class RawList[T] {
  import scala.collection.JavaConversions._
  import scala.collection.mutable.Buffer

  private var rawList = Buffer[T]()

  @XmlAnyElement(lax = true)
  def getRawList: java.util.List[T] = this.rawList

  def setRawList(rawList: java.util.List[T]) {
    this.rawList = rawList
  }

  override def toString = rawList.toString()

}

object RawMatch {
  private val contextXML = JAXBContext.newInstance(classOf[RawMatch])
  private val cfg: JSONConfiguration = JSONConfiguration.natural().rootUnwrapping(false).build()
  private val contextJSON: JSONJAXBContext = new JSONJAXBContext(cfg, classOf[RawMatch])

  private val unmarshaller: Unmarshaller = contextXML.createUnmarshaller()
  private val jSONUnmarshaller: JSONUnmarshaller = contextJSON.createJSONUnmarshaller()

  def unmarshallXML(xml: String) : Option[RawMatch] = {
      try {
        unmarshaller.unmarshal(new StringReader(xml)) match {
          case x: RawMatch => Some(x)
          case _ => None
        }
      }
      catch {
        case ex: Throwable => None
      }
  }

  def unmarshallJSON(json: String) : Option[RawMatch] = {
    try {
      jSONUnmarshaller.unmarshalFromJSON(new StringReader(json), classOf[RawMatch]) match {
        case x: RawMatch => Some(x)
        case _ => None
      }
    }
    catch {
      case ex: Throwable => None
    }
  }

}

object RawTask {
  private val contextXML = JAXBContext.newInstance(classOf[RawTask])
  private val cfg: JSONConfiguration = JSONConfiguration.natural().rootUnwrapping(false).build()
  private val contextJSON: JSONJAXBContext = new JSONJAXBContext(cfg, classOf[RawTask])

  private val unmarshaller: Unmarshaller = contextXML.createUnmarshaller()
  private val jSONUnmarshaller: JSONUnmarshaller = contextJSON.createJSONUnmarshaller()

  implicit def Task2RawTask(t: Task) = RawTask(t.team, t.result.map(Integer.valueOf).getOrElse(null), t.since.getOrElse(null))
  implicit def RawTask2Task(rt: RawTask) = Task(rt.name, Option(rt.result).map(_.intValue), Option(rt.since))

  def unmarshallXML(xml: String) : Option[RawTask] = {
    try {
      unmarshaller.unmarshal(new StringReader(xml)) match {
        case x: RawTask => Some(x)
        case _ => None
      }
    }
    catch {
      case ex: Throwable => None
    }
  }

  def unmarshallJSON(json: String) : Option[RawTask] = {
    try {
      jSONUnmarshaller.unmarshalFromJSON(new StringReader(json), classOf[RawTask]) match {
        case x: RawTask => Some(x)
        case _ => None
      }
    }
    catch {
      case ex: Throwable => None
    }
  }

}

object RawList {
  def apply[T](s: List[T]) = {
    val list: RawList[T] = new RawList[T]
    list.setRawList(s.asJava)
    list
  }
}


object JAXBWrappers {

  private val seq = Seq(
    classOf[RawMatch],
    classOf[RawList[RawMatch]],
    classOf[RawList[RawTeam]],
    classOf[RawTeam],
    classOf[RawTask],
    classOf[RawInsertResult])


  private val contextXML = JAXBContext.newInstance(seq:_*)

  private val marshaller: Marshaller = contextXML.createMarshaller()

  private val unmarshaller: Unmarshaller = contextXML.createUnmarshaller()

  private val cfg: JSONConfiguration = JSONConfiguration.natural().rootUnwrapping(false).build()

  private val contextJSON: JSONJAXBContext = new JSONJAXBContext(cfg, seq:_*)

  private val jSONMarshaller: JSONMarshaller = contextJSON.createJSONMarshaller()

  val jSONUnmarshaller: JSONUnmarshaller = contextJSON.createJSONUnmarshaller()

  def raw2JSONString[T](raw: T) = {
    val writer: StringWriter = new StringWriter()
    jSONMarshaller.marshallToJSON(raw, writer)
    writer.toString
  }



  def raw2XMLString[T](raw: T) = {
    val writer: StringWriter = new StringWriter()
    marshaller.marshal(raw, writer)
    writer.toString
  }

  def XMLString2Option(xml: String) = {
    try {
      Some(unmarshaller.unmarshal(new StringReader(xml)))
    }
    catch {
      case ex: Throwable => None
    }
  }


}