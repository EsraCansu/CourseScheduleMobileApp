package com.coursescheduling.utils.parser

import android.content.Context
import android.net.Uri
import android.util.Log
import android.provider.OpenableColumns
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

data class ParsedCourse(
    var department: String = "",
    var lecturerName: String = "",
    var lecturerPassword: String = "",
    var email: String = "",
    var courseName: String = "",
    var courseCode: String = "",
    var courseClass: String = "",
    var classroomType: String = "",
    var day: String = "",
    var startTime: String = "",
    var endTime: String = "",
    var approvedStatus: String = "Approved",
    var notes: String = "",
    var duration: Int = 1,
    var capacity: Int = 0,
    var lecturerTitle: String = "",
    var isDuplicate: Boolean = false,
    var isInvalid: Boolean = false,
    var missingFields: List<String> = emptyList(),
    var validationError: String? = null
)

data class ImportSummary(
    val totalFound: Int = 0,
    val validRows: Int = 0,
    val invalidRows: Int = 0,
    val duplicateRows: Int = 0,
    val unmappedFields: List<String> = emptyList(),
    val parsedCourses: List<ParsedCourse> = emptyList()
)

object ImportEngine {
    fun parseFile(context: Context, uri: Uri): ImportSummary {
        val courses = mutableListOf<ParsedCourse>()
        Log.d("IMPORT_TRACE", "URI selected: $uri")
        
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri)
            Log.d("IMPORT_TRACE", "MIME type: $mimeType")

