# PowerShell script to optimize the app icon for proper Android sizing
# Creates properly sized icons for adaptive icon system

Write-Host "🎨 Optimizing Nure Health app icon for Android adaptive icons..." -ForegroundColor Green

$sourceIcon = "app_icon_source.png"

if (!(Test-Path $sourceIcon)) {
    Write-Host "❌ Source icon not found: $sourceIcon" -ForegroundColor Red
    Write-Host "Using existing nure_logo.png instead..." -ForegroundColor Yellow
    $sourceIcon = "app/src/main/res/drawable/nure_logo.png"
}

Write-Host "📱 Creating optimized app icon configuration..." -ForegroundColor Yellow

# Create a better app_icon.xml that properly handles the PNG
$appIconXml = @"
<?xml version="1.0" encoding="utf-8"?>
<!-- Health Scanner App Icon - Optimized for Adaptive Icons -->
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Background layer for better visibility -->
    <item>
        <shape android:shape="oval">
            <solid android:color="#FFFFFF" />
            <size android:width="108dp" android:height="108dp" />
        </shape>
    </item>
    
    <!-- Main icon with proper safe zone sizing -->
    <item
        android:drawable="@drawable/nure_logo"
        android:gravity="center">
        <!-- Inset to fit within adaptive icon safe zone (66dp out of 108dp) -->
        <inset
            android:insetLeft="21dp"
            android:insetTop="21dp"
            android:insetRight="21dp"
            android:insetBottom="21dp" />
    </item>
</layer-list>
"@

# Write the optimized app_icon.xml
$appIconXml | Out-File -FilePath "app/src/main/res/drawable/app_icon.xml" -Encoding UTF8

Write-Host "✅ App icon optimized!" -ForegroundColor Green

# Create an alternative vector-based foreground
$vectorForeground = @"
<?xml version="1.0" encoding="utf-8"?>
<!-- Alternative Vector Foreground for Adaptive Icon -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    
    <!-- Nure Health Logo Recreation -->
    <!-- Main circle background -->
    <path
        android:fillColor="#0FB8AD"
        android:pathData="M54,54m-30,0a30,30 0,1 1,60 0a30,30 0,1 1,-60 0" />
    
    <!-- Health cross symbol -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M49,39 L59,39 L59,49 L69,49 L69,59 L59,59 L59,69 L49,69 L49,59 L39,59 L39,49 L49,49 Z" />
    
    <!-- Accent elements -->
    <path
        android:fillColor="#3CCF91"
        android:pathData="M54,54m-35,0a35,35 0,0 1,70 0a35,35 0,0 1,-70 0"
        android:strokeWidth="2"
        android:strokeColor="#FFFFFF"
        android:fillType="evenOdd" />
        
</vector>
"@

# Write the vector alternative
$vectorForeground | Out-File -FilePath "app/src/main/res/drawable/ic_launcher_foreground_vector.xml" -Encoding UTF8

Write-Host "📊 Icon Optimization Summary:" -ForegroundColor Magenta
Write-Host "  • Optimized app_icon.xml with proper safe zone sizing" -ForegroundColor White
Write-Host "  • Added inset margins for adaptive icon compatibility" -ForegroundColor White
Write-Host "  • Created vector alternative for perfect scaling" -ForegroundColor White
Write-Host "  • Maintained Nure Health branding and colors" -ForegroundColor White

Write-Host "`n✅ App icon is now properly sized for Android adaptive icons!" -ForegroundColor Green