import os
import glob
import re

translations = {
    # AuthScreen
    '"TaskFlow Auto"': '"تاسك فلو أوتو"',
    '"Task Management & Automation Engine"': '"محرك إدارة المهام والأتمتة"',
    '"Sign In"': '"تسجيل الدخول"',
    '"Register"': '"حساب جديد"',
    '"Email Address"': '"البريد الإلكتروني"',
    '"Password"': '"كلمة المرور"',
    '"Default FirebaseApp is not initialized in this process"': '"تطبيق Firebase غير متصل بالخادم"',
    '". Make sure to call FirebaseApp.initializeApp(Context) first."': '"يرجى إضافة ملف google-services.json لتفعيل المزامنة."',
    '"Login to Cloud Account"': '"تسجيل الدخول السحابي"',
    '"Create Cloud Account"': '"إنشاء حساب سحابي"',
    '"Continue as Guest (Local Offline Mode)"': '"المتابعة كزائر (وضع عدم الاتصال)"',
    '"OR"': '"أو"',
    '"Firebase Firestore & FCM keep your tasks, scripts, and\\nautomation triggers synced across devices."': '"تقوم خدمات Firebase السحابية بمزامنة مهامك ونصوص الأتمتة عبر أجهزتك."',
    
    # Navigation
    '"Dashboard"': '"الرئيسية"',
    '"Tasks"': '"المهام"',
    '"Automation"': '"الأتمتة"',
    '"Colab"': '"كولاب"',

    # DashboardScreen
    '"Welcome back,"': '"مرحباً بك،"',
    '"Guest User"': '"زائر"',
    '"Today\'s Overview"': '"نظرة عامة على اليوم"',
    '"You have "': '"لديك "',
    '" tasks pending."': '" مهام قيد الانتظار."',
    '"All caught up! Time to relax or run some automations."': '"لقد أنجزت كل شيء! وقت الاسترخاء أو تشغيل بعض الأتمتة."',
    '"Today\'s Tasks"': '"مهام اليوم"',
    '"Active Scripts"': '"النصوص البرمجية النشطة"',
    '"Automations Live"': '"الأتمتة مفعلة"',
    '"Last Run"': '"آخر تشغيل"',
    '"Time: "': '"الوقت: "',
    '"never"': '"أبداً"',
    '"No tasks due today. Enjoy your day!"': '"لا توجد مهام اليوم. استمتع بيومك!"',
    '"Add a task"': '"إضافة مهمة"',
    '"Quick Actions (One-Tap Dispatch)"': '"إجراءات سريعة (بنقرة واحدة)"',
    '"No quick action scripts marked. Star scripts in Automation Hub to see them here."': '"لا توجد نصوص مفضلة. قم بتمييز النصوص بنجمة في قسم الأتمتة لتظهر هنا."',
    
    # TasksScreen
    '"Task Manager"': '"مدير المهام"',
    '"Search tasks or workflows..."': '"البحث عن المهام أو سير العمل..."',
    '"Filter Tasks"': '"تصفية المهام"',
    '"Status"': '"الحالة"',
    '"Priority"': '"الأولوية"',
    '"Clear Filters"': '"مسح الفلاتر"',
    '"All"': '"الكل"',
    '"Pending"': '"قيد الانتظار"',
    '"Completed"': '"مكتمل"',
    '"Low"': '"منخفض"',
    '"Medium"': '"متوسط"',
    '"High"': '"عالي"',
    '"Personal"': '"شخصي"',
    '"Work"': '"عمل"',
    '"Finance"': '"مالية"',
    '"Health"': '"صحة"',
    '"Development"': '"برمجة"',
    '"Others"': '"أخرى"',
    '"No tasks match your filters."': '"لا توجد مهام تطابق الفلاتر الخاصة بك."',
    '"Try adjusting or clearing your search."': '"حاول تعديل أو مسح البحث الخاص بك."',
    '"You have no tasks yet."': '"ليس لديك مهام حتى الآن."',
    '"Tap the + button to create a task."': '"انقر على زر + لإنشاء مهمة."',
    '"Auto Trigger: "': '"تشغيل تلقائي: "',
    '"New Task"': '"مهمة جديدة"',
    '"Edit Task"': '"تعديل المهمة"',
    '"Task Title"': '"عنوان المهمة"',
    '"Description (Optional)"': '"الوصف (اختياري)"',
    '"Category"': '"الفئة"',
    '"Trigger Automation on Complete ⚡"': '"تشغيل الأتمتة عند الاكتمال ⚡"',
    '"None (No Automation)"': '"لا يوجد (بدون أتمتة)"',
    '"Save Task"': '"حفظ المهمة"',
    '"Cancel"': '"إلغاء"',
    
    # AutomationHubScreen
    '"Automation Hub"': '"مركز الأتمتة"',
    '"Build \\& manage workflows"': '"بناء وإدارة سير العمل"',
    '"Run Now"': '"تشغيل الآن"',
    '"History"': '"السجل"',
    '"No scripts created yet."': '"لم يتم إنشاء أي نصوص برمجية بعد."',
    '"Tap + to create your first automation script."': '"اضغط على + لإنشاء النص البرمجي الأول للأتمتة."',
    '"Execution History"': '"سجل التنفيذ"',
    '"Clear History"': '"مسح السجل"',
    '"Close"': '"إغلاق"',
    '"No execution history found."': '"لم يتم العثور على سجل تنفيذ."',
    '"Script / Workflow Name"': '"اسم النص / سير العمل"',
    '"Short Description"': '"وصف قصير"',
    '"Automation Type"': '"نوع الأتمتة"',
    '"HTTP Method"': '"طريقة HTTP"',
    '"JSON Payload Body"': '"حمولة JSON (اختياري)"',
    '"Schedule / Trigger"': '"الجدولة / المُشغِّل"',
    '"Pin to Dashboard Quick Actions"': '"تثبيت في الإجراءات السريعة بالرئيسية"',
    '"Save Script"': '"حفظ النص"',
    '"Termux Command"': '"أمر تيرموكس (Termux)"',
    '"Webhook / API Request"': '"طلب ويب هوك / API"',
    '"Manual (No Schedule)"': '"يدوي (بدون جدولة)"',
    '"Daily at 9:00 AM"': '"يومياً الساعة 9:00 صباحاً"',
    '"Every 1 Hour"': '"كل ساعة"',
    '"On Device Boot"': '"عند تشغيل الجهاز"'
}

files = glob.glob('app/src/main/java/com/example/ui/**/*.kt', recursive=True)
for file_path in files:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
        
    original = content
    for old, new in translations.items():
        content = content.replace(old, new)
        
    if content != original:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f'Translated {file_path}')
