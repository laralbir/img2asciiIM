package com.laralbir.img2asciiim.ui.converter

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laralbir.img2asciiim.BuildConfig
import com.laralbir.img2asciiim.R
import com.laralbir.img2asciiim.data.update.UpdateCheckResult
import com.laralbir.img2asciiim.domain.model.CharsetMode
import com.laralbir.img2asciiim.domain.model.DitherMode
import com.laralbir.img2asciiim.domain.model.ImPlatformPreset
import kotlinx.coroutines.flow.collectLatest

object ConverterTestTags {
  const val SCREEN = "converter_screen"
  const val TOP_BAR = "converter_top_bar"
  const val OFFLINE_BADGE = "converter_offline_badge"
  const val BTN_GALLERY = "btn_source_gallery"
  const val BTN_DOCUMENTS = "btn_source_documents"
  const val BTN_CAMERA = "btn_source_camera"
  const val BTN_DEMO = "btn_source_demo"
  const val CHAT_BUBBLE_CARD = "chat_bubble_preview_card"
  const val ASCII_TEXT = "ascii_art_text"
  const val BTN_COPY = "btn_action_copy"
  const val BTN_SHARE = "btn_action_share"
  const val PROPORTIONAL_WARNING = "proportional_font_warning"
  const val WIDTH_OVERFLOW_WARNING = "width_overflow_warning"
  const val ERROR_BANNER = "error_banner_card"
  const val SLIDER_WIDTH = "slider_width_columns"
  const val SLIDER_BRIGHTNESS = "slider_brightness"
  const val SLIDER_CONTRAST = "slider_contrast"
  const val TOGGLE_INVERT = "toggle_invert_switch"
  const val CHIP_INCLUDE_ASCII = "chip_include_ascii"
  const val CHIP_ATTACH_DEEP_LINK = "chip_attach_deep_link"
  const val BTN_CHECK_UPDATES = "btn_check_updates"
  const val HD_MODE_EXPANDED_CARD = "hd_mode_expanded_card"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
  viewModel: ConverterViewModel,
  onPrepareCameraCaptureTarget: () -> Uri?,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

