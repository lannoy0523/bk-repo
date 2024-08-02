/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.common.ratelimiter.service

import com.tencent.bkrepo.common.ratelimiter.RateLimiterAutoConfiguration
import com.tencent.bkrepo.common.ratelimiter.config.RateLimiterProperties
import com.tencent.bkrepo.common.ratelimiter.service.bandwidth.DownloadBandwidthRateLimiterService
import com.tencent.bkrepo.common.ratelimiter.service.bandwidth.UploadBandwidthRateLimiterService
import com.tencent.bkrepo.common.ratelimiter.service.url.UrlRateLimiterService
import com.tencent.bkrepo.common.ratelimiter.service.url.user.UserUrlRateLimiterService
import com.tencent.bkrepo.common.ratelimiter.service.usage.DownloadUsageRateLimiterService
import com.tencent.bkrepo.common.ratelimiter.service.usage.UploadUsageRateLimiterService
import com.tencent.bkrepo.common.ratelimiter.service.usage.user.UserDownloadUsageRateLimiterService
import com.tencent.bkrepo.common.ratelimiter.service.usage.user.UserUploadUsageRateLimiterService
import com.tencent.bkrepo.common.ratelimiter.stream.CommonRateLimitInputStream
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.io.InputStream
import javax.servlet.http.HttpServletRequest

@Component
class RequestLimitCheckService(
    private val rateLimiterProperties: RateLimiterProperties,
    @Qualifier(RateLimiterAutoConfiguration.URL_RATELIMITER_SERVICE)
    private val urlRateLimiterService: UrlRateLimiterService,
    @Qualifier(RateLimiterAutoConfiguration.UPLOAD_USAGE_RATELIMITER_SERVICE)
    private val uploadUsageRateLimiterService: UploadUsageRateLimiterService,
    @Qualifier(RateLimiterAutoConfiguration.USER_URL_RATELIMITER_SERVICE)
    private val userUrlRateLimiterService: UserUrlRateLimiterService,
    @Qualifier(RateLimiterAutoConfiguration.USER_UPLOAD_USAGE_RATELIMITER_SERVICE)
    private val userUploadUsageRateLimiterService: UserUploadUsageRateLimiterService,
    @Qualifier(RateLimiterAutoConfiguration.DOWNLOAD_USAGE_RATELIMITER_SERVICE)
    private val downloadUsageRateLimiterService: DownloadUsageRateLimiterService,
    @Qualifier(RateLimiterAutoConfiguration.USER_DOWNLOAD_USAGE_RATELIMITER_SERVICE)
    private val userDownloadUsageRateLimiterService: UserDownloadUsageRateLimiterService,
    @Qualifier(RateLimiterAutoConfiguration.DOWNLOAD_BANDWIDTH_RATELIMITER_SERVICE)
    private val downloadBandwidthRateLimiterService: DownloadBandwidthRateLimiterService,
    @Qualifier(RateLimiterAutoConfiguration.UPLOAD_BANDWIDTH_RATELIMITER_ERVICE)
    private val uploadBandwidthRateLimiterService: UploadBandwidthRateLimiterService,
) {

    fun preLimitCheck(request: HttpServletRequest) {
        if (!rateLimiterProperties.enabled) {
            return
        }
        // TODO 可以优化
        userUrlRateLimiterService.limit(request)
        userUploadUsageRateLimiterService.limit(request)
        urlRateLimiterService.limit(request)
        uploadUsageRateLimiterService.limit(request)
    }

    fun postLimitCheck(request: HttpServletRequest, applyPermits: Long) {
        if (!rateLimiterProperties.enabled) {
            return
        }
        downloadUsageRateLimiterService.limit(request, applyPermits)
        userDownloadUsageRateLimiterService.limit(request, applyPermits)

    }

    fun bandwidthCheck(request: HttpServletRequest, inputStream: InputStream): CommonRateLimitInputStream? {
        if (!rateLimiterProperties.enabled) {
            return null
        }
        return downloadBandwidthRateLimiterService.bandwidthRateLimit(request, inputStream)
    }

    fun uploadBandwidthCheck(request: HttpServletRequest, applyPermits: Long) {
        if (!rateLimiterProperties.enabled) {
            return
        }
        uploadBandwidthRateLimiterService.bandwidthRateLimit(request, applyPermits)
    }
}