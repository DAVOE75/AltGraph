package com.example.altgraph

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import java.io.File
import java.util.Locale

object FontHelper {

    private var appContext: Context? = null
    private val typefaceCache = mutableMapOf<String, Typeface>()

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    data class FontOption(
        val key: String,
        val labelEs: String,
        val labelEn: String,
        val labelFr: String,
        val labelIt: String,
        val labelDe: String,
        val scaleX: Float
    ) {
        fun getLocalizedLabel(): String {
            val lang = Locale.getDefault().language.lowercase()
            return when {
                lang.startsWith("es") -> labelEs
                lang.startsWith("fr") -> labelFr
                lang.startsWith("it") -> labelIt
                lang.startsWith("de") -> labelDe
                else -> labelEn
            }
        }
    }

    val options = listOf(
        FontOption("landasans", "Landasans (Ultra Condensed)", "Landasans (Ultra Condensed)", "Landasans (Ultra Étroite)", "Landasans (Ultra Stretta)", "Landasans (Ultra Schmal)", 0.70f),
        FontOption("barlow-condensed", "Barlow Condensed (Pro)", "Barlow Condensed (Pro)", "Barlow Condensed (Pro)", "Barlow Condensed (Pro)", "Barlow Condensed (Pro)", 0.85f),
        FontOption("sans-serif-condensed", "Condensed (Estrecha/Alta)", "Condensed (Narrow/Tall)", "Condensed (Étroite)", "Condensed (Stretta)", "Condensed (Schmal)", 0.72f),
        FontOption("sans-serif", "Sans-Serif (Estándar)", "Sans-Serif (Standard)", "Sans-Serif (Standard)", "Sans-Serif (Standard)", "Sans-Serif (Standard)", 1.0f),
        FontOption("sans-serif-medium", "Sans Medium", "Sans Medium", "Sans Medium", "Sans Medium", "Sans Medium", 1.0f),
        FontOption("sans-serif-black", "Sans Black (Extra Gruesa)", "Sans Black (Heavy)", "Sans Black (Épaisse)", "Sans Black (Pesante)", "Sans Black (Extradick)", 1.15f),
        FontOption("monospace", "Monospace Digital", "Monospace Digital", "Monospacé Digital", "Monospace Digitale", "Monospace Digital", 1.0f)
    )

    fun getTypeface(familyKey: String, style: Int = Typeface.BOLD, context: Context? = null): Typeface {
        val cacheKey = "$familyKey-$style"
        typefaceCache[cacheKey]?.let { return it }

        val ctx = context ?: appContext
        val tf = try {
            when (familyKey) {
                "landasans" -> {
                    if (ctx != null) {
                        try {
                            Typeface.createFromAsset(ctx.assets, "fonts/Landasans-Medium.otf")
                        } catch (e: Exception) {
                            Typeface.create("sans-serif-condensed", style)
                        }
                    } else {
                        Typeface.create("sans-serif-condensed", style)
                    }
                }
                "barlow-condensed" -> {
                    if (ctx != null) {
                        try {
                            val assetName = if (style == Typeface.BOLD) "fonts/BarlowCondensed-Bold.ttf" else "fonts/BarlowCondensed-Medium.ttf"
                            Typeface.createFromAsset(ctx.assets, assetName)
                        } catch (e: Exception) {
                            Typeface.create("sans-serif-condensed", style)
                        }
                    } else {
                        Typeface.create("sans-serif-condensed", style)
                    }
                }
                "sans-serif-condensed" -> {
                    val base = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
                    Typeface.create(base, style)
                }
                "sans-serif" -> {
                    Typeface.create(Typeface.SANS_SERIF, style)
                }
                "sans-serif-medium" -> {
                    val base = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    Typeface.create(base, style)
                }
                "sans-serif-black" -> {
                    val base = Typeface.create("sans-serif-black", Typeface.NORMAL)
                    Typeface.create(base, style)
                }
                "monospace" -> {
                    Typeface.create(Typeface.MONOSPACE, style)
                }
                else -> {
                    Typeface.create("sans-serif-condensed", style)
                }
            }
        } catch (e: Exception) {
            Typeface.create("sans-serif-condensed", style)
        }

        typefaceCache[cacheKey] = tf
        return tf
    }

    fun getScaleX(familyKey: String): Float {
        return options.find { it.key == familyKey }?.scaleX ?: 0.72f
    }

    fun applyFontToPaint(paint: Paint, familyKey: String, style: Int = Typeface.BOLD, context: Context? = null) {
        paint.typeface = getTypeface(familyKey, style, context)
        paint.textScaleX = getScaleX(familyKey)
    }
}