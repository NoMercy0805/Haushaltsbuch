package com.schiwy.haushaltsbuch

import org.json.JSONArray
import org.json.JSONObject

internal fun parseHouseholdBackup(text:String):JSONObject {
    val trimmed=text.trim()
    val required=listOf("income","fixedExpenses","openInvoices","variableCosts","variableContributions","variablePayments","loans","loanPayments","savings","cars","fuelLogs","betaEntries","betaHiddenTitles")
    val result=if(trimmed.startsWith("{"))JSONObject(trimmed) else {
        require(trimmed.contains("# TABLE:")){"Das ist keine gültige Sicherung."}
        val database=JSONObject()
        required.forEach{database.put(it,JSONArray())}
        database.put("potCarryover",0.0)
        var table:String?=null
        var columns:List<String>?=null
        backupCsvRows(trimmed).forEach{row->
            if(row.isEmpty()||row.all{it.isBlank()})return@forEach
            val first=row.first().trim()
            if(first.startsWith("# TABLE:")){table=first.removePrefix("# TABLE:").trim();columns=null;return@forEach}
            if(columns==null){columns=row.map{it.trim()};return@forEach}
            val values=columns!!.mapIndexed{index,key->key to row.getOrElse(index){""}}.toMap()
            when(table){
                "settings"->database.put("potCarryover",values["amount"]?.toDoubleOrNull()?:0.0)
                "appSettings"->{
                    values["tabVisibilityJson"]?.takeIf{it.isNotBlank()}?.let{runCatching{JSONObject(it)}.getOrNull()?.let{visibility->database.put("tabVisibility",visibility)}}
                    values["tilgungExtra"]?.toDoubleOrNull()?.takeIf{it>=0}?.let{database.put("tilgungExtra",it)}
                    values["tilgungCurrentMonth"]?.takeIf{it.isNotBlank()}?.let{database.put("tilgungCurrentMonth",it)}
                }
                else->{
                    val array=database.optJSONArray(table.orEmpty())?:return@forEach
                    val item=JSONObject()
                    values.forEach{(key,value)->
                        val parsed:Any=when(key){
                            "amount","monthlyAmount","actualAmount","originalAmount","remainingBalance","monthlyPayment","liters","pricePerLiter"->value.toDoubleOrNull()?:0.0
                            "totalWithInterest","monthlyRate"->value.toDoubleOrNull()?.takeIf{it>0}?:JSONObject.NULL
                            "lastReconciledBalance"->value.toDoubleOrNull()?:JSONObject.NULL
                            "dueMonth"->value.toIntOrNull()?.takeIf{it!=0}?:1
                            "dueYear","totalTermMonths"->value.toIntOrNull()?.takeIf{it>0}?:JSONObject.NULL
                            "carryoverPct"->value.toIntOrNull()?.takeIf{it in 0..100}?:100
                            "paid","recurring","reconciled","isRecurringTemplate"->value=="true"||value=="1"
                            else->value
                        }
                        item.put(key,parsed)
                    }
                    if(table=="betaEntries"){
                        if(!item.optBoolean("isRecurringTemplate"))item.remove("isRecurringTemplate")
                        if(item.optString("recurringGroupId").isBlank())item.remove("recurringGroupId")
                        if(item.optString("recurrenceEnd").isBlank())item.put("recurrenceEnd",JSONObject.NULL)
                    }
                    if(table=="loanPayments"&&item.optString("type").isBlank())item.put("type","payment")
                    array.put(item)
                }
            }
        }
        database
    }
    required.forEach{if(result.optJSONArray(it)==null)result.put(it,JSONArray())}
    if(result.opt("potCarryover") !is Number)result.put("potCarryover",0.0)
    result.getJSONArray("variableCosts").objects().forEach{item->
        if(item.optString("interval").isBlank())item.put("interval","jaehrlich")
        if(item.optInt("dueMonth",0)==0)item.put("dueMonth",1)
    }
    result.getJSONArray("savings").objects().forEach{if(it.optString("type")!="expense")it.put("type","deposit")}
    result.getJSONArray("betaEntries").objects().forEach{entry->when(entry.optString("type")){"income"->entry.put("type","einnahme");"expense"->entry.put("type","ausgabe")}}
    return result
}

internal fun backupCsvRows(text:String):List<List<String>> {
    val rows=ArrayList<List<String>>()
    var row=ArrayList<String>()
    val field=StringBuilder()
    var quoted=false
    var index=0
    while(index<text.length){
        val character=text[index]
        if(quoted){
            if(character=='"'&&index+1<text.length&&text[index+1]=='"'){field.append('"');index++}
            else if(character=='"')quoted=false else field.append(character)
        }else when(character){
            '"'->quoted=true
            ','->{row.add(field.toString());field.clear()}
            '\n'->{row.add(field.toString());field.clear();rows.add(row);row=ArrayList()}
            '\r'->{}
            else->field.append(character)
        }
        index++
    }
    if(field.isNotEmpty()||row.isNotEmpty()){row.add(field.toString());rows.add(row)}
    return rows
}
