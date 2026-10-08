package com.schiwy.haushaltsbuch;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

/** Native App-Sperre und Einstellungen-Bruecke fuer die WebView. */
public class SecurityBridge {

    private static final String PREFS = "security_prefs";
    private static final String KEY_BIOMETRIC_LOCK = "biometric_lock_enabled";

    private final FragmentActivity activity;
    private final WebView webView;
    private final SharedPreferences prefs;
    private final Executor executor;

    private boolean firstStart = true;
    private boolean lockOnNextStart = false;
    private boolean authenticating = false;
    private boolean externalActivityInProgress = false;

    private static final String SETTINGS_JS = """
        (function(){
          if(window.__nativeSecuritySettingsInstalled) return;
          var menu = document.querySelector('#settingsOverlay .settings-nav-list');
          if(!menu || !window.SecurityBridge) return;
          window.__nativeSecuritySettingsInstalled = true;

          var row = document.createElement('button');
          row.type = 'button';
          row.className = 'settings-nav-row';
          row.setAttribute('data-native-security-settings', 'true');
          row.innerHTML = '<span class="settings-nav-icon"><svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3l7 3v5c0 4.6-2.8 8.1-7 10-4.2-1.9-7-5.4-7-10V6l7-3z"/><path d="M9.5 12.3l1.7 1.7 3.6-4"/></svg></span>' +
            '<span class="settings-nav-text"><span class="settings-nav-title">Sicherheit</span><span class="settings-nav-sub">Fingerabdruck beim Öffnen der App</span></span>' +
            '<span class="settings-nav-arrow">›</span>';
          var resetRow = menu.querySelector('[data-open-settings="reset"]');
          if(resetRow) menu.insertBefore(row, resetRow); else menu.appendChild(row);

          var overlay = document.createElement('div');
          overlay.id = 'settingsSubOverlay-sicherheit';
          overlay.className = 'settings-overlay';
          overlay.innerHTML = '<div class="settings-panel">' +
            '<div class="settings-panel-head">' +
              '<button class="settings-back-btn" data-security-back aria-label="Zurück"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6"/></svg></button>' +
              '<h2>Sicherheit</h2><span style="width:34px;"></span>' +
            '</div>' +
            '<p class="chooser-group-label">App-Sperre</p>' +
            '<div class="summary-card">' +
              '<div class="tab-visibility-row"><span>Mit Fingerabdruck entsperren</span>' +
                '<label class="toggle-switch"><input type="checkbox" id="biometricLockToggle"><span class="track"></span><span class="thumb"></span></label>' +
              '</div>' +
              '<p id="biometricLockHint" style="margin:8px 0 0;font-size:13px;color:var(--ink-soft);">Schützt Haushaltsbuch beim Öffnen und nach der Rückkehr aus dem Hintergrund.</p>' +
              '<p id="biometricLockStatus" style="margin:8px 0 0;font-size:12px;color:var(--ink-soft);"></p>' +
            '</div>' +
          '</div>';
          document.body.appendChild(overlay);

          row.addEventListener('click', function(){
            document.getElementById('settingsOverlay').classList.remove('open');
            overlay.classList.add('open');
          });
          overlay.querySelector('[data-security-back]').addEventListener('click', function(){
            overlay.classList.remove('open');
            document.getElementById('settingsOverlay').classList.add('open');
          });

          var toggle = document.getElementById('biometricLockToggle');
          var hint = document.getElementById('biometricLockHint');
          var status = document.getElementById('biometricLockStatus');

          function refreshSecurity(){
            var enabled = false, available = false;
            try{ enabled = !!window.SecurityBridge.isBiometricLockEnabled(); }catch(e){}
            try{ available = !!window.SecurityBridge.isBiometricAvailable(); }catch(e){}
            toggle.checked = enabled;
            toggle.disabled = !available && !enabled;
            if(!available && !enabled){
              hint.textContent = 'Auf diesem Gerät ist derzeit kein Fingerabdruck bzw. keine Biometrie eingerichtet.';
            }else{
              hint.textContent = 'Schützt Haushaltsbuch beim Öffnen und nach der Rückkehr aus dem Hintergrund. Beim späteren Entsperren kann Android zusätzlich die Gerätesperre als Rückfall anbieten.';
            }
          }

          window.onBiometricLockChanged = function(enabled, message){
            toggle.checked = !!enabled;
            toggle.disabled = false;
            refreshSecurity();
            status.textContent = message || (enabled ? 'App-Sperre ist aktiviert.' : 'App-Sperre ist deaktiviert.');
          };

          toggle.addEventListener('change', function(){
            var target = !!toggle.checked;
            toggle.disabled = true;
            status.textContent = target ? 'Fingerabdruck bestätigen …' : 'App-Sperre wird deaktiviert …';
            try{
              window.SecurityBridge.setBiometricLockEnabled(target);
            }catch(e){
              toggle.checked = !target;
              toggle.disabled = false;
              status.textContent = 'Einstellung konnte nicht geändert werden.';
            }
          });

          refreshSecurity();
        })();
        """;

