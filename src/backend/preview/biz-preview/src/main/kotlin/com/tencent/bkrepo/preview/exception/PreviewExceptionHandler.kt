/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.preview.exception

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.LocaleMessageUtils
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.preview.pojo.PreviewInfo
import com.tencent.bkrepo.preview.service.FilePreview
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 预览统一异常处理
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RestControllerAdvice("com.tencent.bkrepo.preview")
class PreviewExceptionHandler {
    /**
     * 资源不存在
     */
    @ExceptionHandler(PreviewNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleException(exception: PreviewNotFoundException) : Response<PreviewInfo> {
        return previewResponse(exception)
    }

    /**
     * 文件处理异常
     */
    @ExceptionHandler(PreviewHandleException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleException(exception: PreviewHandleException) : Response<PreviewInfo> {
        return previewResponse(exception)
    }

    /**
     * 请求参数异常
     */
    @ExceptionHandler(PreviewInvalidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleException(exception: PreviewInvalidException) : Response<PreviewInfo> {
        return previewResponse(exception)
    }

    private fun previewResponse(exception: ErrorCodeException) : Response<PreviewInfo> {
        val previewInfo = PreviewInfo()
        val errorMessage = LocaleMessageUtils.getLocalizedMessage(exception.messageCode, exception.params)
        previewInfo.fileTemplate = FilePreview.NOT_SUPPORTED_FILE_PAGE
        previewInfo.msg = errorMessage
        return ResponseBuilder.build(exception.messageCode.getCode(), errorMessage ,previewInfo)
    }
}
