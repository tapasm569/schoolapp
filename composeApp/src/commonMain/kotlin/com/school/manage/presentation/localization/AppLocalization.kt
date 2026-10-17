package com.school.manage.presentation.localization

import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    BENGALI("bn", "Bengali", "বাংলা"),
    HINDI("hi", "Hindi", "हिन्दी")
}

data class AppStrings(
    // Header & Role Selection
    val headline: String,
    val languageLabel: String,
    val selectRoleTitle: String,
    val selectRoleDesc: String,
    val loginAdminTitle: String,
    val loginAdminSubtitle: String,
    val loginStaffTitle: String,
    val loginStaffSubtitle: String,
    val loginStudentTitle: String,
    val loginStudentSubtitle: String,
    val orDivider: String,
    val createAdminTitle: String,
    val createAdminSubtitle: String,

    // Login Form & Modal
    val back: String,
    val portalAccess: String,
    val schoolCodeLabel: String,
    val registeredPhoneLabel: String,
    val passwordLabel: String,
    val loginButton: String,
    val loggingIn: String,
    val errorEmptyFields: String,

    // Navigation / Common
    val dashboard: String,
    val attendance: String,
    val students: String,
    val fees: String,
    val staff: String,
    val present: String,
    val absent: String,
    val leave: String,
    val holiday: String
)

val EnglishStrings = AppStrings(
    headline = "Simplify your life and boost\nyour productivity 🚀",
    languageLabel = "Language",
    selectRoleTitle = "Select role",
    selectRoleDesc = "Please choose your role to continue. Select the role that best describes your position within the organization.",
    loginAdminTitle = "Login as Admin",
    loginAdminSubtitle = "Control all administrative tasks and monitor operations.",
    loginStaffTitle = "Login as Staff",
    loginStaffSubtitle = "Manage classes, communicate with students, and track progress.",
    loginStudentTitle = "Login as Student",
    loginStudentSubtitle = "View your classes, fees, homework and more.",
    orDivider = "OR",
    createAdminTitle = "Create Admin Account",
    createAdminSubtitle = "New here? Step up and create your Admin Account to unlock endless possibilities!",
    back = "Back",
    portalAccess = "PORTAL ACCESS",
    schoolCodeLabel = "Institute Code",
    registeredPhoneLabel = "Registered Mobile Number",
    passwordLabel = "Password",
    loginButton = "Sign In 🚀",
    loggingIn = "Signing In...",
    errorEmptyFields = "Please enter all required fields",
    dashboard = "Dashboard",
    attendance = "Attendance",
    students = "Students",
    fees = "Fees",
    staff = "Staff",
    present = "Present",
    absent = "Absent",
    leave = "Leave",
    holiday = "Holiday"
)

val BengaliStrings = AppStrings(
    headline = "স্কুলের সমস্ত কাজ পরিচালনা করুন\nসহজে এবং দ্রুততার সাথে 🚀",
    languageLabel = "ভাষা",
    selectRoleTitle = "ভূমিকা নির্বাচন করুন",
    selectRoleDesc = "এগিয়ে যেতে আপনার ভূমিকা বেছে নিন। প্রতিষ্ঠানে আপনার দায়িত্ব অনুযায়ী সঠিক পোর্টালটি পছন্দ করুন।",
    loginAdminTitle = "অ্যাডমিন লগইন",
    loginAdminSubtitle = "সমস্ত প্রশাসনিক কাজ ও সামগ্রিক কার্যক্রম পরিচালনা করুন।",
    loginStaffTitle = "শিক্ষক ও কর্মী লগইন",
    loginStaffSubtitle = "ক্লাস, উপস্থিতি ও শিক্ষার্থীদের অগ্রগতি পরিচালনা করুন।",
    loginStudentTitle = "শিক্ষার্থী ও অভিভাবক লগইন",
    loginStudentSubtitle = "ক্লাস, ফি, হোমওয়ার্ক ও পরীক্ষার ফলাফল দেখুন।",
    orDivider = "অথবা",
    createAdminTitle = "নতুন স্কুল নিবন্ধন করুন",
    createAdminSubtitle = "নতুন প্রতিষ্ঠান? আপনার অ্যাডমিন অ্যাকাউন্ট তৈরি করে ডিজিটালাইজেশন শুরু করুন!",
    back = "ফিরে যান",
    portalAccess = "পোর্টাল এক্সেস",
    schoolCodeLabel = "স্কুল কোড (Institute Code)",
    registeredPhoneLabel = "নিবন্ধিত মোবাইল নম্বর",
    passwordLabel = "পাসওয়ার্ড",
    loginButton = "লগইন করুন 🚀",
    loggingIn = "লগইন হচ্ছে...",
    errorEmptyFields = "সমস্ত প্রয়োজনীয় তথ্য পূরণ করুন",
    dashboard = "ড্যাশবোর্ড",
    attendance = "উপস্থিতি",
    students = "শিক্ষার্থী",
    fees = "ফি আদায়",
    staff = "স্টাফ ও শিক্ষক",
    present = "উপস্থিত",
    absent = "অনুপস্থিত",
    leave = "ছুটি",
    holiday = "সরকারি ছুটি"
)

val HindiStrings = AppStrings(
    headline = "अपने स्कूल प्रबंधन को बनाएं\nसरल और अधिक उत्पादक 🚀",
    languageLabel = "भाषा",
    selectRoleTitle = "रोल चुनें",
    selectRoleDesc = "आगे बढ़ने के लिए अपना रोल चुनें। संस्था में अपने पद के अनुसार उपयुक्त विकल्प का चयन करें।",
    loginAdminTitle = "एडमिन लॉगिन",
    loginAdminSubtitle = "सभी प्रशासनिक कार्यों और गतिविधियों का प्रबंधन करें।",
    loginStaffTitle = "शिक्षक व स्टाफ़ लॉगिन",
    loginStaffSubtitle = "कक्षाओं, उपस्थिति और छात्रों की प्रगति ट्रैक करें।",
    loginStudentTitle = "विद्यार्थी व अभिभावक लॉगिन",
    loginStudentSubtitle = "अपनी कक्षाएं, फ़ीस, गृहकार्य और परिणाम देखें।",
    orDivider = "या",
    createAdminTitle = "नया स्कूल पंजीकृत करें",
    createAdminSubtitle = "नई संस्था? अपना एडमिन खाता बनाकर डिजिटलाइज़ेशन शुरू करें!",
    back = "वापस",
    portalAccess = "पोर्टल एक्सेस",
    schoolCodeLabel = "इंस्टीट्यूट कोड (School Code)",
    registeredPhoneLabel = "पंजीकृत मोबाइल नंबर",
    passwordLabel = "पासवर्ड",
    loginButton = "लॉगिन करें 🚀",
    loggingIn = "लॉगिन हो रहा है...",
    errorEmptyFields = "कृपया सभी आवश्यक फ़ील्ड भरें",
    dashboard = "डैशबोर्ड",
    attendance = "उपस्थिति",
    students = "विद्यार्थी",
    fees = "फ़ीस",
    staff = "स्टाफ़",
    present = "उपस्थित",
    absent = "अनुपस्थित",
    leave = "अवकाश",
    holiday = "छुट्टी"
)

val LocalAppStrings = staticCompositionLocalOf { EnglishStrings }
