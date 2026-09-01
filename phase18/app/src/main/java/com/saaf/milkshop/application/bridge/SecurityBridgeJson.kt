package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.application.readmodels.SecurityAuditDto
import com.saaf.milkshop.application.readmodels.UserAdministrationDto
import com.saaf.milkshop.data.AppUserEntity
import com.saaf.milkshop.application.readmodels.toAdministrationDto
import org.json.JSONObject

internal fun userAdministrationJson(value: UserAdministrationDto): JSONObject = JSONObject()
    .put("userId", value.userId.toString()).put("loginName", value.loginName).put("displayName", value.displayName)
    .put("roleCode", value.roleCode).putNullable("employeeAccountId", value.employeeAccountId?.toString())
    .put("statusCode", value.statusCode).put("setupRequired", value.setupRequired).put("createdAt", value.createdAt)
    .putNullable("createdByUserId", value.createdByUserId?.toString()).put("updatedAt", value.updatedAt)
    .putNullable("disabledAt", value.disabledAt).putNullable("disabledByUserId", value.disabledByUserId?.toString())

internal fun userAdministrationJson(value: AppUserEntity): JSONObject = userAdministrationJson(value.toAdministrationDto())

internal fun securityAuditJson(value: SecurityAuditDto): JSONObject = JSONObject()
    .put("auditId", value.auditId.toString()).put("occurredAt", value.occurredAt)
    .put("actionCode", value.actionCode).put("resultCode", value.resultCode).putNullable("reasonCode", value.reasonCode)
    .put("actor", JSONObject().putNullable("userId", value.actor.userId?.toString())
        .putNullable("displayName", value.actor.displayName).putNullable("roleCode", value.actor.roleCode))
    .putNullable("targetUserId", value.targetUserId?.toString()).putNullable("details", value.details)
