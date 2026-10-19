from pathlib import Path
from datetime import datetime
import shutil

p = Path(
    "composeApp/src/commonMain/kotlin/com/school/manage/"
    "presentation/screens/DashboardScreen.kt"
)

s = p.read_text()

start = s.find(
    "    val dashboardRefreshConnection = remember("
)
end_marker = "\n    val pickSchoolLogo = rememberPhotoPickerLauncher"
end = s.find(end_marker, start)

if start < 0 or end < 0:
    raise SystemExit(
        "STOP: Could not locate the exact refresh block. "
        "No source changes made."
    )

old = s[start:end]

if "override fun onPostScroll(" not in old:
    raise SystemExit(
        "STOP: Existing refresh block differs from expected. "
        "No source changes made."
    )

new = '''    val dashboardRefreshConnection = remember(schoolCode, dashboardRefreshThreshold) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val atTop = dashboardListState.firstVisibleItemIndex == 0 &&
                    dashboardListState.firstVisibleItemScrollOffset == 0

                if (source == NestedScrollSource.Drag &&
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

                    // Consume the downward drag while pulling to refresh.
                    return Offset(0f, available.y)
                }

                if (available.y < 0f || !atTop) {
                    dashboardPullDistance = 0f
                }

                return Offset.Zero
            }
        }
    }
'''

backup = p.with_name(
    p.name + ".before_pre_scroll_" +
    datetime.now().strftime("%Y%m%d_%H%M%S")
)
shutil.copy2(p, backup)

p.write_text(s[:start] + new + s[end:])

print("PATCH APPLIED")
print("Backup:", backup)
print("Changed only:", p)
