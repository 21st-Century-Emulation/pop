package controllers

import javax.inject._
import play.api._
import play.api.mvc._

@Singleton
class StatusController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  def status() = Action { implicit request: Request[AnyContent] =>
    Ok("Healthy")
  }
}
