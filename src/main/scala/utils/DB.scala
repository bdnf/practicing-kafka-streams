package utils

import java.util.Date

object DB {

  def save(purchaseDate: Long, employeeId: Option[String], itemPurchased: Option[String]) = {
    println(s" Employee with $employeeId purchased $itemPurchased on $purchaseDate")
  }

}
