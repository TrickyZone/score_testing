package com.knoldus.statement

import com.knoldus.utils.StringUtils.upperCamelToLowerSnake

trait StatementGenerator[A] {
  def select(table: String): String

  def selectById(table: String, pk: Long): String

  def insert(table: String): String

  def remove(table: String, pk: Long): String

  def parsert(entity: A): Map[String, Any]
}

object StatementGenerator {

  def apply[A](implicit sg: StatementGenerator[A]): StatementGenerator[A] = sg

  implicit def genericGenerator[A](implicit fieldLister: FieldLister[A]): StatementGenerator[A] = new StatementGenerator[A] {

    override def select(table: String): String = {
      val fields = fieldLister.list.map(upperCamelToLowerSnake).mkString(",")
      s"select $fields from $table"
    }

    def selectById(table: String, pk: Long): String = {
      val fields = fieldLister.list.map(upperCamelToLowerSnake).mkString(",")
      s"select $fields from $table where id = $pk"
    }

    override def insert(table: String): String = {
      val fieldNames = fieldLister.list.map(upperCamelToLowerSnake)
      val fields = fieldNames.mkString(",")

      val placeholders = List.fill(fieldNames.size)("?").mkString(",")

      s"insert into $table($fields) values ($placeholders)"
    }

    override def remove(table: String, pk: Long): String = {
      s"delete from $table where id = $pk"
    }

    override def parsert(entity: A): Map[String, Any] = fieldLister.map(entity)
  }

}