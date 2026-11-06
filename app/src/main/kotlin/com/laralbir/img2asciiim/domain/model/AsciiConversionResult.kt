package com.laralbir.img2asciiim.domain.model

/**
 * Validated, budget-compliant output of the offline conversion pipeline.
 */
data class AsciiConversionResult(
  val rawArt: String,
  val formattedMessage: String,
  val columns: Int,
  val rows: Int,
  val requestedColumns: Int,
  val charCount: Int,
  val utf8ByteCount: Int,
  val maxCharBudget: Int,
  val maxByteBudget: Int?,
  val wasAutoScaled: Boolean,
  val usesMonospaceFence: Boolean,
  val proportionalFontWarning: Boolean,
)
