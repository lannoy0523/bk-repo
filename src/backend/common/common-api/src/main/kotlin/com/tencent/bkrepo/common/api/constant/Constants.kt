package com.tencent.bkrepo.common.api.constant

/**
 * 认证成功后username写入request attributes的key
 */
const val USER_KEY = "userId"

/**
 * 认证成功后appId写入request attributes的key
 */
const val APP_KEY = "appId"

/**
 * 微服务调用请求标记key
 */
const val MS_REQUEST_KEY = "MSRequest"

/**
 * 匿名用户
 */
const val ANONYMOUS_USER = "anonymous"

/**
 * header user id key
 */
const val AUTH_HEADER_UID: String = "X-BKREPO-UID"

/**
 * micro service header user id key
 */
const val MS_AUTH_HEADER_UID: String = "X-BKREPO-MS-UID"

/**
 * common logger name
 */
const val SYSTEM_ERROR_LOGGER_NAME = "SystemErrorLogger"
const val BUSINESS_ERROR_LOGGER_NAME = "BusinessErrorLogger"
const val JOB_LOGGER_NAME = "JobLogger"
const val API_LOGGER_NAME = "ApiLogger"
