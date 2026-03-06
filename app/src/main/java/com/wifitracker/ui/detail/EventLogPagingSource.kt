package com.wifitracker.ui.detail

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.wifitracker.data.local.dao.EventDao
import com.wifitracker.domain.model.EventType
import com.wifitracker.domain.model.WifiEvent

class EventLogPagingSource(
    private val eventDao: EventDao,
    private val trackerId: Long
) : PagingSource<Int, WifiEvent>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, WifiEvent> {
        return try {
            val page = params.key ?: 0
            val pageSize = params.loadSize

            // Get events using Room's PagingSource
            val pagingSource = eventDao.getEventsPaged(trackerId)
            val result = pagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = page,
                    loadSize = pageSize,
                    placeholdersEnabled = false
                )
            )

            when (result) {
                is LoadResult.Page -> {
                    val events = result.data.mapIndexed { index, entity ->
                        // Determine if editable based on pairing
                        val isEditable = if (entity.eventType == "CONNECT") {
                            // Check if next event is DISCONNECT
                            val nextIndex = index + 1
                            nextIndex < result.data.size && result.data[nextIndex].eventType == "DISCONNECT"
                        } else {
                            // DISCONNECT is editable if previous is CONNECT
                            val prevIndex = index - 1
                            prevIndex >= 0 && result.data[prevIndex].eventType == "CONNECT"
                        }

                        WifiEvent(
                            id = entity.id,
                            trackerId = entity.trackerId,
                            eventType = EventType.valueOf(entity.eventType),
                            timestamp = entity.timestamp,
                            isEditable = isEditable
                        )
                    }

                    LoadResult.Page(
                        data = events,
                        prevKey = if (page > 0) page - 1 else null,
                        nextKey = if (events.size == pageSize) page + 1 else null
                    )
                }
                is LoadResult.Error -> LoadResult.Error(result.throwable)
                is LoadResult.Invalid -> LoadResult.Page(
                    data = emptyList(),
                    prevKey = null,
                    nextKey = null
                )
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, WifiEvent>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