  // Activity Result Launchers
  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    viewModel.onGalleryUriSelected(uri?.toString())
  }

  val documentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    viewModel.onDocumentUriSelected(uri?.toString())
  }

  val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture(),
  ) { success ->
    viewModel.onCameraCaptureResult(pendingCameraUri?.toString(), success)
    pendingCameraUri = null
  }

  // Handle transient effects (Snackbar)
  LaunchedEffect(viewModel) {
    viewModel.effects.collectLatest { effect ->
      when (effect) {
        is ConverterUiEffect.ShowSnackbar -> {
          snackbarHostState.showSnackbar(effect.message)
        }
      }
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag(ConverterTestTags.SCREEN),
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = stringResource(R.string.app_name),
              style = MaterialTheme.typography.titleLarge,
              modifier = Modifier.semantics { heading() },
            )
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.testTag(ConverterTestTags.OFFLINE_BADGE),
            ) {
              Text(
                text = stringResource(R.string.converter_offline_pill),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }
        },
        modifier = Modifier.testTag(ConverterTestTags.TOP_BAR),
        actions = {
          IconButton(
            onClick = { viewModel.onCheckForUpdates(BuildConfig.VERSION_NAME) },
            enabled = !uiState.isCheckingUpdate,
            modifier = Modifier.testTag(ConverterTestTags.BTN_CHECK_UPDATES),
          ) {
            if (uiState.isCheckingUpdate) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
              Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = "Buscar actualizaciones en GitHub",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    bottomBar = {
      BottomActionControls(
        preset = uiState.params.preset,
        isConverting = uiState.isConverting,
        onCopy = viewModel::onCopyClicked,
        onShare = viewModel::onShareClicked,
      )
    },
  ) { innerPadding ->
    val context = LocalContext.current
    uiState.updateCheckResult?.let { updateOutcome ->
      when (updateOutcome) {
        is UpdateCheckResult.UpdateAvailable -> {
          AlertDialog(
            onDismissRequest = viewModel::onDismissUpdateDialog,
            title = { Text("Nueva versión disponible (${updateOutcome.releaseInfo.tagName})") },
            text = {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Hay una nueva versión de img2asciiIM en GitHub Releases:")
                if (updateOutcome.releaseInfo.releaseNotes.isNotBlank()) {
                  Text(
                    text = updateOutcome.releaseInfo.releaseNotes.take(300),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            },
            confirmButton = {
              Button(
                onClick = {
                  viewModel.onDismissUpdateDialog()
                  val url = updateOutcome.releaseInfo.apkDownloadUrl ?: updateOutcome.releaseInfo.htmlUrl
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                  context.startActivity(intent)
                },
              ) {
                Text("Descargar APK")
              }
            },
            dismissButton = {
              OutlinedButton(onClick = viewModel::onDismissUpdateDialog) {
                Text("Más tarde")
              }
            },
          )
        }
        is UpdateCheckResult.UpToDate -> {
          AlertDialog(
            onDismissRequest = viewModel::onDismissUpdateDialog,
            title = { Text("Estás al día") },
            text = { Text("Tienes instalada la versión más reciente (${updateOutcome.currentVersion}).") },
            confirmButton = {
              Button(onClick = viewModel::onDismissUpdateDialog) {
                Text("Aceptar")
              }
            },
          )
        }
        is UpdateCheckResult.OfflineOrError -> {
          AlertDialog(
            onDismissRequest = viewModel::onDismissUpdateDialog,
            title = { Text("Comprobación de actualización") },
            text = { Text(updateOutcome.message) },
            confirmButton = {
              Button(onClick = viewModel::onDismissUpdateDialog) {
                Text("Aceptar")
              }
            },
          )
        }
      }
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // 1. Error Banner
      uiState.errorBannerMessage?.let { errorMessage ->
        Card(
          modifier = Modifier.fillMaxWidth().testTag(ConverterTestTags.ERROR_BANNER),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
          ),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
            )
            Text(
              text = errorMessage,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.weight(1f),
            )
            IconButton(
              onClick = viewModel::onDismissErrorBanner,
              modifier = Modifier.semantics {
                contentDescription = "Cerrar aviso de error"
              },
            ) {
              Icon(Icons.Default.Close, contentDescription = null)
            }
          }
        }
      }

      // 2. Image Source Row
      ImageSourceSection(
        sourceOrigin = uiState.sourceOrigin,
        sourceLabel = uiState.sourceLabel,
        dimensionsLabel = uiState.sourceDimensionsLabel,
        isLoading = uiState.isLoadingImage,
        onGalleryClick = {
          galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
          )
        },
        onDocumentClick = {
          documentLauncher.launch(arrayOf("image/*"))
        },
        onCameraClick = {
          val uri = onPrepareCameraCaptureTarget()
          if (uri != null) {
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
          }
        },
        onDemoClick = viewModel::onLoadDemoSample,
      )

      // 3. Platform Preset Selector
      PlatformPresetSection(
        selectedPreset = uiState.params.preset,
        onSelectPreset = viewModel::onSelectPreset,
      )

      // 4. Charset Mode Selector
      CharsetModeSection(
        selectedCharset = uiState.params.charsetMode,
        onSelectCharset = viewModel::onSelectCharsetMode,
      )

      // 5. Opciones de Entrega y Enlace HD
      DeliveryOptionsSection(
        preset = uiState.params.preset,
        includeAsciiArt = uiState.includeAsciiArt,
        attachDeepLink = uiState.attachDeepLink,
        onToggleIncludeAsciiArt = viewModel::onToggleIncludeAsciiArt,
        onToggleAttachDeepLink = viewModel::onToggleAttachDeepLink,
      )

      // 6. Chat Bubble Preview Card
      ChatBubblePreviewSection(
        uiState = uiState,
        onToggleTheme = viewModel::onTogglePreviewBubbleTheme,
      )

      // 7. Photographic & Dither Adjustments
      AdjustmentsSection(
        params = uiState.params,
        onWidthChanged = viewModel::onWidthColumnsChanged,
        onBrightnessChanged = viewModel::onBrightnessChanged,
        onContrastChanged = viewModel::onContrastChanged,
        onInvertToggled = viewModel::onInvertToggled,
        onDitherSelected = viewModel::onSelectDitherMode,
        onReset = viewModel::onResetAdjustments,
      )

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImageSourceSection(
  sourceOrigin: ImageSourceOrigin,
  sourceLabel: String,
  dimensionsLabel: String,
  isLoading: Boolean,
  onGalleryClick: () -> Unit,
  onDocumentClick: () -> Unit,
  onCameraClick: () -> Unit,
  onDemoClick: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.source_heading),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
      )
      if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
      } else {
        val labelText = if (dimensionsLabel.isNotBlank()) {
          "$sourceLabel · $dimensionsLabel"
        } else {
          sourceLabel
        }
        Text(
          text = labelText,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FilterChip(
        selected = sourceOrigin == ImageSourceOrigin.GALLERY,
        onClick = onGalleryClick,
        label = { Text(stringResource(R.string.source_gallery)) },
        leadingIcon = { Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(18.dp)) },
        modifier = Modifier.testTag(ConverterTestTags.BTN_GALLERY).semantics {
          contentDescription = "Abrir Galería de imágenes"
        },
      )
      FilterChip(
        selected = sourceOrigin == ImageSourceOrigin.DOCUMENTS_OR_CLOUD,
        onClick = onDocumentClick,
        label = { Text(stringResource(R.string.source_documents)) },
        leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp)) },
        modifier = Modifier.testTag(ConverterTestTags.BTN_DOCUMENTS).semantics {
          contentDescription = "Abrir Google Drive o Archivos locales"
        },
      )
      FilterChip(
        selected = sourceOrigin == ImageSourceOrigin.CAMERA,
        onClick = onCameraClick,
        label = { Text(stringResource(R.string.source_camera)) },
        leadingIcon = { Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp)) },
        modifier = Modifier.testTag(ConverterTestTags.BTN_CAMERA).semantics {
          contentDescription = "Tomar fotografía con la cámara"
        },
      )
      FilterChip(
        selected = sourceOrigin == ImageSourceOrigin.DEMO,
        onClick = onDemoClick,
        label = { Text(stringResource(R.string.source_demo)) },
        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
        modifier = Modifier.testTag(ConverterTestTags.BTN_DEMO).semantics {
          contentDescription = "Cargar imagen de muestra a bordo"
        },
      )
    }
  }
}

