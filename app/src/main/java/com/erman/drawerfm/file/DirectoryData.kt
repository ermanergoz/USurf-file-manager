import androidx.core.content.FileProvider
import java.util.*

data class DirectoryData(
    val path: String,
    val isFolder: Boolean, //If its true, it is a folder. Otherwise, it is a file.
    val name: String,
    val sizeInMB: Double,
    val subFileNum: Int = 0,
    val lastModifiedDate: Date
)



