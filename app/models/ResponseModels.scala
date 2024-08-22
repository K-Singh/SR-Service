package models

import play.api.libs.json._

object ResponseModels {



  case class WorkMessage(msg: String,
                         b: BigInt,
                         h: Long,
                         pk: String)

  implicit val readsWorkMessage: Reads[WorkMessage] = new Reads[WorkMessage] {
    override   def reads(json: JsValue): JsResult[WorkMessage] = {
      val msg = (json \ "msg").as[String]
      val b = (json \ "b").as[BigInt]
      val h = (json \ "h").as[Long]
      val pk = (json \ "pk").as[String]
      JsSuccess(WorkMessage(msg, b, h, pk))
    }
  }

  implicit val writesWorkMessage: Writes[WorkMessage] = new Writes[WorkMessage] {
    override def writes(o: WorkMessage): JsValue = {
      Json.obj( fields =
        "msg" -> o.msg,
        "b" -> o.b,
        "h" -> o.h,
        "pk" -> o.pk
      )
    }
  }

}