@Composable
private fun PlatformPresetSection(
  selectedPreset: ImPlatformPreset,
  onSelectPreset: (ImPlatformPreset) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = stringResource(R.string.platform_heading),
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.semantics { heading() },
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ImPlatformPreset.entries.forEach { preset ->
        FilterChip(
          selected = selectedPreset == preset,
          onClick = { onSelectPreset(preset) },
          label = { Text(preset.displayName) },
          modifier = Modifier.weight(1f).semantics {
            contentDescription = "Seleccionar preset ${preset.displayName}"
          },
        )
      }
    }
  }
}

@Composable
private fun CharsetModeSection(
  selectedCharset: CharsetMode,
  onSelectCharset: (CharsetMode) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = stringResource(R.string.charset_heading),
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.semantics { heading() },
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      CharsetMode.entries.forEach { mode ->
        FilterChip(
          selected = selectedCharset == mode,
          onClick = { onSelectCharset(mode) },
          label = { Text(mode.displayName) },
          modifier = Modifier.weight(1f).semantics {
            contentDescription = "Modo de texto ${mode.displayName}"
          },
        )
      }
    }
  }
}

@Composable
private fun ChatBubblePreviewSection(
  uiState: ConverterUiState,
  onToggleTheme: () -> Unit,
) {
  val result = uiState.conversionResult
  val bubbleToggleDescription = stringResource(R.string.bubble_theme_toggle_desc)

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.bubble_preview_heading),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (result?.wasAutoScaled == true) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
          ) {
            Text(
              text = stringResource(R.string.autoscale_badge),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
        }
        IconButton(
          onClick = onToggleTheme,
          modifier = Modifier.size(32.dp).semantics {
            contentDescription = bubbleToggleDescription
          },
        ) {
          Icon(
            imageVector = if (uiState.previewDarkBubble) Icons.Default.LightMode else Icons.Default.DarkMode,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }

    // Instagram proportional warning
    if (result?.proportionalFontWarning == true) {
      Card(
        modifier = Modifier.fillMaxWidth().testTag(ConverterTestTags.PROPORTIONAL_WARNING),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = stringResource(R.string.warning_instagram_proportional),
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
    }

    // Mobile width overflow warning (> 32 columns)
    if (uiState.params.widthColumns > 32) {
      Card(
        modifier = Modifier.fillMaxWidth().testTag(ConverterTestTags.WIDTH_OVERFLOW_WARNING),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
          contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "⚠️ Ancho de ${uiState.params.widthColumns} cols excede el ancho de pantalla móvil (~32 cols). En WhatsApp y Telegram las líneas se cortarán y la imagen se verá rota. Recomendado: 28–32 columnas.",
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
    }

    // Chat Bubble Container
    val bubbleBgColor = if (uiState.previewDarkBubble) {
      Color(0xFF131F2E) // Authentic dark chat bubble
    } else {
      Color(0xFFE7FFDB) // Authentic WhatsApp-like light green bubble
    }
    val bubbleTextColor = if (uiState.previewDarkBubble) {
      Color(0xFFE3E8EC)
    } else {
      Color(0xFF111B21)
    }

    ElevatedCard(
      modifier = Modifier.fillMaxWidth().testTag(ConverterTestTags.CHAT_BUBBLE_CARD),
      colors = CardDefaults.elevatedCardColors(
        containerColor = bubbleBgColor,
        contentColor = bubbleTextColor,
      ),
      shape = RoundedCornerShape(16.dp),
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        // Bubble header metrics
        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          val charText = if (result != null) {
            "${result.charCount} / ${result.maxCharBudget} chars"
          } else {
            "— / —"
          }
          val dimsText = if (result != null) {
            "${result.columns} cols × ${result.rows} filas"
          } else {
            ""
          }
          Text(
            text = charText,
            style = MaterialTheme.typography.labelSmall,
            color = bubbleTextColor.copy(alpha = 0.7f),
          )
          Text(
            text = dimsText,
            style = MaterialTheme.typography.labelSmall,
            color = bubbleTextColor.copy(alpha = 0.7f),
          )
        }

        // Monospace text art with horizontal scrolling
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        ) {
          SelectionContainer {
            val previewContent = if (!uiState.includeAsciiArt) {
              "📸 [Foto en Alta Resolución]\n\nEl arte ASCII/Braille ha sido omitido del mensaje para destinar todos los bytes del canal a la fotografía original en alta definición.\n\n🔗 Enlace generado (#data=...):\nhttps://laralbir.github.io/img2asciiIM/#data=..."
            } else if (uiState.attachDeepLink) {
              "${result?.formattedMessage.orEmpty()}\n\n🔗 Ver foto en alta definición:\nhttps://laralbir.github.io/img2asciiIM/#data=..."
            } else {
              result?.formattedMessage.orEmpty()
            }

            Text(
              text = previewContent,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp,
              lineHeight = 10.sp,
              color = bubbleTextColor,
              modifier = Modifier
                .testTag(ConverterTestTags.ASCII_TEXT)
                .clearAndSetSemantics {
                  contentDescription = if (!uiState.includeAsciiArt) {
                    "Modo solo enlace de alta resolución"
                  } else if (result != null) {
                    "Arte de texto con ${result.columns} columnas y ${result.rows} filas, longitud ${result.charCount} caracteres."
                  } else {
                    "Sin arte generado"
                  }
                },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AdjustmentsSection(
  params: com.laralbir.img2asciiim.domain.model.ConversionParams,
  onWidthChanged: (Int) -> Unit,
  onBrightnessChanged: (Float) -> Unit,
  onContrastChanged: (Float) -> Unit,
  onInvertToggled: (Boolean) -> Unit,
  onDitherSelected: (DitherMode) -> Unit,
  onReset: () -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }

  OutlinedCard(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(20.dp))
          Text(
            text = stringResource(R.string.adjustments_heading),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { heading() },
          )
        }
        IconButton(
          onClick = { expanded = !expanded },
          modifier = Modifier.semantics {
            contentDescription = if (expanded) "Ocultar ajustes" else "Mostrar ajustes"
          },
        ) {
          Icon(
            imageVector = if (expanded) Icons.Default.Close else Icons.Default.Tune,
            contentDescription = null,
          )
        }
      }

      if (expanded) {
        // Slider Width Columns
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.slider_columns, params.widthColumns),
              style = MaterialTheme.typography.bodySmall,
            )
            if (params.widthColumns > 32) {
              Text(
                text = "⚠️ > 32 cols cortará líneas",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
              )
            }
          }
          Slider(
            value = params.widthColumns.toFloat(),
            onValueChange = { onWidthChanged(it.toInt()) },
            valueRange = 8f..72f,
            steps = 31,
            modifier = Modifier.testTag(ConverterTestTags.SLIDER_WIDTH),
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            listOf(24, 28, 30, 32).forEach { cols ->
              FilterChip(
                selected = params.widthColumns == cols,
                onClick = { onWidthChanged(cols) },
                label = {
                  Text(
                    text = if (cols == 30) "$cols (Óptimo)" else "$cols cols",
                    style = MaterialTheme.typography.labelSmall,
                  )
                },
                modifier = Modifier.weight(1f),
              )
            }
          }
        }

        // Slider Brightness
        Column {
          Text(
            text = stringResource(R.string.slider_brightness, params.brightness),
            style = MaterialTheme.typography.bodySmall,
          )
          Slider(
            value = params.brightness,
            onValueChange = onBrightnessChanged,
            valueRange = -1f..1f,
            modifier = Modifier.testTag(ConverterTestTags.SLIDER_BRIGHTNESS),
          )
        }

        // Slider Contrast
        Column {
          Text(
            text = stringResource(R.string.slider_contrast, params.contrast),
            style = MaterialTheme.typography.bodySmall,
          )
          Slider(
            value = params.contrast,
            onValueChange = onContrastChanged,
            valueRange = 0.5f..3.0f,
            modifier = Modifier.testTag(ConverterTestTags.SLIDER_CONTRAST),
          )
        }

        // Invert switch
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(R.string.toggle_invert),
            style = MaterialTheme.typography.bodyMedium,
          )
          Switch(
            checked = params.invert,
            onCheckedChange = onInvertToggled,
            modifier = Modifier.testTag(ConverterTestTags.TOGGLE_INVERT),
          )
        }

        // Dithering chips
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = stringResource(R.string.dither_heading),
            style = MaterialTheme.typography.labelMedium,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            DitherMode.entries.forEach { mode ->
              FilterChip(
                selected = params.ditherMode == mode,
                onClick = { onDitherSelected(mode) },
                label = { Text(mode.displayName, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f),
              )
            }
          }
        }

        // Reset button
        OutlinedButton(
          onClick = onReset,
          modifier = Modifier.align(Alignment.End),
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(stringResource(R.string.btn_reset_adjustments))
        }
      }
    }
  }
}

