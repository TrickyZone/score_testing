package com.knoldus.utils

object StringUtils {

  def upperCamelToLowerSnake(name: String) = "([a-z])([A-Z]+)".r.replaceAllIn(name, "$1_$2").toLowerCase

}