            val fileName = getFileName(context, uri)
            Log.d("IMPORT_TRACE", "Resolved FileName: $fileName")

            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e("IMPORT_TRACE", "FAILED to open input stream for URI")
                return ImportSummary()
            }
            Log.d("IMPORT_TRACE", "Stream opened successfully")

            val isCsv = mimeType?.contains("csv") == true || 
                        fileName?.endsWith(".csv", ignoreCase = true) == true ||
                        mimeType == "text/comma-separated-values"

            val unmappedFields = if (isCsv) {
                Log.d("IMPORT_TRACE", "Processing as CSV")
                parseCsv(inputStream, courses)
            } else {
                Log.d("IMPORT_TRACE", "Processing as EXCEL")
                parseExcel(inputStream, courses)
            }
            
            Log.d("FINAL_IMPORT_DEBUG", "Parsed ${courses.size} rows total. Starting final validation.")
            
            // Validation
            var valid = 0
            var skipped = 0
            
            courses.forEach { course ->
                if (course.lecturerName.isBlank() || course.courseCode.isBlank() || course.email.isBlank()) {
                    course.isInvalid = true
                    course.validationError = "Missing core fields (LecturerName/CourseCode/Email)"
                    skipped++
                    Log.w("FINAL_IMPORT_DEBUG", "Row SKIPPED: ${course.courseCode ?: "Unknown"} - Missing core fields")
                } else {
                    course.isInvalid = false
                    valid++
                    Log.d("FINAL_IMPORT_DEBUG", "Row VALID: ${course.courseCode} | Day: ${course.day} | Lecturer: ${course.lecturerName}")
                }
            }
            
            return ImportSummary(
                totalFound = courses.size,
                validRows = valid,
                invalidRows = skipped,
                duplicateRows = 0,
                unmappedFields = unmappedFields,
                parsedCourses = courses
            )
        } catch (e: Exception) {
            Log.e("IMPORT_TRACE", "CRITICAL ERROR during parse: ${e.message}", e)
            return ImportSummary(totalFound = 0)
        }
    }

    private val fieldMappings = mapOf(
        "department" to listOf("Department", "department", "dept"),
        "lecturerName" to listOf("LecturerName", "lecturername", "lecturer"),
        "lecturerPassword" to listOf("Password", "password", "lecturerpassword"),
        "email" to listOf("Email", "email", "lectureremail"),
        "courseName" to listOf("CourseName", "coursename", "course"),
        "courseCode" to listOf("CourseCode", "coursecode", "code"),
        "courseClass" to listOf("CourseClass", "courseclass", "class"),
        "classroomType" to listOf("ClassroomType", "classroomtype", "room"),
        "day" to listOf("CourseDay", "courseday", "day", "weekday"),
        "startTime" to listOf("CourseTime", "coursetime", "start", "starttime"),
        "endTime" to listOf("endTime", "end", "endtime"),
        "duration" to listOf("CourseDuration", "courseduration", "duration"),
        "capacity" to listOf("Capacity", "capacity", "size"),
        "lecturerTitle" to listOf("LecturerTitle", "lecturertitle", "title")
    )

    private fun getSmartMapping(rawHeaders: List<String>): Pair<Map<String, Int>, List<String>> {
        Log.d("FINAL_IMPORT_DEBUG", "RAW HEADERS: $rawHeaders")
        val normalizedHeaders = rawHeaders.map { normalize(it) }

        val mapping = mutableMapOf<String, Int>()
        val unmapped = rawHeaders.toMutableList()
        val mappedIndices = mutableSetOf<Int>()

        fieldMappings.forEach { (field, variants) ->
            for (variant in variants) {
                val normalizedVariant = normalize(variant)
                val index = rawHeaders.indexOfFirst { normalize(it) == normalizedVariant }
                if (index != -1 && index !in mappedIndices) {
                    mapping[field] = index
                    mappedIndices.add(index)
                    unmapped.remove(rawHeaders[index])
                    break
                }
            }
        }
        
        Log.d("FINAL_IMPORT_DEBUG", "MAPPED COLUMN DICTIONARY: $mapping")
        return mapping to unmapped
    }

    private fun normalize(header: String): String {
        return header.trim()
            .lowercase()
            .replace("_", "")
            .replace(" ", "")
            .replace("-", "")
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (name == null) {
            name = uri.path
            val cut = name?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                name = name?.substring(cut + 1)
            }
        }
        return name
    }

    private fun parseCsv(inputStream: InputStream, courses: MutableList<ParsedCourse>): List<String> {
        val reader = inputStream.bufferedReader()
        val lines = reader.readLines()
        if (lines.isEmpty()) {
            Log.w("IMPORT_TRACE", "CSV file is empty")
            return emptyList()
        }
        
        val rawHeaders = lines.first().split(",").map { it.trim() }
        Log.d("IMPORT_TRACE", "RAW CSV HEADERS: $rawHeaders")
        
        val (map, unmapped) = getSmartMapping(rawHeaders)
        
        for (i in 1 until lines.size) {
            val columns = lines[i].split(",").map { it.trim() }
            if (columns.isEmpty() || columns.all { it.isBlank() }) continue

            courses.add(ParsedCourse(
                department = getVal(columns, map, "department") ?: "",
                lecturerName = getVal(columns, map, "lecturerName") ?: "",
                lecturerPassword = getVal(columns, map, "lecturerPassword") ?: "",
                email = getVal(columns, map, "email") ?: "",
                courseName = getVal(columns, map, "courseName") ?: "",
                courseCode = getVal(columns, map, "courseCode") ?: "",
                courseClass = getVal(columns, map, "courseClass") ?: "",
                classroomType = getVal(columns, map, "classroomType") ?: "",
                day = getVal(columns, map, "day") ?: "",
                startTime = getVal(columns, map, "startTime") ?: "",
                endTime = getVal(columns, map, "endTime") ?: "",
                duration = getVal(columns, map, "duration")?.toIntOrNull() ?: 1,
                capacity = getVal(columns, map, "capacity")?.toIntOrNull() ?: 0,
                lecturerTitle = getVal(columns, map, "lecturerTitle") ?: ""
            ))
        }
        return unmapped
    }

    private fun getVal(cols: List<String>, map: Map<String, Int>, key: String): String? {
        val idx = map[key] ?: return null
        return cols.getOrNull(idx)
    }

    private fun parseExcel(inputStream: InputStream, courses: MutableList<ParsedCourse>): List<String> {
        val workbook = WorkbookFactory.create(inputStream)
        val sheet = workbook.getSheetAt(0)
        
        val headerRow = sheet.getRow(0) ?: return emptyList()
        val rawHeaders = mutableListOf<String>()
        for (cell in headerRow) {
            rawHeaders.add(cell.toString().trim())
        }
        Log.d("IMPORT_TRACE", "RAW EXCEL HEADERS: $rawHeaders")
        
        val (map, unmapped) = getSmartMapping(rawHeaders)
        
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            
            val rawDuration = getExcelVal(row, map, "duration") ?: ""
            val parsedDuration = extractNumericDuration(rawDuration)
            
            val rawTime = getExcelVal(row, map, "startTime") ?: ""
            val (startTime, endTime) = splitTimeRange(rawTime)

            val rawDay = getExcelVal(row, map, "day") ?: ""
            Log.d("FINAL_IMPORT_DEBUG", "Row ${rowIndex + 1} courseDay = '$rawDay'")
            
            val course = ParsedCourse(
                department = getExcelVal(row, map, "department") ?: "",
                lecturerName = getExcelVal(row, map, "lecturerName") ?: "",
                lecturerPassword = getExcelVal(row, map, "lecturerPassword") ?: "",
                email = getExcelVal(row, map, "email") ?: "",
                courseName = getExcelVal(row, map, "courseName") ?: "",
                courseCode = getExcelVal(row, map, "courseCode") ?: "",
                courseClass = getExcelVal(row, map, "courseClass") ?: "",
                classroomType = getExcelVal(row, map, "classroomType") ?: "",
                day = rawDay,
                startTime = startTime,
                endTime = endTime,
                duration = parsedDuration,
                capacity = getExcelVal(row, map, "capacity")?.toDouble()?.toInt() ?: 0,
                lecturerTitle = getExcelVal(row, map, "lecturerTitle") ?: "",
                isInvalid = false // Core fields check will happen in parseFile
            )
            
            Log.d("IMPORT_AUTH_DEBUG", "Parsed row object: $course")
            
            if (course.courseCode.isNotBlank() || course.courseName.isNotBlank()) {
                Log.d("CourseImportDebug", "Parsed row: ${course.courseCode} - ${course.courseName} (${course.startTime}-${course.endTime})")
                courses.add(course)
            }
        }
        workbook.close()
        return unmapped
    }

    private fun getExcelVal(row: org.apache.poi.ss.usermodel.Row, map: Map<String, Int>, key: String): String? {
        val idx = map[key] ?: return null
        return row.getCell(idx)?.toString()?.trim()
    }

    private fun extractNumericDuration(input: String): Int {
        if (input.isBlank()) return 1
        // Extract first number found
        val regex = Regex("\\d+")
        val match = regex.find(input)
        return match?.value?.toIntOrNull() ?: 1
    }

    private fun splitTimeRange(input: String): Pair<String, String> {
        if (input.isBlank()) return "" to ""
        // Handle formats like "14:00 - 17:00", "14:00-17:00", "14:00 to 17:00"
        val separators = listOf("-", "to", "–")
        for (sep in separators) {
            if (input.contains(sep)) {
                val parts = input.split(sep).map { it.trim() }
                if (parts.size >= 2) {
                    return parts[0] to parts[1]
                }
            }
        }
        return input to "" // Default: just start time
    }
}
