import cats.effect._
import cats.implicits._
import cats.syntax.all._

import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware.Logger
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.client.Client
import org.http4s.headers.Authorization
import cats.effect.std.Env

import io.circe.Json
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec._

import fs2.Stream
import dev.kovstas.fs2throttler.Throttler.throttle
import dev.kovstas.fs2throttler.Throttler.Shaping
import scala.concurrent.duration._
import scala.collection.MapView

implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

val client_res = EmberClientBuilder.default[IO].build

case class FavgroupResponse(
    id: Int,
    name: String,
    post_ids: List[Int]
)

val t = FavgroupResponse(id = 0, name = "", post_ids = List.empty).asJson

case class DanbooruAuth(
    login: String,
    api_key: String
)

def favGroups(
    pool: String
)(using client: Client[IO])(using auth: DanbooruAuth): IO[FavgroupResponse] = {
  val uri_req =
    uri"https://danbooru.donmai.us/favorite_groups" / s"$pool.json" +? ("login", auth.login) +? ("api_key", auth.api_key)

  val req = Request[IO](
    method = Method.GET,
    uri = uri_req
  )

  client.expect[FavgroupResponse](req)
}

case class PostResponse(
    id: Int,
    tag_string_artist: String,
    preview_file_url: Option[String],
    file_url: Option[String]
)

def postInfo(
    post: Int
)(using client: Client[IO])(using auth: DanbooruAuth): IO[PostResponse] = {
  val uri_req =
    uri"https://danbooru.donmai.us" / "posts" / s"$post.json" +? ("login", auth.login) +? ("api_key", auth.api_key)

  val req = Request[IO](
    method = Method.GET,
    uri = uri_req
  )

  client.expect[PostResponse](req)
}

def commonAuthors(
    posts: List[Int]
)(using
    client: Client[IO]
)(using auth: DanbooruAuth): IO[Map[String, List[PostResponse]]] = {
  for {
    l <- Stream
      .emits(posts)
      .covary[IO]
      .parEvalMap(5)(elem =>
        for {
          resp <- postInfo(elem)
          _ <- IO.println(resp)
        } yield resp
      )
      .through(throttle(9, 1.second, Shaping))
      .compile
      .to(List)

    counted: Map[String, List[PostResponse]] = l
      .groupBy(_.tag_string_artist)
      .filter(_._2.size > 1)
      .map(x => x)

  } yield counted
}

object App extends IOApp.Simple {
  val run = for {
    pool <- Env[IO].get("DANBOORU_POOL").map(_.get)

    given DanbooruAuth <- for {
      login <- Env[IO].get("DANBOORU_USERNAME").map(_.get)
      api_key <- Env[IO].get("DANBOORU_APIKEY").map(_.get)
    } yield DanbooruAuth(login, api_key)

    _ <- client_res.use { implicit client =>
      for {
        favs <- favGroups(pool)
        authors <- commonAuthors(favs.post_ids)

        _ <- authors.toList.traverse { (author, responses) =>
          for {
            _ <- IO.println(author)
            _ <- responses.traverse { resp =>
              {
                val text = resp.file_url match
                  case Some(url) => url
                  case None      => "No preview"
                IO.println(s"\t ${resp.id} - ${text}")
              }
            }
          } yield ()
        }
      } yield ()
    }
  } yield ExitCode.Success
}
