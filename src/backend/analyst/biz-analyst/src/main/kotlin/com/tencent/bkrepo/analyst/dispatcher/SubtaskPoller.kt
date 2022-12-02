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

package com.tencent.bkrepo.analyst.dispatcher

import com.alibaba.cola.statemachine.StateMachine
import com.tencent.bkrepo.analyst.pojo.SubScanTask
import com.tencent.bkrepo.analyst.service.ScanService
import com.tencent.bkrepo.analyst.service.TemporaryScanTokenService
import com.tencent.bkrepo.analyst.statemachine.subtask.SubtaskEvent
import com.tencent.bkrepo.analyst.statemachine.subtask.SubtaskEvent.DISPATCH_FAILED
import com.tencent.bkrepo.analyst.statemachine.subtask.context.DispatchFailedContext
import com.tencent.bkrepo.analyst.statemachine.subtask.context.SubtaskContext
import com.tencent.bkrepo.common.analysis.pojo.scanner.SubScanTaskStatus
import com.tencent.bkrepo.common.analysis.pojo.scanner.SubScanTaskStatus.PULLED
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

open class SubtaskPoller(
    private val dispatcher: SubtaskDispatcher,
    private val scanService: ScanService,
    private val temporaryScanTokenService: TemporaryScanTokenService,
    private val subtaskStateMachine: StateMachine<SubScanTaskStatus, SubtaskEvent, SubtaskContext>
) {

    init {
        val runnable = {
            try {
                dispatch()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                // 执行的任务抛异常后scheduler将不会继续执行后续任务，且没有任何提示，需要手动catch异常输出日志
                logger.error("dispatch subtask failed", e)
            }
        }
        scheduler.scheduleAtFixedRate(runnable, POLL_INITIAL_DELAY, POLL_DELAY, TimeUnit.MILLISECONDS)
    }

    private fun dispatch() {
        var subtask: SubScanTask?
        // 不加锁，允许少量超过执行器的资源限制
        for (i in 0 until dispatcher.availableCount()) {
            // TODO 根据任务元数据选择分发到哪个集群
            subtask = scanService.pull() ?: break
            subtask.token = temporaryScanTokenService.createToken(subtask.taskId)
            if (!dispatcher.dispatch(subtask)) {
                // 分发失败，放回队列中
                logger.warn("dispatch subtask failed, subtask[${subtask.taskId}]")
                subtaskStateMachine.fireEvent(PULLED, DISPATCH_FAILED, DispatchFailedContext(subtask))
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SubtaskPoller::class.java)
        private val scheduler = Executors.newSingleThreadScheduledExecutor {
            Thread(it, "analyst-subtask-poller")
        }
        private const val POLL_INITIAL_DELAY = 30000L
        private const val POLL_DELAY = 5000L
    }
}
