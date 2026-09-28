package pk.groscale.feeregister.ui.nav

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import pk.groscale.feeregister.data.repo.BackupRepository
import pk.groscale.feeregister.data.repo.FeeRepository
import pk.groscale.feeregister.data.db.StudentEditRow
import pk.groscale.feeregister.data.prefs.Profile
import pk.groscale.feeregister.domain.Settlement
import pk.groscale.feeregister.ui.attendance.AttendanceScreen
import pk.groscale.feeregister.notify.ReminderSync
import pk.groscale.feeregister.ui.components.rememberBackupFilePicker
import pk.groscale.feeregister.ui.home.HomeScreen
import pk.groscale.feeregister.ui.home.HomeViewModel
import pk.groscale.feeregister.ui.setup.BatchFormScreen
import pk.groscale.feeregister.ui.setup.RemindersScreen
import pk.groscale.feeregister.ui.setup.SetupScreen
import pk.groscale.feeregister.ui.setup.WelcomeScreen
import pk.groscale.feeregister.ui.students.StudentEditScreen
import pk.groscale.feeregister.ui.students.StudentFormScreen
import pk.groscale.feeregister.ui.settings.SettingsScreen
import pk.groscale.feeregister.ui.settings.SettingsViewModel
import pk.groscale.feeregister.ui.students.StudentsScreen
import pk.groscale.feeregister.util.safely
import java.time.LocalDate

private object Route {
    const val WELCOME = "welcome"
    const val SETUP = "setup"
    const val FIRST_BATCH = "first_batch"
    const val FIRST_STUDENT = "first_student"
    const val FIRST_REMINDERS = "first_reminders"
    const val HOME = "home"
    const val STUDENTS = "students"
    const val ADD_BATCH = "add_batch"
    const val ADD_STUDENT = "add_student"
    const val SETTINGS = "settings"
    const val ATTENDANCE = "attendance"
    const val EDIT_STUDENT = "edit_student"
}

private val tabRoutes = setOf(Route.HOME, Route.STUDENTS, Route.SETTINGS)

