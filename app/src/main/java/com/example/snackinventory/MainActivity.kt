package com.example.snackinventory

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.snackinventory.ui.theme.SnackInventoryTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: SnackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SnackInventoryTheme {
                MainNavigation(viewModel)
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Search : Screen("search", "Search", Icons.Default.Search)
    object Add : Screen("add", "Add", Icons.Default.Add)
    object Alerts : Screen("alerts", "Alerts", Icons.Default.Notifications)
    object Stats : Screen("stats", "Stats", Icons.Default.Info)
}

@Composable
fun MainNavigation(viewModel: SnackViewModel) {
    val navController = rememberNavController()
    val alerts by viewModel.alerts.collectAsState()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val items = listOf(Screen.Home, Screen.Search, Screen.Add, Screen.Alerts, Screen.Stats)
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { 
                            if (screen == Screen.Alerts && alerts.isNotEmpty()) {
                                BadgedBox(badge = { Badge { Text("${alerts.size}") } }) {
                                    Icon(screen.icon, contentDescription = screen.label)
                                }
                            } else {
                                Icon(screen.icon, contentDescription = screen.label)
                            }
                        },
                        label = { Text(screen.label) },
                        selected = currentDestination?.route?.startsWith(screen.route) == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Home.route, Modifier.padding(innerPadding)) {
            composable(Screen.Home.route) { HomeScreen(viewModel, navController) }
            composable(Screen.Search.route) { SearchScreen(viewModel, navController) }
            composable(Screen.Add.route) { AddEditSnackScreen(viewModel, navController) }
            composable(
                route = "edit/{snackId}",
                arguments = listOf(navArgument("snackId") { type = NavType.IntType })
            ) { backStackEntry ->
                val snackId = backStackEntry.arguments?.getInt("snackId")
                AddEditSnackScreen(viewModel, navController, snackId)
            }
            composable(Screen.Alerts.route) { AlertsScreen(viewModel, navController) }
            composable(Screen.Stats.route) { StatsScreen(viewModel) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: SnackViewModel, navController: NavHostController) {
    val snacks by viewModel.allSnacks.collectAsState(initial = emptyList())
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Kitchen Pantry") }) }
    ) { padding ->
        if (snacks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Your pantry is empty!")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        viewModel.addSnack("Potato Chips", 3, 5, "Salty")
                        viewModel.addSnack("Chocolate Bar", 10, 2, "Sweet", System.currentTimeMillis() + 86400000L * 3)
                        viewModel.addSnack("Granola Bars", 1, 3, "Healthy", System.currentTimeMillis() - 86400000L)
                    }) {
                        Text("Load Sample Stock")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(snacks) { snack ->
                    SnackCompactCard(snack, onClick = { navController.navigate("edit/${snack.id}") })
                }
            }
        }
    }
}