@Composable
private fun BottomActionControls(
  preset: ImPlatformPreset,
  isConverting: Boolean,
  onCopy: () -> Unit,
  onShare: () -> Unit,
) {
  Surface(
    shadowElevation = 8.dp,
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      OutlinedButton(
        onClick = onCopy,
        modifier = Modifier.weight(1f).testTag(ConverterTestTags.BTN_COPY).semantics {
          contentDescription = "Copiar arte de texto al portapapeles"
        },
      ) {
        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.btn_copy))
      }

      Button(
        onClick = onShare,
        modifier = Modifier.weight(1.5f).testTag(ConverterTestTags.BTN_SHARE).semantics {
          contentDescription = "Enviar mensaje formateado a ${preset.displayName}"
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
      ) {
        if (isConverting) {
          CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
        } else {
          Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.btn_share, preset.displayName))
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeliveryOptionsSection(
  preset: ImPlatformPreset,
  includeAsciiArt: Boolean,
  attachDeepLink: Boolean,
  onToggleIncludeAsciiArt: (Boolean) -> Unit,
  onToggleAttachDeepLink: (Boolean) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = "Opciones de Entrega y Enlace HD",
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.semantics { heading() },
    )

    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FilterChip(
        selected = includeAsciiArt,
        onClick = { onToggleIncludeAsciiArt(!includeAsciiArt) },
        label = { Text("Arte de texto (ASCII / Braille)") },
        leadingIcon = {
          if (includeAsciiArt) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
          }
        },
        modifier = Modifier.testTag(ConverterTestTags.CHIP_INCLUDE_ASCII),
      )

      FilterChip(
        selected = attachDeepLink,
        onClick = { onToggleAttachDeepLink(!attachDeepLink) },
        label = { Text("Enlace a foto HD (#data=...)") },
        leadingIcon = {
          Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
        },
        enabled = preset != ImPlatformPreset.INSTAGRAM_DIRECT,
        modifier = Modifier.testTag(ConverterTestTags.CHIP_ATTACH_DEEP_LINK),
      )
    }

    if (!includeAsciiArt && attachDeepLink) {
      Card(
        modifier = Modifier.fillMaxWidth().testTag(ConverterTestTags.HD_MODE_EXPANDED_CARD),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
          )
          Text(
            text = "Modo Enlace HD Activo: Al omitir el arte ASCII, se reservan todos los bytes del mensaje para la foto original (hasta 720 px a calidad 75 en WhatsApp).",
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
    }
  }
}
