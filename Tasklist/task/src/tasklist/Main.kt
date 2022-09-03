package tasklist

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.datetime.*
import java.io.File
import java.lang.Integer.min


class TaskManager {
    data class Task(var date: String, var time: String, var priority: Char, var description: String) {
        private val tag: Char
            get() {
                val dateList = date.split("-")
                val taskDate = LocalDate(dateList[0].toInt(), dateList[1].toInt(), dateList[2].toInt())
                val currentDate = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+0")).date
                val numberOfDays = currentDate.daysUntil(taskDate)
                return when {
                    numberOfDays == 0 -> 'T'
                    numberOfDays > 0 -> 'I'
                    else -> 'O'
                }
            }
        val priorityColor: String
            get() {
                return when (priority) {
                    'C' -> "\u001B[101m \u001B[0m"
                    'H' -> "\u001B[103m \u001B[0m"
                    'N' -> "\u001B[102m \u001B[0m"
                    else -> "\u001B[104m \u001B[0m"
                }
            }
        val tagColor: String
            get() {
                return when (tag) {
                    'I' -> "\u001B[102m \u001B[0m"
                    'T' -> "\u001B[103m \u001B[0m"
                    else -> "\u001B[101m \u001B[0m"
                }
            }
    }

    private var taskList = mutableListOf<Task>()
    private val listOfActions = listOf("add", "print", "edit", "delete", "end")
    private val listOfFields = listOf("priority", "date", "time", "task")
    private var action = ""
    private val file = File("tasklist.json")

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val type = Types.newParameterizedType(MutableList::class.java, Task::class.java)
    private val taskListAdapter: JsonAdapter<MutableList<Task>> = moshi.adapter(type)

    fun startTaskManager() {
        getTasksFromFile()
        do {
            getAction()
            when (action) {
                "add" -> add()
                "print" -> printTasks()
                "edit" -> edit()
                "delete" -> delete()
                "end" -> end()
            }

        } while (action != "end")
    }

    private fun getTasksFromFile() {
        if (!file.exists()) {
            return
        }
        val probablyTaskList = taskListAdapter.fromJson(file.readText())
        if (probablyTaskList != null) {
            taskList = probablyTaskList.toMutableList()
        }
    }

    private fun getTaskNumber(): Int {
        var taskNumberInput: String
        do {
            println("Input the task number (1-${taskList.size}):")
            taskNumberInput = readln()
            if (taskNumberInput.toIntOrNull() == null || taskNumberInput.toInt() !in 1..taskList.size) {
                println("Invalid task number")
            }
        } while (taskNumberInput.toIntOrNull() == null || taskNumberInput.toInt() !in 1..taskList.size)
        return taskNumberInput.toInt()
    }

    private fun getFieldToEdit(): String {
        var fieldToEdit: String
        do {
            println("Input a field to edit (priority, date, time, task):")
            fieldToEdit = readln()
            if (fieldToEdit !in listOfFields) {
                println("Invalid field")
            }
        } while (fieldToEdit !in listOfFields)
        return fieldToEdit
    }

    private fun edit() {
        printTasks()
        if (taskList.isEmpty()) {
            return
        }

        val taskIndex = getTaskNumber() - 1

        when (getFieldToEdit()) {
            "priority" -> {
                taskList[taskIndex].priority = getTaskPriority()
            }

            "date" -> {
                taskList[taskIndex].date = getTaskDate()
            }

            "time" -> {
                taskList[taskIndex].time = getTaskTime()
            }

            "task" -> {
                taskList[taskIndex].description = getTaskDescription()
            }
        }
        println("The task is changed")
    }

    private fun delete() {
        printTasks()
        if (taskList.isEmpty()) {
            return
        }
        val taskNumber = getTaskNumber()
        taskList.removeAt(taskNumber - 1)
        println("The task is deleted")
    }

    private fun getAction() {
        do {
            println("Input an action (${listOfActions.joinToString(", ")}):")
            action = readln()
            if (action !in listOfActions) {
                println("The input action is invalid")
            }
        } while (action !in listOfActions)
    }

    private fun getTaskPriority(): Char {
        val template = "[chnl]".toRegex()
        var priority = ' '
        do {
            println("Input the task priority (C, H, N, L):")
            val input = readln().lowercase()
            if (template.matches(input)) {
                priority = input[0]
            }
        } while (priority !in listOf('c', 'h', 'n', 'l'))
        return priority.uppercaseChar()
    }

