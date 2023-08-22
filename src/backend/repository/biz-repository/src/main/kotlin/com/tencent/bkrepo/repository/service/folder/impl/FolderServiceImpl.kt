/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2023 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.repository.service.folder.impl

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.repository.dao.FavoriteDao
import com.tencent.bkrepo.repository.model.TFavorites
import com.tencent.bkrepo.repository.pojo.favorite.FavoriteCreateRequset
import com.tencent.bkrepo.repository.pojo.favorite.FavoritePageRequest
import com.tencent.bkrepo.repository.pojo.favorite.FavoriteRequest
import com.tencent.bkrepo.repository.service.folder.FolderService
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service

@Service
class FolderServiceImpl(
    private val favoriteDao: FavoriteDao,
) :FolderService{

    override fun createFavorite(favoriteRequest: FavoriteCreateRequset) {
        val favorite = TFavorites(
            path = favoriteRequest.path,
            repoId = favoriteRequest.repoId,
            projectId = favoriteRequest.projectId,
            userId = favoriteRequest.userId,
            createdDate = favoriteRequest.createdDate
        )
        favoriteDao.insert(favorite)
    }

    override fun modifyFavorite(before: FavoriteRequest, after: FavoriteRequest) {
        favoriteDao.updateFirst(
            Query.query(
                Criteria.where("path").`is`(before.path).
                and("projectId").`is`(before.projectId).
                and("repoId").`is`(before.repoId)),
            Update.update("repoId", after.repoId).
                set("projectId", after.projectId).
                set("path", after.path)
        )
    }

    override fun pageFavorite(favoritePageRequest: FavoritePageRequest): Page<TFavorites> {
       with(favoritePageRequest) {
           val query = Query()
           projectId?.let { query.addCriteria(Criteria.where("prokectId").`is`(projectId)) }
           repoId?.let { query.addCriteria(Criteria.where("repoId").`is`(repoId)) }
           val records = favoriteDao.find(query)
           val pageRequest = Pages.ofRequest(pageNumber, pageSize)
           val totalRecords = favoriteDao.count(query)
           return Pages.ofResponse(pageRequest, totalRecords, records)
        }
    }

    override fun removeFavorite(id: String) {
       favoriteDao.remove(Query.query(Criteria.where("_id").`is`(id)))
    }
}