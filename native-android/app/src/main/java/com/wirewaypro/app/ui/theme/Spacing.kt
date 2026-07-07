package com.wirewaypro.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The spacing scale — one source of truth for gaps, insets, and paddings.
 *
 * A 4dp base grid keeps rhythm consistent across ~30 screens without every screen
 * hardcoding its own `16.dp`. Field-usability tokens ([touchTarget], [screen]) encode
 * the non-obvious job-site rules: a gloved thumb needs a bigger hit area than
 * Material's 48dp minimum, and screen gutters stay wide so content never hugs the
 * bezel where a cracked-screen-protector or sunlight glare eats the edge.
 */
object Spacing {
    /** 2dp — hairline gaps (e.g. a value and its caption). */
    val xxs: Dp = 2.dp
    /** 4dp — the base grid unit; tight icon/label gaps. */
    val xs: Dp = 4.dp
    /** 8dp — related items in a row. */
    val sm: Dp = 8.dp
    /** 12dp — intra-card grouping. */
    val md: Dp = 12.dp
    /** 16dp — the default content padding / gap between cards. */
    val lg: Dp = 16.dp
    /** 20dp — card interior padding for breathing room. */
    val xl: Dp = 20.dp
    /** 24dp — section separation. */
    val xxl: Dp = 24.dp
    /** 32dp — major blocks / hero breathing space. */
    val xxxl: Dp = 32.dp

    /** Horizontal gutter for screen bodies — wide so content clears the bezel. */
    val screen: Dp = 16.dp

    /**
     * Minimum interactive height for anything a gloved thumb taps. Larger than
     * Material's 48dp because field gloves cost precision; primary actions on core
     * screens should meet this, not just the 48dp floor.
     */
    val touchTarget: Dp = 56.dp
}
