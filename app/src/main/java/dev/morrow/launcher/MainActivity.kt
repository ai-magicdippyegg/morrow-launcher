package dev.morrow.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.text.Editable
import android.text.TextWatcher
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class LaunchableApp(
    val packageName: String,
    val label: String,
    val icon: android.graphics.drawable.Drawable
)

private data class Palette(
    val background: Int,
    val surface: Int,
    val surfaceRaised: Int,
    val ink: Int,
    val muted: Int,
    val line: Int,
    val accent: Int,
    val forest: Int,
    val forestDeep: Int,
    val paper: Int
)

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val preferences by lazy { getSharedPreferences("morrow-settings", MODE_PRIVATE) }
    private val isDark: Boolean
        get() = preferences.getBoolean("midnight", false)

    private val palette: Palette
        get() = if (isDark) {
            Palette(
                background = 0xFF171F1B.toInt(),
                surface = 0xFF202B25.toInt(),
                surfaceRaised = 0xFF29362F.toInt(),
                ink = 0xFFF1EEE5.toInt(),
                muted = 0xFFADB7AD.toInt(),
                line = 0xFF3A4940.toInt(),
                accent = 0xFFE5A078.toInt(),
                forest = 0xFF30483C.toInt(),
                forestDeep = 0xFF1B2A22.toInt(),
                paper = 0xFFF1EEE5.toInt()
            )
        } else {
            Palette(
                background = 0xFFF3F0E7.toInt(),
                surface = 0xFFFBF9F2.toInt(),
                surfaceRaised = 0xFFEAE5D9.toInt(),
                ink = 0xFF202B25.toInt(),
                muted = 0xFF7C857D.toInt(),
                line = 0xFFDDD8CC.toInt(),
                accent = 0xFFC96D4B.toInt(),
                forest = 0xFF31483D.toInt(),
                forestDeep = 0xFF1C2B23.toInt(),
                paper = 0xFFF3F0E7.toInt()
            )
        }

    private var showingLibrary = false
    private var clockView: TextView? = null
    private var libraryGrid: LinearLayout? = null
    private var libraryCount: TextView? = null
    private var allApps: List<LaunchableApp> = emptyList()

    private val clockTick = object : Runnable {
        override fun run() {
            clockView?.text = currentTime()
            val delay = 60_000L - (System.currentTimeMillis() % 60_000L)
            handler.postDelayed(this, delay)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        allApps = loadApps()
        renderHome()
    }

    override fun onResume() {
        super.onResume()
        allApps = loadApps()
        clockView?.text = currentTime()
        handler.removeCallbacks(clockTick)
        handler.post(clockTick)
    }

    override fun onPause() {
        handler.removeCallbacks(clockTick)
        super.onPause()
    }

    @Deprecated("Deprecated in Android 13, retained for compatibility with older devices")
    override fun onBackPressed() {
        if (showingLibrary) {
            renderHome()
        } else {
            super.onBackPressed()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun currentTime(): String =
        SimpleDateFormat("h:mm", Locale.getDefault()).format(Date())

    private fun currentDate(): String =
        SimpleDateFormat("EEEE  ·  d MMMM", Locale.getDefault())
            .format(Date())
            .uppercase(Locale.getDefault())

    private fun rounded(
        fill: Int,
        radius: Int,
        stroke: Int? = null,
        strokeWidth: Int = 1
    ): GradientDrawable = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(strokeWidth), stroke)
    }

    private fun gradient(radius: Int, first: Int, second: Int): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(first, second)
        ).apply {
            cornerRadius = dp(radius).toFloat()
        }

    private fun text(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false,
        tracking: Float = 0f
    ) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        letterSpacing = tracking
        typeface = if (bold) {
            Typeface.create("sans-serif-medium", Typeface.NORMAL)
        } else {
            Typeface.create("sans-serif", Typeface.NORMAL)
        }
        includeFontPadding = true
    }

    private fun spacer(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun configureWindow(root: View) {
        // Access decorView first: PhoneWindow's insetsController is unavailable until
        // its decor has been installed. This activity builds its content programmatically.
        val decor = window.decorView

        window.statusBarColor = palette.background
        window.navigationBarColor = palette.background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            val light = if (isDark) 0 else {
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            }
            decor.windowInsetsController?.setSystemBarsAppearance(
                light,
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
            root.setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                view.setPadding(
                    dp(24) + bars.left,
                    dp(18) + bars.top,
                    dp(24) + bars.right,
                    dp(16) + bars.bottom
                )
                insets
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = if (isDark) 0 else (
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                )
            root.setPadding(dp(24), dp(18), dp(24), dp(16))
        }
    }

    private fun newRoot(): FrameLayout = FrameLayout(this).apply {
        setBackgroundColor(palette.background)
        configureWindow(this)
    }

    private fun loadApps(): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, 0)
            .mapNotNull { resolve ->
                val info = resolve.activityInfo ?: return@mapNotNull null
                if (info.packageName == packageName) return@mapNotNull null
                val appInfo: ApplicationInfo = info.applicationInfo ?: return@mapNotNull null
                LaunchableApp(
                    packageName = info.packageName,
                    label = appInfo.loadLabel(packageManager).toString(),
                    icon = appInfo.loadIcon(packageManager)
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    private fun renderHome() {
        showingLibrary = false
        libraryGrid = null
        libraryCount = null
        allApps = loadApps()

        val root = newRoot()
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            clipToPadding = false
        }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brandStack = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val brandLine = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brandMark = View(this).apply {
            background = rounded(palette.accent, 5)
            layoutParams = LinearLayout.LayoutParams(dp(9), dp(9)).apply {
                rightMargin = dp(10)
            }
        }
        brandLine.addView(brandMark)
        brandLine.addView(text("MORROW", 16f, palette.ink, true, 0.18f))
        brandStack.addView(brandLine)
        brandStack.addView(text("A QUIETER KIND OF HOME", 9f, palette.muted, true, 0.15f).apply {
            setPadding(dp(19), dp(3), 0, 0)
        })
        topBar.addView(brandStack, LinearLayout.LayoutParams(0, -2, 1f))

        val settings = TextView(this).apply {
            text = "···"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(palette.ink)
            background = rounded(palette.surfaceRaised, 18)
            contentDescription = "Morrow settings"
            setOnClickListener { showSettings() }
        }
        topBar.addView(settings, LinearLayout.LayoutParams(dp(48), dp(42)))
        page.addView(topBar)

        page.addView(spacer(46))
        page.addView(text(currentDate(), 11f, palette.muted, true, 0.11f))

        clockView = text(currentTime(), 78f, palette.ink, false, -0.055f).apply {
            includeFontPadding = false
            setPadding(0, dp(4), 0, 0)
        }
        page.addView(clockView, LinearLayout.LayoutParams(-1, -2))
        page.addView(text("Make room for what matters.", 16f, palette.muted).apply {
            setPadding(0, dp(10), 0, 0)
        })

        page.addView(spacer(32))
        page.addView(createHeroCard())
        page.addView(spacer(32))

        val section = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        section.addView(text("YOUR SHORTCUTS", 11f, palette.ink, true, 0.12f))
        section.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        val shortcutCount = preferredApps().size.coerceAtMost(4)
        section.addView(text("$shortcutCount PINNED", 9f, palette.muted, true, 0.08f))
        page.addView(section)
        page.addView(spacer(17))

        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val shortcuts = preferredApps()
        if (shortcuts.isEmpty()) {
            dock.addView(text("Your installed apps will appear here.", 13f, palette.muted))
        } else {
            shortcuts.forEach { app ->
                dock.addView(createHomeShortcut(app), LinearLayout.LayoutParams(0, -2, 1f).apply {
                    marginEnd = dp(6)
                })
            }
        }
        page.addView(dock)
        page.addView(spacer(26))
        page.addView(createLibraryButton())
        page.addView(spacer(26))
        page.addView(text("TAKE IT ONE SCREEN AT A TIME", 9f, palette.muted, true, 0.14f).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(8))
        })

        scroll.addView(page)
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        clockView?.text = currentTime()
        handler.removeCallbacks(clockTick)
        handler.post(clockTick)
    }

    private fun createHeroCard(): View {
        val p = palette
        val card = FrameLayout(this).apply {
            background = gradient(28, p.forest, p.forestDeep)
            clipToOutline = true
        }

        val words = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(22), dp(22), dp(112), dp(22))
        }
        words.addView(text("A LITTLE LESS NOISE.", 10f, 0xFFE5B68D.toInt(), true, 0.14f))
        words.addView(spacer(11))
        words.addView(text("Your phone.\nYour pace.", 24f, 0xFFF4F0E5.toInt(), true).apply {
            setLineSpacing(dp(1).toFloat(), 1f)
        })
        words.addView(spacer(9))
        words.addView(text("A home for the next thing.", 12f, 0xFFCCD4C8.toInt()))
        card.addView(words, FrameLayout.LayoutParams(-1, -1))
        card.addView(
            OrbitMark(this, 0xFFE6B27C.toInt(), 0xFF9EB6A2.toInt()),
            FrameLayout.LayoutParams(dp(124), dp(124), Gravity.END or Gravity.CENTER_VERTICAL).apply {
                rightMargin = dp(7)
            }
        )
        card.layoutParams = LinearLayout.LayoutParams(-1, dp(184))
        return card
    }

    private fun preferredApps(): List<LaunchableApp> {
        val byPackage = allApps.associateBy { it.packageName }
        val preferred = listOf(
            "com.google.android.dialer",
            "com.android.dialer",
            "com.google.android.apps.messaging",
            "com.android.messaging",
            "com.android.chrome",
            "com.google.android.apps.maps",
            "com.android.camera2",
            "com.google.android.contacts"
        )
        val chosen = preferred.mapNotNull { byPackage[it] }.distinctBy { it.packageName }.take(4)
        return if (chosen.size >= 4) chosen else {
            (chosen + allApps.filterNot { app -> chosen.any { it.packageName == app.packageName } })
                .distinctBy { it.packageName }
                .take(4)
        }
    }

    private fun createHomeShortcut(app: LaunchableApp): View {
        val holder = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setOnClickListener { openApp(app) }
        }
        val iconSurface = FrameLayout(this).apply {
            background = rounded(palette.surface, 22, palette.line, 1)
            addView(ImageView(this@MainActivity).apply {
                setImageDrawable(app.icon)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(dp(11), dp(11), dp(11), dp(11))
            }, FrameLayout.LayoutParams(-1, -1))
        }
        holder.addView(iconSurface, LinearLayout.LayoutParams(dp(58), dp(58)))
        holder.addView(text(app.label, 10f, palette.muted, true).apply {
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(0, dp(9), 0, 0)
        }, LinearLayout.LayoutParams(-1, -2))
        return holder
    }

    private fun createLibraryButton(): View {
        val button = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(palette.surface, 22, palette.line, 1)
            isClickable = true
            isFocusable = true
            setPadding(dp(19), dp(15), dp(18), dp(15))
            setOnClickListener { showLibrary() }
        }
        val copy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        copy.addView(text("OPEN APP LIBRARY", 11f, palette.ink, true, 0.09f))
        copy.addView(text("${allApps.size} apps, all in one place", 11f, palette.muted).apply {
            setPadding(0, dp(4), 0, 0)
        })
        button.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
        button.addView(text("↗", 22f, palette.accent, true))
        return button
    }

    private fun showLibrary() {
        showingLibrary = true
        allApps = loadApps()

        val root = newRoot()
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val headingStack = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        headingStack.addView(text("THE LIBRARY", 11f, palette.muted, true, 0.14f))
        headingStack.addView(text("Everything, in its place.", 24f, palette.ink, true).apply {
            setPadding(0, dp(5), 0, 0)
        })
        header.addView(headingStack, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(TextView(this).apply {
            text = "HOME  ↗"
            textSize = 10f
            letterSpacing = 0.08f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(palette.accent)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(12), 0, dp(12))
            setOnClickListener { renderHome() }
        })
        page.addView(header)
        page.addView(spacer(23))

        val search = EditText(this).apply {
            hint = "Find an app"
            textSize = 14f
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setTextColor(palette.ink)
            setHintTextColor(palette.muted)
            background = rounded(palette.surface, 17, palette.line, 1)
            setPadding(dp(17), dp(13), dp(17), dp(13))
        }
        page.addView(search, LinearLayout.LayoutParams(-1, dp(52)))
        page.addView(spacer(17))

        val countRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        countRow.addView(text("INSTALLED APPS", 10f, palette.ink, true, 0.12f))
        countRow.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        libraryCount = text("", 10f, palette.muted, true, 0.08f)
        countRow.addView(libraryCount)
        page.addView(countRow)
        page.addView(spacer(12))

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            clipToPadding = false
        }
        libraryGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(libraryGrid, FrameLayout.LayoutParams(-1, -2))
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                populateLibrary(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        root.addView(page, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        populateLibrary("")
        search.requestFocus()
    }

    private fun populateLibrary(query: String) {
        val grid = libraryGrid ?: return
        val filtered = allApps.filter { it.label.contains(query.trim(), ignoreCase = true) }
        grid.removeAllViews()
        libraryCount?.text = "${filtered.size} APPS"

        if (filtered.isEmpty()) {
            grid.addView(text("No apps found. Try another search.", 13f, palette.muted).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(34), 0, dp(34))
            })
            return
        }

        filtered.chunked(3).forEach { rowApps ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.TOP
            }
            rowApps.forEach { app ->
                row.addView(createLibraryApp(app), LinearLayout.LayoutParams(0, dp(112), 1f).apply {
                    rightMargin = dp(5)
                    bottomMargin = dp(8)
                })
            }
            repeat(3 - rowApps.size) {
                row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
            }
            grid.addView(row)
        }
    }

    private fun createLibraryApp(app: LaunchableApp): View {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(palette.surface, 19, palette.line, 1)
            isClickable = true
            isFocusable = true
            setOnClickListener { openApp(app) }
            setPadding(dp(5), dp(10), dp(5), dp(8))
        }
        tile.addView(ImageView(this).apply {
            setImageDrawable(app.icon)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }, LinearLayout.LayoutParams(dp(43), dp(43)))
        tile.addView(text(app.label, 11f, palette.ink, true).apply {
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(dp(3), dp(8), dp(3), 0)
        }, LinearLayout.LayoutParams(-1, -2))
        return tile
    }

    private fun openApp(app: LaunchableApp) {
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            renderHome()
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                startActivity(launchIntent)
            } catch (_: Exception) {
                showLaunchError(app.label)
            }
        } else {
            showLaunchError(app.label)
        }
    }

    private fun showLaunchError(label: String) {
        AlertDialog.Builder(this)
            .setTitle("Couldn't open $label")
            .setMessage("Android didn't provide a launch action for this app.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSettings() {
        val themeChoice = if (isDark) "Switch to warm paper" else "Switch to midnight"
        AlertDialog.Builder(this)
            .setTitle("Morrow")
            .setItems(arrayOf(themeChoice, "About Morrow")) { _, which ->
                when (which) {
                    0 -> {
                        preferences.edit().putBoolean("midnight", !isDark).apply()
                        renderHome()
                    }
                    1 -> showAbout()
                }
            }
            .setNegativeButton("CLOSE", null)
            .show()
    }

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle("Morrow 0.1.0")
            .setMessage(
                "A quieter kind of home.\n\n" +
                    "Built from scratch with native Android views. " +
                    "No analytics, no ads, and no network permission.\n\n" +
                    "Made for a little more breathing room."
            )
            .setPositiveButton("NICE", null)
            .show()
    }
}

private class OrbitMark(
    context: android.content.Context,
    private val warm: Int,
    private val cool: Int
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width * 0.52f
        val cy = height * 0.50f
        val scale = minOf(width, height).toFloat()
        val outer = scale * 0.43f

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scale * 0.009f
        paint.color = 0x66D9E0D4
        canvas.drawCircle(cx, cy, outer * 0.82f, paint)

        val orbit = Path().apply {
            addOval(
                cx - outer,
                cy - outer * 0.40f,
                cx + outer,
                cy + outer * 0.40f,
                Path.Direction.CW
            )
        }
        canvas.save()
        canvas.rotate(-34f, cx, cy)
        paint.color = 0x77D9E0D4
        canvas.drawPath(orbit, paint)
        canvas.restore()

        paint.style = Paint.Style.FILL
        paint.color = warm
        canvas.drawCircle(cx - outer * 0.27f, cy - outer * 0.20f, outer * 0.24f, paint)

        paint.color = cool
        canvas.drawCircle(cx + outer * 0.48f, cy + outer * 0.24f, outer * 0.09f, paint)

        paint.color = 0xFFE9D6B6.toInt()
        canvas.drawCircle(cx + outer * 0.54f, cy - outer * 0.47f, outer * 0.045f, paint)
    }
}