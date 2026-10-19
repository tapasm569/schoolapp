from pathlib import Path
from datetime import datetime
import shutil

p = Path(
    "composeApp/src/commonMain/kotlin/com/school/manage/"
    "presentation/screens/DashboardScreen.kt"
)
s = p.read_text()

if "dashboardRefreshConnection" in s:
    raise SystemExit("STOP: Refresh code already exists.")

# Validate exact insertion points before editing.
anchor = "    val scope = rememberCoroutineScope()\n"
old_list = "        LazyColumn(\n            modifier = Modifier\n"
old_header = (
    "        ) {\n"
    "            item { Spacer(modifier = Modifier.height(6.dp)) }"
)

for label, needle in (
    ("scope declaration", anchor),
    ("LazyColumn declaration", old_list),
    ("first list item", old_header),
):
    count = s.count(needle)
    if count != 1:
        raise SystemExit(
            f"STOP: {label}: expected 1 match, found {count}. "
            "Source unchanged."
        )

service = Path(
    "composeApp/src/commonMain/kotlin/com/school/manage/"
    "core/firebase/FirestoreSyncService.kt"
).read_text()
if "suspend fun restoreAllFromCloud(schoolCode: String)" not in service:
    raise SystemExit("STOP: Restore function not found. Source unchanged.")

# Imports
s = s.replace(
    "import androidx.compose.foundation.lazy.LazyColumn",
    "import androidx.compose.foundation.lazy.LazyColumn\n"
    "import androidx.compose.foundation.lazy.rememberLazyListState\n"
    "import androidx.compose.ui.geometry.Offset\n"
    "import androidx.compose.ui.input.nestedscroll.NestedScrollConnection\n"
    "import androidx.compose.ui.input.nestedscroll.NestedScrollSource\n"
    "import androidx.compose.ui.input.nestedscroll.nestedScroll",
    1,
)

# State and connection
addition = '''    val dashboardListState = rememberLazyListState()
    var isRefreshingDashboard by remember { mutableStateOf(false) }
    var dashboardPullDistance by remember { mutableFloatStateOf(0f) }
    val dashboardRefreshThreshold = with(androidx.compose.ui.platform.LocalDensity.current) {
        80.dp.toPx()
    }

    val dashboardRefreshConnection = remember(schoolCode, dashboardRefreshThreshold) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val atTop = dashboardListState.firstVisibleItemIndex == 0 &&
                    dashboardListState.firstVisibleItemScrollOffset == 0

                if (source == NestedScrollSource.Drag && atTop &&
                    available.y > 0f && !isRefreshingDashboard
                ) {
                    dashboardPullDistance += available.y
                    if (dashboardPullDistance >= dashboardRefreshThreshold) {
                        dashboardPullDistance = 0f
                        isRefreshingDashboard = true
                        scope.launch {
                            try {
                                FirestoreSyncService(database)
                                    .restoreAllFromCloud(schoolCode)
                            } finally {
                                isRefreshingDashboard = false
                            }
                        }
                    }
                } else if (available.y < 0f || !atTop) {
                    dashboardPullDistance = 0f
                }
                return Offset.Zero
            }
        }
    }

'''
s = s.replace(anchor, anchor + addition, 1)

# Attach scroll state and connection.
s = s.replace(
    old_list,
    "        LazyColumn(\n"
    "            state = dashboardListState,\n"
    "            modifier = Modifier\n"
    "                .nestedScroll(dashboardRefreshConnection)\n",
    1,
)

# Add loading indicator above the first item.
s = s.replace(
    old_header,
    '''        ) {
            item {
                if (isRefreshingDashboard) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Refreshing data...")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(6.dp)) }''',
    1,
)

if "restoreAllFromCloud(schoolCode)" not in s:
    raise SystemExit("STOP: Validation failed. Source unchanged.")
if s.count("dashboardRefreshConnection") != 2:
    raise SystemExit("STOP: Connection validation failed. Source unchanged.")

backup = p.with_name(
    p.name + ".before_pull_refresh_" + datetime.now().strftime("%Y%m%d_%H%M%S")
)
shutil.copy2(p, backup)
p.write_text(s)

print("PATCH APPLIED")
print("Backup:", backup)
