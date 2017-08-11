package com.cqrs.cloud.domain

final case class AddUserCmd(user: User)
final case class UserAddedResp(user: User)
final case class UserExistsResp(user: User)
