package com.bkk.sm.mongo.customers.resources

data class CompanyWithAdminResource(
    var companyResource: CompanyResource,
    var userResource: UserResource
)