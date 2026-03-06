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
                        // Determine if editable: only completed sessions (CONNECT + DISCONNECT pairs)
                        // Events are ordered DESC, so DISCONNECT comes before its CONNECT
                        val isEditable: Boolean
                        val minTimestamp: Long?
                        val maxTimestamp: Long?

                        if (entity.eventType == "DISCONNECT") {
                            // DISCONNECT is editable if next event (earlier time) is CONNECT
                            val nextIndex = index + 1
                            val pairedConnect = result.data.getOrNull(nextIndex)
                            isEditable = pairedConnect?.eventType == "CONNECT"
                            // New timestamp must be strictly after the paired CONNECT
                            minTimestamp = if (isEditable) pairedConnect?.timestamp else null
                            // New timestamp must be strictly before the event that follows this
                            // DISCONNECT (i.e. the CONNECT of the next, more recent session)
                            maxTimestamp = result.data.getOrNull(index - 1)?.timestamp
                        } else if (entity.eventType == "CONNECT") {
                            // CONNECT is editable if previous event (later time) is DISCONNECT
                            val prevIndex = index - 1
                            val pairedDisconnect = result.data.getOrNull(prevIndex)
                            isEditable = pairedDisconnect?.eventType == "DISCONNECT"
                            // New timestamp must be strictly after the event that precedes this
                            // CONNECT (i.e. the DISCONNECT of the previous, older session)
                            minTimestamp = result.data.getOrNull(index + 1)?.timestamp
                            // New timestamp must be strictly before the paired DISCONNECT
                            maxTimestamp = if (isEditable) pairedDisconnect?.timestamp else null
                        } else {
                            isEditable = false
                            minTimestamp = null
                            maxTimestamp = null
                        }

                        WifiEvent(
                            id = entity.id,
                            trackerId = entity.trackerId,
                            eventType = EventType.valueOf(entity.eventType),
                            timestamp = entity.timestamp,
                            isEditable = isEditable,
                            minTimestamp = minTimestamp,
                            maxTimestamp = maxTimestamp
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
