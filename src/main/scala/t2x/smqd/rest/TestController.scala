package t2x.smqd.rest

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, StreamConverters}
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import t2x.smqd.Smqd

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
  * 2018. 6. 24. - Created by Kwon, Yeong Eon
  */
class TestController(name: String, smqd: Smqd, config: Config) extends RestController(name, smqd, config) with Directives with StrictLogging {

  override def routes: Route = metrics

  private implicit val system: ActorSystem = smqd.system
  private implicit val ec: ExecutionContext = smqd.gloablDispatcher
  private implicit val materializer: ActorMaterializer = ActorMaterializer()

  def metrics: Route = {
    ignoreTrailingSlash {
      extractRequestContext { ctx =>
        path("blackhole" / Remaining.?) { remain =>
          val suffix = remain match {
            case Some(str) => str
            case None => ""
          }

          val contentType = ctx.request.entity.getContentType

          val content = if (contentType.mediaType.isText){
            ctx.request.entity.dataBytes.map(bs => bs.utf8String).runFold(new StringBuilder())(_.append(_)).map(_.toString)
          }
          else {
            ctx.request.entity.dataBytes.map(bs => bs.length).runFold(0)(_+_).map(i => i.toString)
          }

          val received = ctx.request.entity.contentLengthOption.getOrElse(0)

          content.onComplete{
            case Success(str) =>
              logger.debug("Blackhole received {} ({} bytes) with {}", str, received, suffix)
            case Failure(ex) =>
              logger.debug("Blackhole failed", ex)
          }
          complete(StatusCodes.OK, s"OK $received bytes received")
        } ~
          path("echo") {
            complete(StatusCodes.OK, "Hello")
          }
      }
    }
  }
}