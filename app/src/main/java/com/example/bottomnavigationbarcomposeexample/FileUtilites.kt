package com.example.bottomnavigationbarcomposeexample
import android.os.Environment
import android.util.Log
import java.io.File

fun getSaveFolder(subFolder: String = ""):File{
    val documentFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
    val resultsFolder = File(documentFolder, "BLESensorData/" + subFolder)
    resultsFolder.mkdir()
    return resultsFolder
}
public fun generateNewFile(fileName: String): File {
    val file = File("${getSaveFolder().absolutePath}/$fileName")
    file.createNewFile()
    return file
}

fun generateAndAppend(fileName: String, text: String, header: String = "", subFolder: String = ""){
    val file = File("${getSaveFolder(subFolder).absolutePath}/$fileName")

    if (!file.exists()){
        file.createNewFile()
        file.appendText(header)
    }
    file.appendText(text = text)
}
