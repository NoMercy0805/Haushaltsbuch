package com.schiwy.haushaltsbuch;

/**
 * Eigenstaendiger Empfaenger nur fuer die Aufdecken/Wiederverstecken-Broadcasts
 * des Kontostand-Widgets (ACTION_REVEAL / ACTION_REHIDE). Nutzt dieselbe
 * onReceive-Logik wie die groessen-spezifischen Provider (geerbt von
 * BetaWidgetProvider), ist aber keiner bestimmten Widget-Groesse zugeordnet -
 * getLayoutId() wird hier nie aufgerufen, da onUpdate fuer diesen Empfaenger
 * nicht vom System getriggert wird (kein appwidget-provider Meta-Data).
 */
public class HaushaltsbuchBetaWidgetReceiver extends BetaWidgetProvider {
    @Override
    protected int getLayoutId() {
        return R.layout.beta_widget_4x1;
    }
}
