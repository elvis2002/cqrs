package com.cqrs.cloud.util

import com.zaxxer.hikari.HikariDataSource

class DatabaseService(jdbcUrl: String, dbUser: String, dbPassword: String) {
	private val ds = new HikariDataSource()
	ds.setMaximumPoolSize(20)
	ds.setDriverClassName("org.mariadb.jdbc.Driver")
	ds.setJdbcUrl(jdbcUrl)
	ds.addDataSourceProperty("user", dbUser)
	ds.addDataSourceProperty("password", dbPassword)
	//ds.setAutoCommit(false)

	val driver = slick.driver.MySQLDriver

	import driver.api._
	val db = Database.forDataSource(ds)
	implicit val dbSession = db.createSession()
	
	ds.getConnection.createStatement().execute("CREATE DATABASE IF NOT EXISTS cqrs")
}
