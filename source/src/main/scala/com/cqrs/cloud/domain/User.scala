package com.cqrs.cloud.domain

final case class User(email: String, firstName: String, lastName: String) {
	require(!email.isEmpty, "email.empty")
}
