package com.wpf.util.common.ui.marketplace.markets.base

import androidx.compose.runtime.MutableState
import com.wpf.server.FileServer
import com.wpf.server.serverBasePath
import javafx.scene.web.WebView
import java.io.File

interface BrowserMarket : Market {

    override val baseUrl: String
        get() = ""

    val browserUrl: String

    val showBrowserS: MutableState<Boolean>

    val canPush: Boolean

    fun onWebUrlChange(url: String?, webView: WebView) {

    }

    fun WebView.querySelector(selector: String, attribute: String): String? {
        return this.engine.executeScript("document.querySelector('a[class=\\'$selector\\']')?.$attribute")?.toString()
    }

    fun WebView.setElementValue(name: String, value: String) {
        if (findElements(name)) {
            this.engine.executeScript("document.getElementsByName('${name}').item(0).value = '${value}'")
        }
    }

    fun WebView.setElementCheck(name: String, value: Boolean) {
        if (findElements(name)) {
            this.engine.executeScript("document.getElementsByName('${name}').item(0).checked = '${value}'")
        }
    }

    fun WebView.findElements(name: String): Boolean {
        val returnObj = this.engine.executeScript("document.getElementsByName('${name}')?.length")
        return returnObj != "undefined" && (returnObj != 0 || returnObj != "0")
    }

    fun WebView.inputFile(name: String, pos: Int = 0, filePath: String, fileName: String = "") {
        if (findElements(name)) {
            println("准备上传：$filePath")
            this.engine.executeScript("var fileInput = document.getElementsByName(\"$name\").item($pos);const path = '$filePath';fetch(path).then(response => response.blob()).then(blob => {const file = new File([blob], '$fileName');var changeEvent = new Event(\"change\");Object.defineProperty(fileInput, 'files', { value: [file] });fileInput.dispatchEvent(changeEvent);});")
        }
    }

    fun WebView.buttonSubmit(name: String) {
        this.engine.executeScript("\$('.$name').submit();")
    }

    fun getFileLocalUrl(filePath: String): String {
        return FileServer.baseUrl + "?filePath=$serverBasePath${File.separator}$filePath"
    }
}