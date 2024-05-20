import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "cv_user")
data class User(
    @PrimaryKey val id: Int,
    val name: String,
    val email: String,
    val password: String
)


@Entity(
    tableName = "cv_activity_class"
)
data class ActivityClass(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String?
)


@Entity(
    tableName = "cv_material_class"
)
data class MaterialClass(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String?
)


@Entity(
    tableName = "cv_activity",
    foreignKeys = [
        ForeignKey(entity = ActivityClass::class, parentColumns = ["id"], childColumns = ["class_id"])
    ]
)
data class Activity(
    @PrimaryKey val id: Int,
    val class_id: Int,
    val scrs_timestamp: String,
    val scrs_path: String,
    val is_complete: Boolean,
    val result_conf: Float?,
    val result_json: String?,
    val speed_ms: Int,
    val comment: String?,
    val username: String?
)


@Entity(
    tableName = "cv_activity_mat",
    foreignKeys = [
        ForeignKey(entity = Activity::class, parentColumns = ["id"], childColumns = ["act_id"]),
        ForeignKey(entity = MaterialClass::class, parentColumns = ["id"], childColumns = ["mat_class_id"])
    ]
)
data class ActivityMat(
    @PrimaryKey val id: Int,
    val act_id: Int,
    val mat_class_id: Int,
    val coords: String,
    val conf: Float,
    val comment: String?
)


@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User)

    @Query("SELECT * FROM cv_user WHERE id = :id")
    suspend fun getUserById(id: Int): User?
}


@Dao
interface ActivityClassDao {
    @Insert
    suspend fun insert(activityClass: ActivityClass)

    @Query("SELECT * FROM cv_activity_class WHERE id = :id")
    suspend fun getActivityClassById(id: Int): ActivityClass?
}


@Dao
interface MaterialClassDao {
    @Insert
    suspend fun insert(materialClass: MaterialClass)

    @Query("SELECT * FROM cv_material_class WHERE id = :id")
    suspend fun getMaterialClassById(id: Int): MaterialClass?
}


@Dao
interface ActivityDao {
    @Insert
    suspend fun insert(activity: Activity)

    @Query("SELECT * FROM cv_activity WHERE id = :id")
    suspend fun getActivityById(id: Int): Activity?
}


@Dao
interface ActivityMatDao {
    @Insert
    suspend fun insert(activityMat: ActivityMat)

    @Query("SELECT * FROM cv_activity_mat WHERE id = :id")
    suspend fun getActivityMatById(id: Int): ActivityMat?
}


@Database(
    entities = [User::class, ActivityClass::class, MaterialClass::class, Activity::class, ActivityMat::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun activityClassDao(): ActivityClassDao
    abstract fun materialClassDao(): MaterialClassDao
    abstract fun activityDao(): ActivityDao
    abstract fun activityMatDao(): ActivityMatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "data.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

