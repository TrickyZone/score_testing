package com.knoldus

import com.knoldus.common.Config
import pureconfig.ConfigSource
import pureconfig.generic.auto._

package object dbTest {

  val DefaultConfig: Config =  ConfigSource.default.load[Config] match {
    case Right(conf) => conf
    case Left(error) => throw new Exception(error.toString())
  }

}
