from pathlib import Path
from datetime import datetime
import shutil

p = Path(
    "composeApp/src/commonMain/kotlin/com/school/manage/"
    "presentation/screens/DashboardScreen.kt"
)

s = p.read_text()

# Ensure this patch has not already been applied.
if "dashboardRefreshConnection" in s:
    raise SystemExit("STOP: Dashboard refresh patch already exists.")

# Verify the exact insertion points before changing anything.
required = [
    "import androidx.compose.foundation.lazy.LazyColumn",
    "import androidx.compose.runtime.rememberCoroutineScope",
    "    val scope = rememberCoroutineScope()\n",
    "        LazyColumn(\n            modifier = Modifier\n",
]

for text in required:
    count = s.count(text)
    if count != 1:
        raise SystemExit(
            f"STOP: Expected exactly one occurrence, found {count}: {text!r}\n"
            "No source changes were made."
        )

# Confirm the cloud restore method exists.
service = Path(
    "composeApp/src/commonMain/kotlin/com/school/manage/"
    "core/firebase/FirestoreSyncService.kt"
).read_text()

if "suspend fun restoreAllFromCloud(schoolCode: String)" not in service:
    raise SystemExit("STOP: Cloud restore method not found. No changes made.")

# Add imports.
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

# Add refresh state and nested-scroll connection.
anchor = "    val scope = rememberCoroutineScope()\n"

addition = '''    val dashboardListState = rememberLazyListState()
    var isRefreshingDashboard by remember { mutableStateOf(false) }
    var dashboardPullDistance by remember { mutableStateOf(0f) }
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

                if (
                    source == NestedScrollSource.Drag &&
                    atTop &&
                    available.y > 0f &&
                    !isRefreshingDashboard
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

# Attach the connection and list state to the existing dashboard list.
old_list = "        LazyColumn(\\n            modifier = Modifier\\n"
new_list = (
    "        LazyColumn(\\n"
    "            state = dashboardListState,\\n"
    "            modifier = Modifier\\n"
    "                .nestedScroll(dashboardRefreshConnection)\\n"
)

if s.count(old_list) != 1:
    raise SystemExit("STOP: LazyColumn layout changed. No source file was written.")

s = s.replace(old_list, new_list, 1)

# Show a small progress indicator at the top while cloud data is being restored.
anchor_item = (
    "        ) {\\n"
    "            item { Spacer(modifier = Modifier.height(6.dp)) }"
)

refresh_item = '''        ) {
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

            item { Spacer(modifier = Modifier.height(6.dp)) }'''

if s.count(anchor_item) != 1:
    raise SystemExit("STOP: Dashboard list header changed. No source file was written.")

s = s.replace(anchor_item, refresh_item, 1)

# Basic structural checks before writing.
if s.count("dashboardRefreshConnection") != 2:
    raise SystemExit("STOP: Refresh connection validation failed.")
if "restoreAllFromCloud(schoolCode)" not in s:
    raise SystemExit("STOP: Refresh action validation failed.")

backup = p.with_name(
    p.name + ".before_pull_refresh_" +
    datetime.now().strftime("%Y%m%d_%H%M%S")
)
shutil.copy2(p, backup)
p.write_text(s)

print("PATCH APPLIED")
print(f"Backup: {backup}")
print("Added dashboard pull-to-refresh and a refresh indicator.")
print("The existing dashboard content and admission-fee code were preserved.")
