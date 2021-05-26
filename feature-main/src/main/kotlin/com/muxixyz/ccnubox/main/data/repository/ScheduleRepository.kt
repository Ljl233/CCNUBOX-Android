package com.muxixyz.ccnubox.main.data.repository

import com.muxixyz.android.iokit.Result
import com.muxixyz.ccnubox.main.data.database.ScheduleLocalRepo
import com.muxixyz.ccnubox.main.data.domain.Schedule
import com.muxixyz.ccnubox.main.data.network.ScheduleRemoteRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap


class ScheduleRepository(
    private val scheduleLocal: ScheduleLocalRepo,
    private val scheduleRemote: ScheduleRemoteRepo
) {

    private var cachedSchedules: ConcurrentMap<String, Schedule>? = null

    suspend fun getSchedules(forceUpdate: Boolean): Result<List<Schedule>> {
        return withContext(Dispatchers.IO) {
            // Respond immediately with cache if available and not dirty
            if (!forceUpdate) {
                cachedSchedules?.let { cachedSchedules ->
                    return@withContext Result.Success(cachedSchedules.values.sortedBy { it.id })
                }
            }

            val newSchedules = fetchSchedulesFromRemoteOrLocal(forceUpdate)

            // Refresh the cache with the new schedules
            (newSchedules as? Result.Success)?.let { refreshCache(it.data) }

            cachedSchedules?.values?.let { schedules ->
                return@withContext Result.Success(schedules.sortedBy { it.id })
            }

            (newSchedules as? Result.Success)?.let {
                if (it.data.isEmpty()) {
                    return@withContext Result.Success(it.data)
                }
            }

            return@withContext Result.Error(Exception("Illegal state"))
        }
    }

    private suspend fun fetchSchedulesFromRemoteOrLocal(forceUpdate: Boolean): Result<List<Schedule>> {
        // Remote first
        val remoteSchedules = scheduleRemote.getAllSchedules()
        when (remoteSchedules) {
            is Result.Error -> ("Remote data source fetch failed")
            is Result.Success -> {
                refreshLocalDataSource(remoteSchedules.data)
                return remoteSchedules
            }
            else -> throw IllegalStateException()
        }

        // Don't read from local if it's forced
        if (forceUpdate) {
            return Result.Error(Exception("Can't force refresh: remote data source is unavailable"))
        }

        // Local if remote fails
        val localSchedules = scheduleLocal.getAllSchedules()
        if (localSchedules is Result.Success) return localSchedules
        return Result.Error(Exception("Error fetching from remote and local"))
    }

    private suspend fun refreshLocalDataSource(schedules: List<Schedule>) {
        scheduleLocal.deleteAllSchedules()
        scheduleLocal.addSchedules(schedules)
    }

    private fun refreshCache(schedules: List<Schedule>) {
        cachedSchedules?.clear()
        schedules.sortedBy { it.id }.forEach {
            cacheAndPerform(it) {}
        }
    }

    private fun cacheSchedule(schedule: Schedule): Schedule {
        val cachedSchedule = Schedule(
            schedule.id,
            schedule.title,
            schedule.content,
            schedule.isInterval,
            schedule.startTime,
            schedule.endTime,
            schedule.repeatMode,
            schedule.cron,
            schedule.kind,
            schedule.priority,
            schedule.done,
            schedule.categoryId,
            schedule.cellColorId,
            schedule.createdAt,
            schedule.updatedAt,
            schedule.sortKey
        )
        // Create if it doesn't exist.
        if (cachedSchedules == null) {
            cachedSchedules = ConcurrentHashMap()
        }
        cachedSchedules?.put(cachedSchedule.id, cachedSchedule)
        return cachedSchedule
    }

    private inline fun cacheAndPerform(schedule: Schedule, perform: (Schedule) -> Unit) {
        val cachedSchedule = cacheSchedule(schedule)
        perform(cachedSchedule)
    }

    suspend fun addSchedule(schedule: Schedule) {
        // Do in memory cache update to keep the app UI up to date
        cacheAndPerform(schedule) {
            coroutineScope {
//                launch { scheduleRemote.addSchedule(it) }
                launch { scheduleLocal.addSchedule(it) }
            }
        }
    }

    private suspend fun completeSchedule(schedule: Schedule) {
        // Do in memory cache update to keep the app UI up to date
        cacheAndPerform(schedule) {
            it.done = true
            coroutineScope {
                launch { scheduleRemote.updateScheduleDone(it.id, true) }
                launch { scheduleLocal.updateScheduleDone(it.id, true) }
            }
        }
    }

    suspend fun completeSchedule(scheduleId: String) {
        withContext(Dispatchers.IO) {
            getScheduleById(scheduleId)?.let {
                completeSchedule(it)
            }
        }
    }

    suspend fun deleteAllSchedules() {
        withContext(Dispatchers.IO) {
            coroutineScope {
                launch { scheduleRemote.deleteAllSchedules() }
                launch { scheduleLocal.deleteAllSchedules() }
            }
        }
        cachedSchedules?.clear()
    }

    suspend fun deleteSchedule(scheduleId: String) {
        coroutineScope {
            launch { scheduleRemote.deleteSchedule(scheduleId) }
            launch { scheduleLocal.deleteSchedule(scheduleId) }
        }

        cachedSchedules?.remove(scheduleId)
    }

    private fun getScheduleById(id: String) = cachedSchedules?.get(id)

    interface LoadSchedulesCallback {
        fun onSchedulesLoaded(schedules: List<Schedule>)
        fun onDataNotAvailable(message: String)
    }
}
