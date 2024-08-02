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

package com.tencent.bkrepo.common.ratelimiter.algorithm

import com.tencent.bkrepo.common.api.util.HumanReadable
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class SlidingWindowRateLimiterTest {

    @Test
    fun testTryAcquire() {
        val ratelimiter = SlidingWindowRateLimiter(5, 1, TimeUnit.SECONDS)
        val passed1 = ratelimiter.tryAcquire(1)
        Assertions.assertTrue(passed1)
        val passed2 = ratelimiter.tryAcquire(1)
        Assertions.assertTrue(passed2)
        val passed3 = ratelimiter.tryAcquire(1)
        Assertions.assertTrue(passed3)
        val passed4 = ratelimiter.tryAcquire(1)
        Assertions.assertTrue(passed4)
        val passed5 = ratelimiter.tryAcquire(1)
        Assertions.assertTrue(passed5)
        Thread.sleep(1000)
        var passed6 = ratelimiter.tryAcquire(1)
        Assertions.assertTrue(passed6)
        var passed7 = ratelimiter.tryAcquire(1)
        Assertions.assertTrue(passed7)
        val passed8 = ratelimiter.tryAcquire(1)
        Assertions.assertTrue(passed8)
    }

    @Test
    fun testTryAcquireOnMultiThreads() {
        val ratelimiter = SlidingWindowRateLimiter(5, 1, TimeUnit.SECONDS)
        var successNum = 0
        var failedNum = 0
        var errorNum = 0
        val readers = Runtime.getRuntime().availableProcessors()
        val countDownLatch = CountDownLatch(readers)
        val elapsedTime = measureTimeMillis {
            repeat(readers) {
                thread {
                    try {
                        val passed = ratelimiter.tryAcquire(1)
                        if (passed) {
                            successNum++
                        } else {
                            failedNum++
                        }
                    } catch (e: Exception) {
                        errorNum++
                    }
                    countDownLatch.countDown()
                }
            }
        }
        println("elapse: ${HumanReadable.time(elapsedTime, TimeUnit.MILLISECONDS)}")
        println("successNum $successNum, failedNum $failedNum. errorNum $errorNum")
        countDownLatch.await()
        Assertions.assertEquals(5, successNum)
        Assertions.assertEquals(readers - 5, failedNum)
        Assertions.assertEquals(0, errorNum)
    }
}