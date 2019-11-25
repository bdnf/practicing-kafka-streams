package models

import java.time.Instant
import java.util.{Calendar, Date}

case class PurchasePattern(purchase: Purchase){

  val zipCode = purchase.zipCode
  val item = purchase.itemPurchased
  val date = purchase.purchaseDate
  val amount = purchase.price * purchase.quantity

}

case class RewardAccumulator(purchase: Purchase) {
  val customerId = purchase.lastName + "," + purchase.firstName
  val purchaseTotal = purchase.price * purchase.quantity
  val rewardPoints = purchaseTotal
}

case class Purchase(
                var firstName: Option[String] = None,
                var lastName: Option[String] = None,
                private var customerId: Option[String] = None,
                private var creditCardNumber: Option[String] = None,
                var itemPurchased: Option[String] = None,
                var department: Option[String] = None,
                var employeeId: Option[String] = None,
                var quantity: Int = 0,
                var price: Int = 0,
                var purchaseDate: Long = Instant.now.getEpochSecond,
                var zipCode:Option[String] = None,
                private var storeId: Option[String] = None) {

                //extends CustomDeserializer[Purchase] {

  private final val CC_NUMBER_REPLACEMENT = "xxxx-xxxx-xxxx-"

  def maskCreditCard(): Purchase = {

    creditCardNumber match {
      case None => throw new SecurityException("Credit Card can't be null")
      case Some(ccNumber) =>
        val parts: Array[String] = ccNumber.split("-")
        if (parts.length < 4) {
          creditCardNumber = Some("xxxx")
        }
        else {
          val last4Digits: String = ccNumber.split("-")(3)
          creditCardNumber = Some(CC_NUMBER_REPLACEMENT + last4Digits)
        }
        return this
    }
  }


}
