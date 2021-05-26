package com.muxixyz.ccnubox.main.data.database

import androidx.room.*
import com.muxixyz.ccnubox.main.data.domain.Schedule
import java.text.DateFormat.*
import java.util.*

@Entity
data class DatabaseSchedule(
    @PrimaryKey
    var id: String,
    var title: String,
    var content: String?,
    var isInterval: Boolean, // false 为时间点，true 为时间段。时间点类型的只有一个 startTime
    var startTime: Long?,
    var endTime: Long?,
    var repeatMode: Int, // repeat_mode(重复规则) 0表示不重复，1表示基于日历规则重复，2表示基于当前学期周数重复
    var cron: String?, // 重复的 cron 表达式
    var kind: Int, // 0 是日常，1 是待办
    var priority: Int, // 优先级 0 1 2 3 优先级依次递增
    var done: Boolean,
    var categoryId: Int?, // 分类 id
    var cellColorId: Int?, // 颜色 id，随机生成一个 0-10 之间的数即可。用于去色板里获取格子颜色
    var createdAt: Long,
    var updatedAt: Long?,
    var sortKey: String? // 用于排序的 key，因为其他产品待办列表里都可以自己调整顺序，所以需要给一个字段。初始值可以给毫秒级时间戳
)

fun List<DatabaseSchedule>.asDomainModel(): List<Schedule> {
    return map {
        it.asDomainModel()
    }
}

fun DatabaseSchedule.asDomainModel(): Schedule {
    return let {
        Schedule(
            id = it.id,
            title = it.title,
            content = it.content,
            isInterval = it.isInterval,
            startTime = if (it.startTime == null) null else Calendar.getInstance()
                .also { calendar -> calendar.timeInMillis = it.startTime!! },
            endTime = if (it.endTime == null) null else Calendar.getInstance()
                .also { calendar -> calendar.timeInMillis = it.endTime!! },
            repeatMode = it.repeatMode,
            cron = it.cron,
            kind = it.kind,
            priority = it.priority,
            done = it.done,
            categoryId = it.categoryId,
            cellColorId = it.cellColorId,
            createdAt = Calendar.getInstance()
                .also { calendar -> calendar.timeInMillis = it.createdAt },
            updatedAt = if (it.updatedAt == null) null else Calendar.getInstance()
                .also { calendar -> calendar.timeInMillis = it.updatedAt!! },
            sortKey = it.sortKey
        )
    }
}

@Dao
interface ScheduleDao {

    @Query("select * from databaseschedule")
    suspend fun getSchedules(): List<DatabaseSchedule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: DatabaseSchedule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<DatabaseSchedule>)

    @Query("SELECT * FROM databaseschedule WHERE id = :scheduleId")
    suspend fun getScheduleById(scheduleId: String): DatabaseSchedule?

    @Update
    suspend fun updateSchedule(schedule: DatabaseSchedule)

    @Query("UPDATE databaseschedule SET kind = :to WHERE id IN (:ids) AND kind LIKE :from")
    suspend fun changeSchedulesKind(ids: List<String>, from: Int, to: Int)

    @Query("UPDATE databaseschedule SET done = :done WHERE id = :scheduleId")
    suspend fun updateDone(scheduleId: String, done: Boolean)

    @Query("delete from databaseschedule")
    fun deleteAllSchedules()

    @Query("delete from databaseschedule where id = :scheduleId")
    suspend fun deleteScheduleById(scheduleId: String)
}