@Composable
fun AppNav(repo: FeeRepository, backups: BackupRepository) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // The whole "is logged in" check: one boolean, no server.
    val setupComplete by repo.profile.map { it.setupComplete }.collectAsState(initial = null)
    if (setupComplete == null) return // one frame while DataStore reads

    val start = if (setupComplete == true) Route.HOME else Route.WELCOME
    val backStack by nav.currentBackStackEntryAsState()
    val onTab = backStack?.destination?.route in tabRoutes

    Scaffold(
        bottomBar = { if (onTab) BottomBar(nav, backStack?.destination?.route) },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable(Route.WELCOME) {
                // Restoring here needs no "replace everything?" warning: this
                // screen only shows before setup, so there is no register to
                // lose. On success the restored profile carries setupComplete,
                // and the NavHost routes to Home on its own - that is the
                // confirmation. Only a failure needs saying out loud.
                val pickBackup = rememberBackupFilePicker { text ->
                    scope.launch {
                        safely("restore backup") { backups.restore(text) }
                            .onSuccess { count ->
                                Toast.makeText(
                                    context,
                                    "Restored $count students.",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                            .onFailure {
                                Toast.makeText(
                                    context,
                                    "That file is not an Inkpot backup. Nothing was changed.",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                    }
                }
                WelcomeScreen(
                    onGetStarted = { nav.navigate(Route.SETUP) },
                    onRestore = pickBackup,
                )
            }

            composable(Route.SETUP) {
                SetupScreen { tuition, teacher, phone ->
                    scope.launch {
                        repo.saveTuition(tuition, teacher, phone)
                        nav.navigate(Route.FIRST_BATCH)
                    }
                }
            }

            composable(Route.FIRST_BATCH) {
                BatchFormScreen(
                    title = "Your first batch",
                    subtitle = "A batch is one class at one time — you can add more later.",
                    ctaText = "Next",
                ) { draft ->
                    scope.launch {
                        repo.createBatch(
                            draft.name, draft.classLabel, draft.daysOfWeekMask,
                            draft.startMinute, draft.defaultFee,
                        )
                        nav.navigate(Route.FIRST_STUDENT)
                    }
                }
            }

            composable(Route.FIRST_STUDENT) {
                val batches by repo.observeBatches().collectAsState(initial = emptyList())
                val profile by repo.profile.collectAsState(initial = Profile())
                StudentFormScreen(
                    batches = batches,
                    title = "Your first student",
                    graceDays = profile.graceDays,
                    roundingStep = profile.roundingStep,
                ) { draft ->
                    scope.launch {
                        repo.addStudent(
                            name = draft.name,
                            classLabel = draft.classLabel,
                            guardianPhone = draft.guardianPhone,
                            batchId = draft.batchId,
                            fee = draft.fee,
                            cycle = draft.cycle,
                        )
                        nav.navigate(Route.FIRST_REMINDERS)
                    }
                }
            }

            composable(Route.FIRST_REMINDERS) {
                val batches by repo.observeBatches().collectAsState(initial = emptyList())
                RemindersScreen(
                    batchName = batches.firstOrNull()?.name.orEmpty(),
                ) { attendance, fee ->
                    scope.launch {
                        safely("save reminders") { repo.setReminders(attendance, fee) }
                        safely("schedule reminders") { ReminderSync.rescheduleAll(context) }
                        // Last thing, deliberately: `setupComplete` is what the
                        // NavHost routes on, so flipping it earlier would rebuild
                        // the graph and skip this screen entirely.
                        repo.markSetupComplete()
                        nav.navigate(Route.HOME) { popUpTo(Route.WELCOME) { inclusive = true } }
                    }
                }
            }

            composable(Route.HOME) {
                val vm: HomeViewModel = viewModel(
                    factory = viewModelFactory { initializer { HomeViewModel(repo) } },
                )
                val state by vm.state.collectAsState()
                HomeScreen(
                    state = state,
                    events = vm.events,
                    onResumed = vm::makeBillsForToday,
                    onPreviousMonth = vm::previousMonth,
                    onNextMonth = vm::nextMonth,
                    onReceivePayment = vm::receivePayment,
                    onMarkReminded = vm::markReminded,
                    onAddStudent = { nav.navigate(Route.ADD_STUDENT) },
                )
            }

            composable(Route.STUDENTS) {
                val students by repo.observeStudents().collectAsState(initial = emptyList())
                val batches by repo.observeBatches().collectAsState(initial = emptyList())
                val enrollments by repo.observeEnrollments().collectAsState(initial = emptyList())

                val feeByStudent = remember(enrollments) { enrollments.associate { it.studentId to it.fee } }
                val batchByStudent = remember(enrollments, batches) {
                    val names = batches.associate { it.id to it.name }
                    enrollments.associate { it.studentId to names[it.batchId].orEmpty() }
                }

                StudentsScreen(
                    students = students,
                    batches = batches,
                    feeFor = { feeByStudent[it] ?: 0 },
                    batchFor = { batchByStudent[it].orEmpty() },
                    onAddStudent = { nav.navigate(Route.ADD_STUDENT) },
                    onAddBatch = { nav.navigate(Route.ADD_BATCH) },
                    onOpenStudent = { nav.navigate("${Route.EDIT_STUDENT}/$it") },
                    onMarkAttendance = { nav.navigate(Route.ATTENDANCE) },
                )
            }

            composable(Route.ATTENDANCE) {
                val batches by repo.observeBatches().collectAsState(initial = emptyList())
                val profile by repo.profile.collectAsState(initial = Profile())
                var batchId by remember { mutableLongStateOf(0L) }
                var date by remember { mutableStateOf(LocalDate.now()) }

                // Same race as the student form: `batches` lands a frame later.
                LaunchedEffect(batches) {
                    if (batches.none { it.id == batchId }) batchId = batches.firstOrNull()?.id ?: 0L
                }

                val roll by remember(batchId, date) {
                    if (batchId == 0L) flowOf(emptyList()) else repo.observeRoll(batchId, date)
                }.collectAsState(initial = emptyList())

                AttendanceScreen(
                    batches = batches,
                    selectedBatchId = batchId,
                    onSelectBatch = { batchId = it },
                    date = date,
                    onPreviousDay = { date = date.minusDays(1) },
                    // Attendance cannot be recorded for a day that has not happened.
                    onNextDay = { if (date < LocalDate.now()) date = date.plusDays(1) },
                    roll = roll,
                    onToggle = { row ->
                        scope.launch { repo.setAbsent(row.enrollmentId, date, !row.absent) }
                    },
                    tuitionName = profile.tuitionName,
                )
            }

            composable("${Route.EDIT_STUDENT}/{studentId}") { entry ->
                val studentId = entry.arguments?.getString("studentId")?.toLongOrNull()
                var row by remember(studentId) { mutableStateOf<StudentEditRow?>(null) }
                // Worked out up front so the leave dialog can answer "has he
                // cleared his dues?" the moment it opens, not a spinner later.
                var leaving by remember(studentId) { mutableStateOf<Settlement?>(null) }
                LaunchedEffect(studentId) {
                    if (studentId != null) {
                        row = repo.studentForEdit(studentId)
                        safely("work out what is owed") { repo.previewLeaving(studentId) }
                            .onSuccess { leaving = it }
                    }
                }
                row?.let { r ->
                    StudentEditScreen(
                        student = r,
                        leaving = leaving,
                        onSave = { name, phone, fee ->
                            scope.launch {
                                safely("update student") {
                                    repo.updateStudent(r.studentId, r.enrollmentId, name, phone, fee)
                                }
                                nav.popBackStack()
                            }
                        },
                        onMarkLeft = {
                            scope.launch {
                                safely("mark student left") { repo.markStudentLeft(r.studentId) }
                                nav.popBackStack()
                            }
                        },
                    )
                }
            }

            composable(Route.SETTINGS) {
                val vm: SettingsViewModel = viewModel(
                    factory = viewModelFactory { initializer { SettingsViewModel(repo, backups) } },
                )
                SettingsScreen(vm)
            }

            composable(Route.ADD_BATCH) {
                BatchFormScreen(title = "Add a batch", subtitle = null, ctaText = "Save batch") { draft ->
                    scope.launch {
                        repo.createBatch(
                            draft.name, draft.classLabel, draft.daysOfWeekMask,
                            draft.startMinute, draft.defaultFee,
                        )
                        nav.popBackStack()
                    }
                }
            }

            composable(Route.ADD_STUDENT) {
                val batches by repo.observeBatches().collectAsState(initial = emptyList())
                val profile by repo.profile.collectAsState(initial = Profile())
                StudentFormScreen(
                    batches = batches,
                    graceDays = profile.graceDays,
                    roundingStep = profile.roundingStep,
                ) { draft ->
                    scope.launch {
                        repo.addStudent(
                            name = draft.name,
                            classLabel = draft.classLabel,
                            guardianPhone = draft.guardianPhone,
                            batchId = draft.batchId,
                            fee = draft.fee,
                            cycle = draft.cycle,
                        )
                        nav.popBackStack()
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController, current: String?) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
            selected = current == Route.HOME,
            onClick = { if (current != Route.HOME) nav.navigate(Route.HOME) { launchSingleTop = true } },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("This month") },
        )
        NavigationBarItem(
            selected = current == Route.STUDENTS,
            onClick = { if (current != Route.STUDENTS) nav.navigate(Route.STUDENTS) { launchSingleTop = true } },
            icon = { Icon(Icons.Filled.Groups, contentDescription = null) },
            label = { Text("Students") },
        )
        NavigationBarItem(
            selected = current == Route.SETTINGS,
            onClick = { if (current != Route.SETTINGS) nav.navigate(Route.SETTINGS) { launchSingleTop = true } },
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            label = { Text("Settings") },
        )
    }
}
