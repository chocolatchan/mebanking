package com.mebanking.app.ui.navigation

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.mebanking.app.domain.model.navigation.BranchDistance
import com.mebanking.app.domain.model.navigation.GeoPoint

/**
 * Interactive HTML5 / Leaflet map rendered inside an Android WebView.
 * Provides real street map tiles, pre-marked banks, distance polyline,
 * user beacon pin, and seamless JavaScript-to-Kotlin callbacks.
 * Works 100% offline and online without requiring Google Play Services or restricted API keys.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InteractiveMapView(
    userLocation: GeoPoint,
    branches: List<BranchDistance>,
    selectedBranch: BranchDistance?,
    onBranchClick: (BranchDistance) -> Unit,
    onMapUnavailable: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val webView = remember(context) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.evaluateJavascript(
                        "(function(){ if (typeof L === 'undefined' && window.AndroidBridge && window.AndroidBridge.onLeafletUnavailable) { window.AndroidBridge.onLeafletUnavailable(); } })();",
                        null,
                    )
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: android.webkit.WebResourceRequest?,
                    error: android.webkit.WebResourceError?,
                ) {
                    if (request?.isForMainFrame == true) {
                        onMapUnavailable()
                    }
                }
            }
        }
    }

    val jsInterface = remember(branches, onBranchClick, onMapUnavailable) {
        object {
            @JavascriptInterface
            fun onMarkerClicked(branchId: Long) {
                val matched = branches.firstOrNull { it.branch.id == branchId }
                if (matched != null) {
                    webView.post {
                        onBranchClick(matched)
                    }
                }
            }

            @JavascriptInterface
            fun onLeafletUnavailable() {
                webView.post {
                    onMapUnavailable()
                }
            }
        }
    }

    DisposableEffect(webView) {
        webView.addJavascriptInterface(jsInterface, "AndroidBridge")
        onDispose {
            webView.removeJavascriptInterface("AndroidBridge")
            webView.destroy()
        }
    }

    // Load initial HTML when userLocation or branches change
    LaunchedEffect(userLocation, branches) {
        val html = buildMapHtml(userLocation, branches, selectedBranch)
        webView.loadDataWithBaseURL("https://appassets.androidplatform.net", html, "text/html", "UTF-8", null)
    }

    // Update selected branch & polyline dynamically in JavaScript
    LaunchedEffect(selectedBranch) {
        if (selectedBranch != null) {
            val b = selectedBranch.branch
            val js = "if (window.selectBranch) { window.selectBranch(${b.latitude}, ${b.longitude}, ${b.id}); }"
            webView.evaluateJavascript(js, null)
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize(),
    )
}

private fun buildMapHtml(
    userLocation: GeoPoint,
    branches: List<BranchDistance>,
    selectedBranch: BranchDistance?,
): String {
    val markersJson = StringBuilder("[")
    branches.forEachIndexed { i, bDist ->
        val b = bDist.branch
        val isAtm = b.name.contains("ATM", ignoreCase = true) || b.type.equals("ATM", ignoreCase = true)
        val distText = if (bDist.directDistanceMeters < 1000) {
            "${bDist.directDistanceMeters.toInt()}m"
        } else {
            String.format(java.util.Locale.US, "%.1f km", bDist.directDistanceMeters / 1000.0)
        }
        val safeName = b.name.replace("'", "\\'").replace("\"", "\\\"")
        val safeAddress = b.address.replace("'", "\\'").replace("\"", "\\\"")

        markersJson.append("""
            {
                "id": ${b.id},
                "lat": ${b.latitude},
                "lng": ${b.longitude},
                "name": "$safeName",
                "address": "$safeAddress",
                "dist": "$distText",
                "isAtm": $isAtm
            }
        """)
        if (i < branches.size - 1) markersJson.append(",")
    }
    markersJson.append("]")

    val selectedId = selectedBranch?.branch?.id ?: -1L
    val selLat = selectedBranch?.branch?.latitude ?: userLocation.latitude
    val selLng = selectedBranch?.branch?.longitude ?: userLocation.longitude

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; background: #e5e3df; }
                .user-pin {
                    width: 18px; height: 18px; border-radius: 50%;
                    background: #1A73E8; border: 3px solid #FFFFFF;
                    box-shadow: 0 0 12px rgba(26,115,232,0.8);
                }
                .bank-pin {
                    display: flex; align-items: center; justify-content: center;
                    width: 32px; height: 32px; border-radius: 8px;
                    color: white; font-weight: bold; font-size: 11px;
                    border: 2px solid white; box-shadow: 0 2px 6px rgba(0,0,0,0.3);
                    cursor: pointer; font-family: sans-serif;
                }
                .bank-vcb { background: #7C5CBF; }
                .bank-tcb { background: #DC2626; }
                .bank-bidv { background: #0284C7; }
                .bank-general { background: #4F46E5; }
                .bank-atm { background: #0D9488; }
                .leaflet-popup-content-wrapper { border-radius: 12px; font-family: sans-serif; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var userLat = ${userLocation.latitude};
                var userLng = ${userLocation.longitude};
                var selectedId = $selectedId;

                var map = L.map('map', {
                    zoomControl: false,
                    attributionControl: false
                }).setView([userLat, userLng], 15);

                // Standard OpenStreetMap street tiles with fast fallback
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19,
                    subdomains: ['a', 'b', 'c']
                }).addTo(map);

                // User Location Marker
                var userIcon = L.divIcon({ className: 'user-pin', iconSize: [18, 18], iconAnchor: [9, 9] });
                L.marker([userLat, userLng], { icon: userIcon }).addTo(map).bindPopup("<b>Vị trí của bạn</b><br/>Điểm bắt đầu");

                var branches = $markersJson;
                var markers = {};
                var polyline = null;

                function getPinClass(name, isAtm) {
                    if (isAtm) return 'bank-atm';
                    var lower = name.toLowerCase();
                    if (lower.indexOf('vietcombank') >= 0 || lower.indexOf('vcb') >= 0) return 'bank-vcb';
                    if (lower.indexOf('techcombank') >= 0 || lower.indexOf('tcb') >= 0) return 'bank-tcb';
                    if (lower.indexOf('bidv') >= 0) return 'bank-bidv';
                    return 'bank-general';
                }

                function getShortLabel(name, isAtm) {
                    if (isAtm) return 'ATM';
                    var lower = name.toLowerCase();
                    if (lower.indexOf('vietcombank') >= 0 || lower.indexOf('vcb') >= 0) return 'VCB';
                    if (lower.indexOf('techcombank') >= 0 || lower.indexOf('tcb') >= 0) return 'TCB';
                    if (lower.indexOf('bidv') >= 0) return 'BIDV';
                    if (lower.indexOf('agribank') >= 0) return 'AGR';
                    if (lower.indexOf('mb') >= 0) return 'MB';
                    if (lower.indexOf('acb') >= 0) return 'ACB';
                    return 'NGH';
                }

                branches.forEach(function(b) {
                    var pinCls = getPinClass(b.name, b.isAtm);
                    var label = getShortLabel(b.name, b.isAtm);
                    var icon = L.divIcon({
                        className: '',
                        html: '<div class="bank-pin ' + pinCls + '">' + label + '</div>',
                        iconSize: [32, 32],
                        iconAnchor: [16, 16]
                    });

                    var marker = L.marker([b.lat, b.lng], { icon: icon }).addTo(map);
                    marker.bindPopup('<b>' + b.name + '</b><br/>' + b.address + '<br/><span style="color:#7C5CBF;font-weight:bold;">Khoảng cách: ' + b.dist + '</span>');
                    marker.on('click', function() {
                        selectBranch(b.lat, b.lng, b.id);
                        if (window.AndroidBridge && window.AndroidBridge.onMarkerClicked) {
                            window.AndroidBridge.onMarkerClicked(b.id);
                        }
                    });
                    markers[b.id] = marker;
                });

                window.selectBranch = function(lat, lng, id) {
                    if (polyline) {
                        map.removeLayer(polyline);
                    }
                    // Draw direct polyline from user to selected bank
                    polyline = L.polyline([[userLat, userLng], [lat, lng]], {
                        color: '#7C5CBF',
                        weight: 4,
                        opacity: 0.85,
                        dashArray: '8, 8'
                    }).addTo(map);

                    map.setView([lat, lng], 15.5);
                    if (markers[id]) {
                        markers[id].openPopup();
                    }
                };

                // Initial selected branch line if any
                if (selectedId > 0) {
                    window.selectBranch($selLat, $selLng, selectedId);
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
