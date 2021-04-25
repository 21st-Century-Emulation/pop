package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.ws.WSClient
import scala.concurrent.ExecutionContext
import scala.util.Success
import scala.util.Failure
import scala.concurrent.Future

case class CpuFlags(var sign: Boolean, var zero: Boolean, var auxCarry: Boolean, var parity: Boolean, var carry: Boolean)

case class CpuState(var a: Int, var b: Int, var c: Int, var d: Int, var e: Int, var h: Int, var l: Int, var stackPointer: Int, programCounter: Int, var cycles: Long, flags: CpuFlags, interruptsEnabled: Boolean)

case class Cpu(opcode: Int, id: String, state: CpuState)

object Cpu {
  implicit val formatCpuFlags: Format[CpuFlags] = Json.format[CpuFlags]
  implicit val formatCpuState: Format[CpuState] = Json.format[CpuState]
  implicit val formatCpu: Format[Cpu] = Json.format[Cpu]
}

@Singleton
class ExecuteController @Inject() (val ws: WSClient, implicit val ec: ExecutionContext, val controllerComponents: ControllerComponents) extends BaseController {
  private val readMemoryApi: String = sys.env.get("READ_MEMORY_API").get;

  def execute: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    request.body.asJson match {
      case None => Future { Ok("Body isn't json") }(ec)
      case Some(json) => json.asOpt[Cpu] match {
        case Some(cpu) => {
          val highByteFuture = ws.url(this.readMemoryApi).withQueryStringParameters(("address", s"${cpu.state.stackPointer}"), ("id", cpu.id)).get().map(response => response.body.toInt);
          val lowByteFuture = ws.url(this.readMemoryApi).withQueryStringParameters(("address", s"${cpu.state.stackPointer + 1}"), ("id", cpu.id)).get().map(response => response.body.toInt);

          val combinedFuture = for {
            hb <- highByteFuture
            lb <- lowByteFuture
          } yield (hb, lb)

            combinedFuture.map { case (highByte, lowByte) => {
              cpu.state.cycles += 10;
              cpu.state.stackPointer += 2;
              cpu.opcode match {
                case 0xC1 => {
                  cpu.state.b = highByte;
                  cpu.state.c = lowByte;
                  Ok(Json.toJson(cpu))
                }
                case 0xD1 => {
                  cpu.state.d = highByte;
                  cpu.state.e = lowByte;
                  Ok(Json.toJson(cpu))
                }
                case 0xE1 => {
                  cpu.state.h = highByte;
                  cpu.state.l = lowByte;
                  Ok(Json.toJson(cpu))
                }
                case 0xF1 => {
                  cpu.state.a = highByte;
                  cpu.state.flags.sign = (lowByte & 0x80) == 0x80;
                  cpu.state.flags.zero = (lowByte & 0x40) == 0x40;
                  cpu.state.flags.auxCarry = (lowByte & 0x10) == 0x10;
                  cpu.state.flags.parity = (lowByte & 0x04) == 0x04;
                  cpu.state.flags.carry = (lowByte & 0x01) == 0x01;
                  Ok(Json.toJson(cpu))
                }
                case _ => BadRequest("Invalid opcode")
              }
            }
          }
        }
        case None => Future { Ok("Body isn't json") }(ec)
      }
    }
  }
}