@Composable
fun SnackCompactCard(snack: Snack, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(snack.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(snack.category, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                if (snack.isLowStock()) {
                    Text("Low Stock", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Surface(
                color = if (snack.isLowStock()) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            ) {
                Text(
                    text = "${snack.quantity}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: SnackViewModel, navController: NavHostController) {
    val snacks by viewModel.filteredSnacks.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCat by viewModel.selectedCategory.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.updateSearchQuery(it) },
            label = { Text("Search by name...") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { viewModel.updateSearchQuery("") }) { Icon(Icons.Default.Clear, null) } }
        )
        
        Spacer(Modifier.height(8.dp))
        
        ScrollableTabRow(
            selectedTabIndex = categories.indexOf(selectedCat).coerceAtLeast(0),
            edgePadding = 0.dp,
            divider = {},
            containerColor = Color.Transparent
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCat == cat,
                    onClick = { viewModel.updateCategory(cat) },
                    label = { Text(cat) },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f).padding(top = 16.dp)
        ) {
            items(snacks) { snack -> 
                SnackCompactCard(snack, onClick = { navController.navigate("edit/${snack.id}") })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSnackScreen(viewModel: SnackViewModel, navController: NavHostController, snackId: Int? = null) {
    val context = LocalContext.current
    val snacks by viewModel.allSnacks.collectAsState(initial = emptyList())
    val existingSnack = snacks.find { it.id == snackId }

    var name by remember(existingSnack) { mutableStateOf(existingSnack?.name ?: "") }
    var category by remember(existingSnack) { mutableStateOf(existingSnack?.category ?: "General") }
    var quantity by remember(existingSnack) { mutableStateOf(existingSnack?.quantity?.toString() ?: "1") }
    var threshold by remember(existingSnack) { mutableStateOf(existingSnack?.minThreshold?.toString() ?: "2") }
    var expirationDate by remember(existingSnack) { mutableStateOf(existingSnack?.expirationDate) }

    val dateStr = expirationDate?.let { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it)) } ?: "None"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (snackId == null) "Add Snack" else "Edit Snack") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Snack Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = quantity, 
                    onValueChange = { quantity = it.filter { c -> c.isDigit() } }, 
                    label = { Text("Quantity") }, 
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = threshold, 
                    onValueChange = { threshold = it.filter { c -> c.isDigit() } }, 
                    label = { Text("Low Stock Threshold") }, 
                    modifier = Modifier.weight(1f)
                )
            }
            
            Card(
                onClick = {
                    val calendar = Calendar.getInstance()
                    DatePickerDialog(context, { _, y, m, d ->
                        calendar.set(y, m, d)
                        expirationDate = calendar.timeInMillis
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, null)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Expiration Date", style = MaterialTheme.typography.labelMedium)
                        Text(dateStr, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val q = quantity.toIntOrNull() ?: 0
                    val t = threshold.toIntOrNull() ?: 2
                    if (snackId == null) {
                        viewModel.addSnack(name, q, t, category, expirationDate)
                    } else {
                        viewModel.updateSnack(existingSnack!!.copy(name = name, quantity = q, minThreshold = t, category = category, expirationDate = expirationDate))
                    }
                    navController.popBackStack()
                }, 
                modifier = Modifier.fillMaxWidth(), 
                enabled = name.isNotBlank()
            ) {
                Text(if (snackId == null) "Add to Pantry" else "Update Snack")
            }
            
            if (snackId != null) {
                TextButton(
                    onClick = { viewModel.removeSnack(existingSnack!!); navController.popBackStack() }, 
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Remove from Inventory", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(viewModel: SnackViewModel, navController: NavHostController) {
    val alerts by viewModel.alerts.collectAsState()
    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Notifications") }) }) { padding ->
        if (alerts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color.Green, modifier = Modifier.size(64.dp))
                    Text("All snacks are in good status!")
                }
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(alerts) { snack ->
                    val statusColor = if (snack.isExpired()) Color.Red else if (snack.isExpiringSoon()) Color(0xFFFFA000) else MaterialTheme.colorScheme.error
                    val message = when {
                        snack.isExpired() -> "${snack.name} has expired!"
                        snack.isExpiringSoon() -> "${snack.name} is expiring soon!"
                        snack.isLowStock() -> "${snack.name} is running low (${snack.quantity} left)"
                        else -> ""
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { navController.navigate("edit/${snack.id}") },
                        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                    ) {
                        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (snack.isExpired()) Icons.Default.Warning else Icons.Default.Info,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(message, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text("Tap to manage inventory", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: SnackViewModel) {
    val history by viewModel.allHistory.collectAsState(initial = emptyList())
    val snacks by viewModel.allSnacks.collectAsState(initial = emptyList())

    val consumption = history.filter { it.quantityChange < 0 }
        .groupBy { it.snackName }
        .mapValues { it.value.sumOf { h -> -h.quantityChange } }
        .toList().sortedByDescending { it.second }.take(5)

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Inventory Insights") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            
            Text("Consumption Trends", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (consumption.isEmpty()) {
                Text("Start using snacks to see trends!", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            } else {
                consumption.forEach { (name, count) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                        Text("$count units", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    LinearProgressIndicator(
                        progress = { count.toFloat() / (consumption.firstOrNull()?.second ?: 1).toFloat() },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                    )
                }
            }
            
            HorizontalDivider()
            
            Text("Pantry Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatBox("Total", "${snacks.size}", Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer)
                StatBox("Low", "${snacks.count { it.isLowStock() }}", Modifier.weight(1f), MaterialTheme.colorScheme.errorContainer)
                StatBox("Expired", "${snacks.count { it.isExpired() }}", Modifier.weight(1f), Color(0xFFFFEBEE))
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier, containerColor: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
