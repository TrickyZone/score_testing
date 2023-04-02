package com.knoldus.dbTest

import com.knoldus.common.DatabaseConfig
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.typesafe.scalalogging.StrictLogging
import org.postgresql.jdbc.PgConnection
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

/** Base trait for tests which use the database. The database is cleaned after each test.
  */
trait TestEmbeddedPostgres extends BeforeAndAfterEach with BeforeAndAfterAll with StrictLogging { self: Suite =>
  private var postgres: EmbeddedPostgres = _
  private var currentDbConfig: DatabaseConfig = _
  var currentDb: TestDB = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    postgres = EmbeddedPostgres.builder().start()
    val url = postgres.getJdbcUrl("postgres")
    postgres.getPostgresDatabase.getConnection.asInstanceOf[PgConnection].setPrepareThreshold(100)
    currentDbConfig = DefaultConfig.dbConfig.copy(
      user = "postgres",
      password = "",
      url = url
    )
    currentDb = new TestDB(currentDbConfig)
    currentDb.testConnection()
    currentDb.migrate()
  }

  override protected def afterAll(): Unit = {
    postgres.close()
    currentDb.close()
    super.afterAll()
  }

  override protected def beforeEach(): Unit = {
    //currentDb.migrate()
    super.beforeEach()

  }

  override protected def afterEach(): Unit = {
    currentDb.truncateAllTables()
    super.afterEach()
  }
}
