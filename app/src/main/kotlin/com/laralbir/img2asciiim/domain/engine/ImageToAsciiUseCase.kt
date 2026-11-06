package com.laralbir.img2asciiim.domain.engine

import com.laralbir.img2asciiim.domain.model.AsciiConversionResult
import com.laralbir.img2asciiim.domain.model.CharsetMode
import com.laralbir.img2asciiim.domain.model.ConversionParams
import com.laralbir.img2asciiim.domain.model.GrayscaleGrid
import com.laralbir.img2asciiim.domain.model.ImPlatformPreset
import com.laralbir.img2asciiim.domain.model.PixelGrid

/**
 * End-to-end pure Kotlin image-to-IM-text pipeline with binary-search auto-scaling (`DD-02 Opción A`)
 * guaranteeing both character count (`maxCharacters`) and UTF-8 byte count (`maxUtf8Bytes`) budgets.
 */
class ImageToAsciiUseCase(
  private val luminanceExtractor: LuminanceExtractor = LuminanceExtractor(),
  private val toneAdjuster: ToneAdjuster = ToneAdjuster(),
  private val resampler: AspectRatioResampler = AspectRatioResampler(),
  private val ditheringEngine: DitheringEngine = DitheringEngine(),
  private val brailleRenderer: BrailleUnicodeRenderer = BrailleUnicodeRenderer(),
  private val characterRampRenderer: CharacterRampRenderer = CharacterRampRenderer(),
  private val messageFormatter: ImMessageFormatter = ImMessageFormatter(),
) {

  operator fun invoke(
    pixelGrid: PixelGrid,
    params: ConversionParams = ConversionParams(),
  ): AsciiConversionResult {
    val preset = params.preset
    val charsetMode = params.charsetMode
    val requestedCols = params.widthColumns.coerceIn(MIN_REQUESTED_COLUMNS, MAX_REQUESTED_COLUMNS)
    val usesFence = messageFormatter.usesMonospaceFence(preset, charsetMode)
    val proportionalWarning = messageFormatter.hasProportionalFontWarning(preset, charsetMode)

    // Extract luminance once for this conversion pass; candidate column widths reuse `sourceLuminance`
    val sourceLuminance = luminanceExtractor.extract(pixelGrid, invert = params.invert)

    val initialAttempt = renderAtColumns(
      sourceLuminance = sourceLuminance,
      columns = requestedCols,
      params = params,
      usesFence = usesFence,
    )
    if (fitsBudget(initialAttempt.formattedMessage, initialAttempt.utf8ByteCount, preset)) {
      return initialAttempt.toResult(
        requestedColumns = requestedCols,
        preset = preset,
        wasAutoScaled = false,
        usesMonospaceFence = usesFence,
        proportionalFontWarning = proportionalWarning,
      )
    }

    // Binary-search largest `cols` in `MIN_AUTOSCALE_COLUMNS..(requestedCols - 1)` that fits both budgets
    var low = MIN_AUTOSCALE_COLUMNS
    var high = requestedCols - 1
    var bestFitting: RenderedCandidate? = null

    while (low <= high) {
      val mid = (low + high) ushr 1
      val candidate = renderAtColumns(
        sourceLuminance = sourceLuminance,
        columns = mid,
        params = params,
        usesFence = usesFence,
      )
      if (fitsBudget(candidate.formattedMessage, candidate.utf8ByteCount, preset)) {
        bestFitting = candidate
        low = mid + 1
      } else {
        high = mid - 1
      }
    }

    if (bestFitting != null) {
      return bestFitting.toResult(
        requestedColumns = requestedCols,
        preset = preset,
        wasAutoScaled = true,
        usesMonospaceFence = usesFence,
        proportionalFontWarning = proportionalWarning,
      )
    }

    // Final safety net for extreme aspect ratios (e.g. 1:50 ultra-tall strip where even cols=4 overflows):
    // render at MIN_AUTOSCALE_COLUMNS and binary-search the maximum number of rows that fit 100% cleanly.
    val minColsCandidate = renderAtColumns(
      sourceLuminance = sourceLuminance,
      columns = MIN_AUTOSCALE_COLUMNS,
      params = params,
      usesFence = usesFence,
    )
    val truncatedCandidate = truncateRowsToFitBudget(
      candidate = minColsCandidate,
      preset = preset,
      charsetMode = charsetMode,
    )
    return truncatedCandidate.toResult(
      requestedColumns = requestedCols,
      preset = preset,
      wasAutoScaled = true,
      usesMonospaceFence = usesFence,
      proportionalFontWarning = proportionalWarning,
    )
  }

  private fun renderAtColumns(
    sourceLuminance: GrayscaleGrid,
    columns: Int,
    params: ConversionParams,
    usesFence: Boolean,
  ): RenderedCandidate {
    val dims = resampler.computeTargetDimensions(
      sourceWidth = sourceLuminance.width,
      sourceHeight = sourceLuminance.height,
      columns = columns,
      charsetMode = params.charsetMode,
    )
    val resampled = resampler.resample(
      grid = sourceLuminance,
      sampleWidth = dims.sampleWidth,
      sampleHeight = dims.sampleHeight,
    )
    val adjustedInk = toneAdjuster.adjust(
      grid = resampled,
      brightness = params.brightness,
      contrast = params.contrast,
      invert = params.invert,
    )
    val levels = characterRampRenderer.levelCount(params.charsetMode, usesFence)
    val quantized = ditheringEngine.quantize(
      grid = adjustedInk,
      levels = levels,
      mode = params.ditherMode,
    )

    val rawArt = when (params.charsetMode) {
      CharsetMode.BRAILLE_UNICODE -> brailleRenderer.render(
        subpixelLevels = quantized,
        sampleWidth = dims.sampleWidth,
        sampleHeight = dims.sampleHeight,
      )
      CharsetMode.ASCII_CLASSIC,
      CharsetMode.UNICODE_BLOCKS -> characterRampRenderer.render(
        quantizedLevels = quantized,
        columns = dims.columns,
        rows = dims.rows,
        charsetMode = params.charsetMode,
        usesMonospaceFence = usesFence,
      )
    }

    val formatted = messageFormatter.format(
      rawArt = rawArt,
      preset = params.preset,
      charsetMode = params.charsetMode,
    )
    val utf8Bytes = formatted.encodeToByteArray().size
    return RenderedCandidate(
      rawArt = rawArt,
      formattedMessage = formatted,
      columns = dims.columns,
      rows = dims.rows,
      utf8ByteCount = utf8Bytes,
    )
  }

  private fun truncateRowsToFitBudget(
    candidate: RenderedCandidate,
    preset: ImPlatformPreset,
    charsetMode: CharsetMode,
  ): RenderedCandidate {
    val lines = candidate.rawArt.split('\n')
    var low = 1
    var high = lines.size
    var bestRaw = lines.first()
    var bestFormatted = messageFormatter.format(bestRaw, preset, charsetMode)
    var bestBytes = bestFormatted.encodeToByteArray().size
    var bestRows = 1

    while (low <= high) {
      val mid = (low + high) ushr 1
      val subRaw = lines.subList(0, mid).joinToString("\n")
      val subFormatted = messageFormatter.format(subRaw, preset, charsetMode)
      val subBytes = subFormatted.encodeToByteArray().size
      if (fitsBudget(subFormatted, subBytes, preset)) {
        bestRaw = subRaw
        bestFormatted = subFormatted
        bestBytes = subBytes
        bestRows = mid
        low = mid + 1
      } else {
        high = mid - 1
      }
    }

    return RenderedCandidate(
      rawArt = bestRaw,
      formattedMessage = bestFormatted,
      columns = candidate.columns,
      rows = bestRows,
      utf8ByteCount = bestBytes,
    )
  }

  private fun fitsBudget(
    formatted: String,
    utf8ByteCount: Int,
    preset: ImPlatformPreset,
  ): Boolean {
    if (formatted.length > preset.maxCharacters) return false
    val maxBytes = preset.maxUtf8Bytes
    return maxBytes == null || utf8ByteCount <= maxBytes
  }

  private data class RenderedCandidate(
    val rawArt: String,
    val formattedMessage: String,
    val columns: Int,
    val rows: Int,
    val utf8ByteCount: Int,
  ) {
    fun toResult(
      requestedColumns: Int,
      preset: ImPlatformPreset,
      wasAutoScaled: Boolean,
      usesMonospaceFence: Boolean,
      proportionalFontWarning: Boolean,
    ): AsciiConversionResult = AsciiConversionResult(
      rawArt = rawArt,
      formattedMessage = formattedMessage,
      columns = columns,
      rows = rows,
      requestedColumns = requestedColumns,
      charCount = formattedMessage.length,
      utf8ByteCount = utf8ByteCount,
      maxCharBudget = preset.maxCharacters,
      maxByteBudget = preset.maxUtf8Bytes,
      wasAutoScaled = wasAutoScaled,
      usesMonospaceFence = usesMonospaceFence,
      proportionalFontWarning = proportionalFontWarning,
    )
  }

  companion object {
    const val MIN_AUTOSCALE_COLUMNS: Int = 4
    const val MIN_REQUESTED_COLUMNS: Int = 8
    const val MAX_REQUESTED_COLUMNS: Int = 72
  }
}
