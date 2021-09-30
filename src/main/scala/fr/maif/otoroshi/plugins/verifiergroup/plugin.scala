package fr.maif.otoroshi.plugins.verifiergroup

import akka.stream.Materializer
import otoroshi.env.Env
import otoroshi.gateway.Errors
import otoroshi.models.RefJwtVerifier
import otoroshi.script._
import otoroshi.utils.syntax.implicits._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws.DefaultWSCookie
import play.api.mvc.{Result, Results}

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Luc Bonadé
 */
class VerifierGroupEmulator extends RequestTransformer {

  val logger = Logger("VerifierGroupEmulator")

  override def description: Option[String] =
    Some(
      """ This plug in emulate a JWT verifier, grouping verifier.
        |
        | the service descriptor plugin instance config has two value :
        |  - `strict` with default value to false, a flag to make this group JWT verifier strict
        |  - `groupId` the group Id, to lookuo the group members in the Plugin's global config
        |
        | the plug in global configuration has two value :
        |   - `strict`, an optional flag to change the default plugin's instance `strict` value witch defaukt is false
        |   - `groups`,  a map (Object), with the differents groupId as keys, and an array with verifier Id defining the group members
        |""".stripMargin)

  override def defaultConfig: Option[JsObject] =
    Some(
      Json.obj(
        "VerifierGroupEmulator" -> Json.obj(
          "groupId" -> "",
          "strict" -> false
        )
      )
    )

  override def configSchema: Option[JsObject] =
    Some(
      Json.obj(
        "groupId" -> Json.obj(
          "type" -> "string",
          "label" -> "the group name to lookup for the verifiers in the global config"
        ),
        "strict" -> Json.obj(
          "type" -> "boolean",
          "label" -> "is this verifier group strict"
        )

      )
    )

  override def transformRequestWithCtx
  (context: TransformerRequestContext)(implicit env: Env, ec: ExecutionContext, mat: Materializer)
  : Future[Either[Result, HttpRequest]] = {
    val groupId = (context.config \ "VerifierGroupEmulator" \ "groupId").as[String];
    val strict: Boolean = (context.config \ "VerifierGroupEmulator" \ "strict").asOpt[Boolean]
      .getOrElse((context.globalConfig \ "VerifierGroupEmulator" \ "strict").asOpt[Boolean].getOrElse(false))
    val groupVerifiersIdOpt = (context.globalConfig \ "VerifierGroupEmulator" \ "groups" \ groupId).asOpt[JsArray]
    logger.trace("strict : " + strict)

    groupVerifiersIdOpt match {
      case None if !strict => Right(context.otoroshiRequest).future
      case None if strict =>
        Errors
          .craftResponseResult(
            "Forbidden Invalid Token",
            Results.Forbidden,
            context.request,
            None,
            None,
            attrs = context.attrs
          ).map(Left.apply)
      case Some(jsGroupVerifiersId) => val groupVerifiersId = jsGroupVerifiersId.value.map(v => v.as[String])
        logger.trace("group Id : " + groupId)
        logger.trace("group verifiers Id : " + groupVerifiersId)
        groupVerifiersId match {
          case s if !strict && s.isEmpty => Right(context.otoroshiRequest).future
          case _ => RefJwtVerifier(groupVerifiersId, true, Seq.empty).verifyGen[HttpRequest](context.request, context.descriptor, context.apikey, context.user, context.attrs.get(otoroshi.plugins.Keys.ElCtxKey).get, context.attrs) {
            jwtInjection =>
              jwtInjection.decodedToken match {
                case None => Left(Results.Forbidden(Json.obj("error" -> "Forbidden JWT Token validation failed"))).future
                case Some(_) => Right(
                  context.otoroshiRequest.copy(
                    headers = (context.otoroshiRequest.headers ++ jwtInjection.additionalHeaders)
                      .filter(entry => jwtInjection.removeHeaders.contains(entry._1)),
                    cookies = (context.otoroshiRequest.cookies ++ jwtInjection.additionalCookies
                      .map(entry => DefaultWSCookie(entry._1, entry._2)))
                      .filter(entry => jwtInjection.removeCookies.contains(entry.name))
                  )
                ).future
              }
          }.map(x => x match {
            case Left(_) => Left(Results.Forbidden(Json.obj("error" -> "Forbidden JWT Token validation failed")))
            case x => x
          })
        }
    }
  }
}

//new VerifierGroupEmulator()