    public SecurityBridge(FragmentActivity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
        this.prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.executor = ContextCompat.getMainExecutor(activity);
        if (isBiometricLockEnabled()) {
            webView.setVisibility(View.INVISIBLE);
        }
    }

    public void installSettingsUi() {
        webView.evaluateJavascript(SETTINGS_JS, null);
    }

    @JavascriptInterface
    public boolean isBiometricLockEnabled() {
        return prefs.getBoolean(KEY_BIOMETRIC_LOCK, false);
    }

    @JavascriptInterface
    public boolean isBiometricAvailable() {
        return BiometricManager.from(activity).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    @JavascriptInterface
    public void setBiometricLockEnabled(final boolean enabled) {
        activity.runOnUiThread(() -> {
            if (!enabled) {
                prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK, false).apply();
                webView.setVisibility(View.VISIBLE);
                notifyWeb(false, "App-Sperre ist deaktiviert.");
                return;
            }

            if (!isBiometricAvailable()) {
                notifyWeb(false, "Kein Fingerabdruck bzw. keine Biometrie eingerichtet.");
                return;
            }

            authenticate(
                    createEnablePrompt(),
                    () -> {
                        prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK, true).apply();
                        webView.setVisibility(View.VISIBLE);
                        notifyWeb(true, "App-Sperre ist aktiviert.");
                    },
                    () -> notifyWeb(false, "Aktivierung wurde abgebrochen.")
            );
        });
    }

    public void onHostStart() {
        if (!isBiometricLockEnabled()) {
            firstStart = false;
            lockOnNextStart = false;
            webView.setVisibility(View.VISIBLE);
            return;
        }

        if (firstStart || lockOnNextStart) {
            firstStart = false;
            lockOnNextStart = false;
            webView.setVisibility(View.INVISIBLE);
            authenticate(
                    createUnlockPrompt(),
                    () -> webView.setVisibility(View.VISIBLE),
                    () -> activity.finish()
            );
        } else {
            firstStart = false;
        }
    }

    public void onHostStop() {
        if (isBiometricLockEnabled() && !externalActivityInProgress && !authenticating) {
            lockOnNextStart = true;
            webView.setVisibility(View.INVISIBLE);
        }
    }

    public void setExternalActivityInProgress(boolean inProgress) {
        externalActivityInProgress = inProgress;
    }

    private BiometricPrompt.PromptInfo createEnablePrompt() {
        return new BiometricPrompt.PromptInfo.Builder()
                .setTitle("App-Sperre aktivieren")
                .setSubtitle("Fingerabdruck oder Biometrie bestätigen")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .setNegativeButtonText("Abbrechen")
                .build();
    }

    @SuppressWarnings("deprecation")
    private BiometricPrompt.PromptInfo createUnlockPrompt() {
        BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Haushaltsbuch+ entsperren")
                .setSubtitle("Fingerabdruck oder Gerätesperre verwenden")
                .setConfirmationRequired(false);
        if (Build.VERSION.SDK_INT >= 30) {
            builder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
                            | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        } else {
            builder.setDeviceCredentialAllowed(true);
        }
        return builder.build();
    }

    private void authenticate(BiometricPrompt.PromptInfo promptInfo,
                              Runnable onSuccess,
                              Runnable onCancel) {
        if (authenticating) return;
        authenticating = true;

        BiometricPrompt prompt = new BiometricPrompt(activity, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        authenticating = false;
                        onSuccess.run();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        authenticating = false;
                        if (errString != null && errString.length() > 0
                                && errorCode != BiometricPrompt.ERROR_USER_CANCELED
                                && errorCode != BiometricPrompt.ERROR_CANCELED
                                && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            Toast.makeText(activity, errString, Toast.LENGTH_SHORT).show();
                        }
                        onCancel.run();
                    }
                });
        prompt.authenticate(promptInfo);
    }

    private void notifyWeb(boolean enabled, String message) {
        String safe = message == null ? "" : message.replace("\\", "\\\\").replace("'", "\\'");
        webView.evaluateJavascript(
                "window.onBiometricLockChanged && window.onBiometricLockChanged("
                        + enabled + ", '" + safe + "');", null);
    }
}
