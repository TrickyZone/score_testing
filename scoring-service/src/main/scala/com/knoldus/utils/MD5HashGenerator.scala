package com.knoldus.utils

import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter

object MD5HashGenerator {


  def getMD5HashValue(value : String) : String = {
    val md = MessageDigest.getInstance("MD5")
    md.update(value.getBytes)
    val digest = md.digest
    DatatypeConverter.printHexBinary(digest).toUpperCase
  }


}
