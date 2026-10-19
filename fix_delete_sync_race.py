from pathlib import Path

p = Path(
    "composeApp/src/commonMain/kotlin/com/school/manage/core/firebase/FirestoreSyncService.kt"
)

s = p.read_text()

student_old = '''                                    "photoBase64" to item.photoBase64
                                )
                            )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        scope.launch {
            database.staffDao().getStaffBySchool(schoolCode).collectLatest { list ->
'''

student_new = '''                                    "photoBase64" to item.photoBase64
                                )
                            )

                        // Recheck after upload to handle a concurrent deletion.
                        val markerAfterUpload = firestore.collection("schools")
                            .document(schoolCode)
                            .collection("deletedStudents")
                            .document(item.id.toString())
                            .get()

                        if (markerAfterUpload.exists) {
                            firestore.collection("schools")
                                .document(schoolCode)
                                .collection("students")
                                .document(item.id.toString())
                                .delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        scope.launch {
            database.staffDao().getStaffBySchool(schoolCode).collectLatest { list ->
'''

staff_old = '''                                    "joinDate" to item.joinDate
                                )
                            )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        scope.launch {
            database.feeDao().getFeeRecordsBySchool(schoolCode).collectLatest { list ->
'''

staff_new = '''                                    "joinDate" to item.joinDate
                                )
                            )

                        // Recheck after upload to handle a concurrent deletion.
                        val markerAfterUpload = firestore.collection("schools")
                            .document(schoolCode)
                            .collection("deletedStaff")
                            .document(item.id.toString())
                            .get()

                        if (markerAfterUpload.exists) {
                            firestore.collection("schools")
                                .document(schoolCode)
                                .collection("staff")
                                .document(item.id.toString())
                                .delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        scope.launch {
            database.feeDao().getFeeRecordsBySchool(schoolCode).collectLatest { list ->
'''

for label, old in [
    ("student post-upload check", student_old),
    ("staff post-upload check", staff_old),
]:
    count = s.count(old)
    if count != 1:
        raise SystemExit(
            f"STOP: {label}: expected exactly 1 match, found {count}. "
            "Source file unchanged."
        )

if s.count("markerAfterUpload") != 0:
    raise SystemExit("STOP: post-upload check already exists. Source unchanged.")

backup = p.with_name(p.name + ".before_sync_race_fix_20261019")
if backup.exists():
    raise SystemExit(f"STOP: backup already exists: {backup}. Source unchanged.")

updated = s.replace(student_old, student_new, 1)
updated = updated.replace(staff_old, staff_new, 1)

backup.write_text(s)
p.write_text(updated)

print("PATCH APPLIED")
print("Backup:", backup)
print("Added post-upload deletion checks for students and staff.")
