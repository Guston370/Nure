# PowerShell script to generate Android app icons from source image
param(
    [string]$SourceImage = "20251013_1425_Nure Health Icon_simple_compose_01k7ecdcvdenns98xgasctpd3f.png"
)

# Check if source image exists
if (-not (Test-Path $SourceImage)) {
    Write-Error "Source image not found: $SourceImage"
    exit 1
}

Write-Host "Generating Android app icons from: $SourceImage"

# Define icon sizes for different densities
$iconSizes = @{
    "mdpi" = 48
    "hdpi" = 72
    "xhdpi" = 96
    "xxhdpi" = 144
    "xxxhdpi" = 192
}

# Load System.Drawing assembly for image processing
Add-Type -AssemblyName System.Drawing

try {
    # Load the source image
    $sourceImg = [System.Drawing.Image]::FromFile((Resolve-Path $SourceImage).Path)
    Write-Host "Source image loaded: $($sourceImg.Width)x$($sourceImg.Height)"
    
    # Generate icons for each density
    foreach ($density in $iconSizes.Keys) {
        $size = $iconSizes[$density]
        $outputDir = "app\src\main\res\mipmap-$density"
        
        # Create directory if it doesn't exist
        if (-not (Test-Path $outputDir)) {
            New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
        }
        
        # Create resized bitmap
        $resizedImg = New-Object System.Drawing.Bitmap($size, $size)
        $graphics = [System.Drawing.Graphics]::FromImage($resizedImg)
        
        # Set high quality rendering
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
        
        # Draw the resized image
        $graphics.DrawImage($sourceImg, 0, 0, $size, $size)
        
        # Save as PNG
        $outputPath = "$outputDir\ic_launcher.png"
        $resizedImg.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
        
        # Also create round version (same image for now)
        $outputPathRound = "$outputDir\ic_launcher_round.png"
        $resizedImg.Save($outputPathRound, [System.Drawing.Imaging.ImageFormat]::Png)
        
        Write-Host "Generated: $outputPath ($($size)x$size)"
        
        # Clean up
        $graphics.Dispose()
        $resizedImg.Dispose()
    }
    
    # Clean up source image
    $sourceImg.Dispose()
    
    Write-Host "✅ All app icons generated successfully!"
    Write-Host "📱 Icons created for densities: $($iconSizes.Keys -join ', ')"
    
} catch {
    Write-Error "Error generating icons: $($_.Exception.Message)"
    exit 1
}