    private fun isDateCorrect(date: String): Boolean {
        val template = "\\d{4}-\\d{1,2}-\\d{1,2}".toRegex()
        val dateList = date.split("-").toList()
        return try {
            if (!template.matches(date)) {
                return false
            }
            LocalDate(dateList[0].toInt(), dateList[1].toInt(), dateList[2].toInt())
            true
        } catch (e: RuntimeException) {
            false
        }
    }

    private fun getTaskDate(): String {
        var date: String
        do {
            println("Input the date (yyyy-mm-dd):")
            date = readln()
            if (!isDateCorrect(date)) {
                println("The input date is invalid")
            }
        } while (!isDateCorrect(date))
        val dateList = date.split("-")
        return ("0".repeat(4 - dateList[0].length) + dateList[0] + "-" + "0".repeat(2 - dateList[1].length) +
                dateList[1] + "-" + "0".repeat(
            2 - dateList[2].length
        ) + dateList[2])
    }

    private fun isTimeCorrect(time: String): Boolean {
        val template = "\\d{1,2}:\\d{1,2}".toRegex()
        val timeList = time.split(":")
        return try {
            if (!template.matches(time)) {
                return false
            }
            LocalDateTime(2000, 1, 1, timeList[0].toInt(), timeList[1].toInt())
            true
        } catch (e: RuntimeException) {
            false
        }
    }

    private fun getTaskTime(): String {
        var time: String
        do {
            println("Input the time (hh:mm):")
            time = readln()
            if (!isTimeCorrect(time)) {
                println("The input time is invalid")
            }
        } while (!isTimeCorrect(time))
        val timeList = time.split(":")

        return ("0".repeat(2 - timeList[0].length) + timeList[0] + ":" + "0".repeat(2 - timeList[1].length) + timeList[1])

    }

    private fun getTaskDescription(): String {
        println("Input a new task (enter a blank line to end):")
        var task = ""
        do {
            val input = readln().trim()
            if (input.isNotBlank()) {
                task += (if (task.isNotEmpty()) "\n" else "") + input
            }
        } while (input.isNotBlank())
        return task
    }

    private fun add() {
        val priority = getTaskPriority()
        val date = getTaskDate()
        val time = getTaskTime()
        val taskDescription = getTaskDescription()
        if (taskDescription.isNotBlank()) {
            taskList.add(Task(date, time, priority, taskDescription))
        } else {
            println("The task is blank")
        }
    }

    private fun saveDataToFile() {
        val dataForJson = taskListAdapter.toJson(taskList)
        file.writeText(dataForJson)
    }

    private fun end() {
        saveDataToFile()
        println("Tasklist exiting!")
    }

    private fun printTasks() {
        if (taskList.isEmpty()) {
            println("No tasks have been input")
            return
        }

        println("+----+------------+-------+---+---+--------------------------------------------+")
        println("| N  |    Date    | Time  | P | D |                   Task                     |")
        println("+----+------------+-------+---+---+--------------------------------------------+")

        for (i in taskList.indices) {
            val taskDescriptionLines = getTaskLines(taskList[i])
            val firstLine = "| " + (i + 1).toString() + if (i < 9) {
                "  "
            } else {
                " "
            } + "| ${taskList[i].date} | ${taskList[i].time} | ${taskList[i].priorityColor} | ${taskList[i].tagColor} |${taskDescriptionLines[0]}|"
            println(firstLine)
            for (j in 1..taskDescriptionLines.lastIndex) {
                println("|    |            |       |   |   |${taskDescriptionLines[j]}|")
            }
            println("+----+------------+-------+---+---+--------------------------------------------+")
        }
    }

    private fun getTaskLines(task: Task): MutableList<String> {
        val listOfLines = mutableListOf<String>()
        for (line in task.description.lines()) {
            for (i in line.indices step 44) {
                var amountOfSpaces = 0
                if (line.length < i + 44) {
                    amountOfSpaces = 44 - line.length % 44
                }
                listOfLines.add(line.substring(i, min(i + 44, line.length)) + " ".repeat(amountOfSpaces))
            }
        }
        return listOfLines
    }
}

fun main() {
    val taskManager = TaskManager()
    taskManager.startTaskManager()
}


