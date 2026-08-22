package com.example.ui.components

import android.annotation.SuppressLint
import android.os.Build
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.artifacts.Artifact
import com.example.data.artifacts.ArtifactType
import com.example.ui.theme.*

/**
 * Sandboxed Interactive Preview Runner.
 * Executes or renders HTML/JS/CSS, Markdown, JSON, SVG in an isolated, memory-safe container.
 * Full compatibility with legacy Android (API 17+) and modern Android releases.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SandboxedInteractivePreview(
    artifact: Artifact,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var reloadTrigger by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.let { wv ->
                wv.stopLoading()
                wv.webChromeClient = null
                wv.clearHistory()
                wv.removeAllViews()
                wv.destroy()
            }
            webViewInstance = null
        }
    }

    when (artifact.type) {
        ArtifactType.HTML, ArtifactType.JAVASCRIPT, ArtifactType.CSS, ArtifactType.SVG -> {
            val htmlContent = remember(artifact.content, reloadTrigger) {
                wrapIntoSandboxedHtml(artifact)
            }

            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 360.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, ZegaNeonCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("sandboxed_webview"),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                defaultTextEncodingName = "utf-8"

                                // Security: Prevent file access from sandboxed preview
                                allowFileAccess = false
                                allowContentAccess = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                    allowFileAccessFromFileURLs = false
                                    allowUniversalAccessFromFileURLs = false
                                }

                                // Legacy optimizations
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    setRenderPriority(WebSettings.RenderPriority.HIGH)
                                }
                                cacheMode = WebSettings.LOAD_NO_CACHE
                            }

                            webViewClient = WebViewClient()
                            webChromeClient = WebChromeClient()

                            loadDataWithBaseURL("https://sandbox.local/", htmlContent, "text/html", "UTF-8", null)
                            webViewInstance = this
                        }
                    },
                    update = { view ->
                        view.loadDataWithBaseURL("https://sandbox.local/", htmlContent, "text/html", "UTF-8", null)
                    }
                )

                // Quick Reload Floating Button
                IconButton(
                    onClick = { reloadTrigger++ },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .testTag("reload_sandbox_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload Sandbox",
                        tint = ZegaNeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        ArtifactType.JSON -> {
            // Interactive Structured JSON Viewer
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0D1117))
                    .border(1.dp, ZegaNeonViolet.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = artifact.content,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = ZegaNeonCyan,
                    lineHeight = 16.sp
                )
            }
        }

        ArtifactType.MARKDOWN, ArtifactType.PYTHON, ArtifactType.KOTLIN, ArtifactType.CODE -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0A0E14))
                    .border(1.dp, ZegaPulseOrange.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = artifact.content,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color(0xFFE6EDF3),
                    lineHeight = 17.sp
                )
            }
        }
    }
}

private const val DEXIE_VFS_SCRIPT = """
<script src="https://unpkg.com/dexie@3.2.4/dist/dexie.min.js"></script>
<script>
  // In-Sandbox Dexie.js IndexedDB Virtual File System Engine
  (function() {
    try {
      const db = new Dexie('ZegaVirtualFileSystem');
      db.version(1).stores({
        nodes: '++id, fullPath, name, parentPath, isDirectory, content, updatedAt'
      });

      window.vfs = {
        db: db,
        createFolder: async function(parentPath, name) {
          const cleanParent = parentPath.endsWith('/') && parentPath.length > 1 ? parentPath.slice(0, -1) : parentPath;
          const fullPath = cleanParent === '/' ? '/' + name : cleanParent + '/' + name;
          await db.nodes.put({
            fullPath: fullPath,
            name: name,
            parentPath: cleanParent,
            isDirectory: 1,
            content: '',
            updatedAt: Date.now()
          });
          return fullPath;
        },
        writeFile: async function(parentPath, name, content) {
          const cleanParent = parentPath.endsWith('/') && parentPath.length > 1 ? parentPath.slice(0, -1) : parentPath;
          const fullPath = cleanParent === '/' ? '/' + name : cleanParent + '/' + name;
          await db.nodes.put({
            fullPath: fullPath,
            name: name,
            parentPath: cleanParent,
            isDirectory: 0,
            content: content,
            updatedAt: Date.now()
          });
          return fullPath;
        },
        readFile: async function(fullPath) {
          const item = await db.nodes.where('fullPath').equals(fullPath).first();
          return item ? item.content : null;
        },
        deleteFile: async function(fullPath) {
          await db.nodes.where('fullPath').equals(fullPath).delete();
          // delete nested children if directory
          await db.nodes.where('parentPath').startsWith(fullPath).delete();
          return true;
        },
        listFiles: async function(parentPath) {
          const cleanParent = parentPath.endsWith('/') && parentPath.length > 1 ? parentPath.slice(0, -1) : parentPath;
          return await db.nodes.where('parentPath').equals(cleanParent).toArray();
        },
        getAll: async function() {
          return await db.nodes.toArray();
        }
      };

      // Seed initial root folders in IndexedDB
      db.on('ready', async function() {
        const count = await db.nodes.count();
        if (count === 0) {
          await window.vfs.createFolder('/', 'projects');
          await window.vfs.createFolder('/', 'artifacts');
          await window.vfs.writeFile('/projects', 'index.html', '<a>Hello World</a>');
        }
      });
    } catch(err) {
      console.warn('Dexie VFS init warning:', err);
    }
  })();
</script>
"""

private fun wrapIntoSandboxedHtml(artifact: Artifact): String {
    return when (artifact.type) {
        ArtifactType.HTML -> {
            if (artifact.content.contains("<html", ignoreCase = true)) {
                if (artifact.content.contains("<head>", ignoreCase = true)) {
                    artifact.content.replaceFirst("<head>", "<head>$DEXIE_VFS_SCRIPT", ignoreCase = true)
                } else {
                    "$DEXIE_VFS_SCRIPT\n${artifact.content}"
                }
            } else {
                """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  $DEXIE_VFS_SCRIPT
                  <style>
                    body {
                      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                      margin: 0;
                      padding: 16px;
                      box-sizing: border-box;
                    }
                  </style>
                </head>
                <body>
                  ${artifact.content}
                </body>
                </html>
                """.trimIndent()
            }
        }
        ArtifactType.JAVASCRIPT -> {
            """
            <!DOCTYPE html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              $DEXIE_VFS_SCRIPT
              <style>
                body {
                  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                  background: #0D1117;
                  color: #00FFCC;
                  padding: 16px;
                  margin: 0;
                }
                #console {
                  background: #161B22;
                  border: 1px solid #30363D;
                  border-radius: 8px;
                  padding: 12px;
                  font-family: monospace;
                  font-size: 13px;
                  color: #58A6FF;
                  min-height: 120px;
                  white-space: pre-wrap;
                }
              </style>
            </head>
            <body>
              <h4 style="margin-top:0; color:#FF007A;">⚡ JavaScript + Dexie.js VFS Sandbox</h4>
              <div id="console"></div>
              <script>
                const consoleDiv = document.getElementById('console');
                const log = (msg) => {
                  consoleDiv.innerText += (typeof msg === 'object' ? JSON.stringify(msg, null, 2) : msg) + '\n';
                };
                console.log = log;
                (async function() {
                  try {
                    ${artifact.content}
                  } catch(e) {
                    log('❌ Error: ' + e.message);
                  }
                })();
              </script>
            </body>
            </html>
            """.trimIndent()
        }
        ArtifactType.SVG -> {
            """
            <!DOCTYPE html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <style>
                body {
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  height: 100vh;
                  margin: 0;
                  background: #0D1117;
                }
                svg {
                  max-width: 90%;
                  max-height: 90%;
                }
              </style>
            </head>
            <body>
              ${artifact.content}
            </body>
            </html>
            """.trimIndent()
        }
        ArtifactType.CSS -> {
            """
            <!DOCTYPE html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <style>
                ${artifact.content}
              </style>
            </head>
            <body>
              <div class="demo-box">
                <h2>CSS Style Sandbox</h2>
                <p>Previewing custom stylesheet classes.</p>
                <button>Styled Button</button>
              </div>
            </body>
            </html>
            """.trimIndent()
        }
        else -> artifact.content
    }
}
