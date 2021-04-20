package controllers

import javax.inject._
import play.api._
import play.api.mvc._

@Singleton
class DebugReadMemoryController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  def read() = Action { implicit request: Request[AnyContent] =>
    request.getQueryString("address") match {
      case Some(addressString) => addressString.toIntOption match {
        case Some(address) => Ok(s"${address & 0xFF}")
        case None => BadRequest("Invalid memory address")
      }
      case None => BadRequest("Invalid memory address")
    }
  }
}
