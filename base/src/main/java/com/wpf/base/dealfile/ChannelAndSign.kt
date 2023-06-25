package com.wpf.base.dealfile

import com.android.zipflinger.BytesSource
import com.android.zipflinger.ZipArchive
import com.wpf.base.dealfile.util.*
import java.io.File
import java.lang.Exception
import java.util.concurrent.Callable
import java.util.zip.Deflater
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.system.exitProcess

object ChannelAndSign {
    private val defaultChannelName: String =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(channelBaseInsertFilePath)
            .getElementsByTagName("meta-data").item(0).attributes.item(1).nodeValue

    //原始文件路径
    private var inputFilePath: String = ""

    fun scanFile(
        inputFilePath: String, dealSign: Boolean = true, finish: (() -> Unit)
    ) {
        if (inputFilePath.isEmpty()) {
            println("输入的文件路径不正确")
            finish.invoke()
            return
        }
        this.inputFilePath = inputFilePath
        try {
            dealScanFile(inputFilePath, dealSign) {
                finish.invoke()
                exitProcess(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("运行错误:${e.message}")
            finish.invoke()
            exitProcess(-6457)
        }
    }

    private fun dealScanFile(inputFilePath: String, dealSign: Boolean = true, finish: (() -> Unit)) {
        AXMLEditor2Util.clearCache()
        val dealFile = File(inputFilePath)
        if (dealFile.exists() && dealFile.isFile && "apk" == dealFile.extension) {
            val curPath = dealFile.parent + File.separator
            dealChannel(dealFile) {
                val channelPath = getChannelPath(curPath).ifEmpty { curPath }
                zipalignPath(channelPath) {
                    signPath(dealSign, channelPath, finish)
                }
            }
        } else if (dealFile.isDirectory) {
            val apkFileList = dealFile.listFiles()?.filter {
                it.isFile && "apk" == it.extension
            }
            ThreadPoolHelper.run(runnable = apkFileList?.map {
                Callable {
                    dealChannel(it)
                }
            }) { results ->
                results?.forEach {
                    it?.get()?.forEach { log ->
                        println(log)
                    }
                }
                if (apkFileList?.isNotEmpty() == true) {
                    val channelPath = getChannelPath(inputFilePath).ifEmpty { inputFilePath }
                    zipalignPath(channelPath) {
                        signPath(dealSign, channelPath, finish)
                    }
                } else {
                    println("目录下未找到apk")
                    finish.invoke()
                }
            }
        } else {
            println("未找到要处理的文件")
            finish.invoke()
        }
    }

    private fun getChannelPath(curPath: String): String {
        var channelPath = channelSavePath
        if (channelPath.isNotEmpty()) {
            if (!channelPath.endsWith(File.separator)) channelPath += File.separator
            if (!(channelPath.startsWith(File.separator) || channelPath.contains(":"))) channelPath =
                curPath + channelPath
        }
        return channelPath
    }

    /**
     * 处理加固包打渠道包
     */
    private fun dealChannel(inputApkPath: File, finish: (() -> Unit)? = null): List<String> {
        //如果是渠道包
        if (inputApkPath.nameWithoutExtension.contains("_")) return arrayListOf("已是渠道文件，不需处理")
        val logList = arrayListOf<String>()
        val curPath = inputApkPath.parent + File.separator
        val inputZipFile = ZipFile(inputApkPath)
        //创建渠道包存储文件夹
        val channelPath = getChannelPath(curPath)
        val channelPathFile = File(channelPath)
        channelPathFile.mkdirs()

        //解压得到AndroidManifest.xml
        val baseManifestFile = File(curPath + "AndroidManifest.xml")
        baseManifestFile.createNewFile()
        FileUtil.save2File(inputZipFile.getInputStream(inputZipFile.getEntry("AndroidManifest.xml")), baseManifestFile)
        logList.add("解压 ${inputApkPath.name} 得到AndroidManifest.xml")

        //先去除旧的渠道数据
        val outNoChannelFile = File(curPath + "AndroidManifestNoChannel.xml")
        outNoChannelFile.createNewFile()
        AXMLEditor2Util.doCommandTagDel(
            "meta-data", "UMENG_CHANNEL", baseManifestFile.path, outNoChannelFile.path
        )
        logList.add("去除原渠道并重命名为AndroidManifestNoChannel.xml")

        val channelsFile = File(channelsFilePath)
        ThreadPoolHelper.run(runnable = channelsFile.readLines().map {
            Callable {
                dealChannelApk(it, curPath, outNoChannelFile, channelPath, inputApkPath)
            }
        }) { results ->
            results?.forEach {
                it?.get()?.forEach { log ->
                    println(log)
                }
            }
            File(curPath + File.separator + "cache").delete()
            baseManifestFile.delete()
            outNoChannelFile.delete()
            inputZipFile.close()
            finish?.invoke()
        }
        return logList
    }

    private fun dealChannelApk(
        it: String, curPath: String, outNoChannelFile: File, channelPath: String, inputApkPath: File
    ): List<String> {
        val logList = arrayListOf<String>()
        val fields = it.split(" ")
        val channelName: String = fields[2].trim().replace("\n", "")
        val channelApkFileName: String = fields[1].trim().replace("\n", "")
        val outNoChannelFileNew =
            File(curPath + File.separator + "cache" + File.separator + outNoChannelFile.nameWithoutExtension + "_" + channelName + ".xml")
        outNoChannelFile.copyTo(outNoChannelFileNew, true)
        val baseManifestChannelFilePath = curPath + File.separator + "cache" + File.separator + channelName
        val baseManifestFileNew = File(baseManifestChannelFilePath + File.separator + "AndroidManifest.xml")
        outNoChannelFileNew.copyTo(baseManifestFileNew, true)
        //修改渠道数据
        val baseInsertFile = File(channelBaseInsertFilePath)
        val newChannelInsertFile = File(curPath + "insert_${channelName}.xml")
        baseInsertFile.copyTo(newChannelInsertFile, true)
        //更新新渠道文件内渠道
        newChannelInsertFile.writeText(
            newChannelInsertFile.readText().replace(defaultChannelName, channelName)
        )
        //插入渠道信息
        AXMLEditor2Util.doCommandTagInsert(
            newChannelInsertFile.path, outNoChannelFileNew.path, baseManifestFileNew.path
        )
        //用完删除新渠道文件
        newChannelInsertFile.delete()
        outNoChannelFileNew.delete()

        val newChannelApkFile =
            File(channelPath.ifEmpty { curPath } + "${inputApkPath.nameWithoutExtension}_${channelApkFileName}" + ".apk")
        if (newChannelApkFile.exists()) {
            newChannelApkFile.delete()
        }
        inputApkPath.copyTo(newChannelApkFile)
        val newChannelApkZipFile = ZipArchive(newChannelApkFile.toPath())
        //更新新渠道AndroidManifest.xml到渠道apk中
        newChannelApkZipFile.delete("AndroidManifest.xml")
        newChannelApkZipFile.add(
            BytesSource(
                baseManifestFileNew.toPath(), "AndroidManifest.xml", Deflater.NO_COMPRESSION
            )
        )
        newChannelApkZipFile.close()
        baseManifestFileNew.delete()
        File(baseManifestChannelFilePath).delete()
        logList.add("apk已更新渠道信息，并保存到${newChannelApkZipFile.path.toAbsolutePath()}")
        return logList
    }

    private fun zipalignPath(inputApkPath: String, finish: (() -> Unit)? = null) {
        val dealFile = File(inputApkPath)
        if (dealFile.isDirectory) {
            val dealFiles = dealFile.listFiles()?.filter {
                it.isFile && "apk" == it.extension
            }
            ThreadPoolHelper.run(runnable = dealFiles?.map {
                Callable {
                    dealZipalign(it)
                }
            }) { results ->
                results?.forEach {
                    it?.get()?.forEach { log ->
                        println(log)
                    }
                }
                finish?.invoke()
            }
        } else {
            dealZipalign(dealFile).forEach {
                println(it)
            }
            finish?.invoke()
        }
    }

    /**
     * 签名之前对齐zip
     */
    private fun dealZipalign(inputApkPath: File): List<String> {
        if (!inputApkPath.isFile || "apk" != inputApkPath.extension) return arrayListOf("非apk，不需处理")
        val logList = arrayListOf<String>()
        val curPath = inputApkPath.parent + File.separator
        if (!ZipalignUtil.check(inputApkPath.path)) {
            logList.add("正在对齐apk:${inputApkPath.name}")
            val zipFilePath = curPath + inputApkPath.nameWithoutExtension + "_zip.apk"
            if (ZipalignUtil.zipalign(inputApkPath.path, zipFilePath)) {
                //对齐后改为原来的名字
                inputApkPath.delete()
                val zipFile = File(zipFilePath)
                if (zipFile.renameTo(inputApkPath)) {
                    zipFile.delete()
                }
                logList.add("对齐apk:${inputApkPath.name}完成")
            } else {
                logList.add("对齐apk:${inputApkPath.name}失败")
            }
        } else {
            logList.add("apk:${inputApkPath.name}已对齐")
        }
        return logList
    }

    private fun signPath(dealSign: Boolean = true, inputApkPath: String, finish: (() -> Unit)? = null) {
        if (!dealSign) {
            finish?.invoke()
            return
        }
        val dealFile = File(inputApkPath)
        if (dealFile.isDirectory) {
            val dealFiles = dealFile.listFiles()?.filter {
                it.isFile && "apk" == it.extension
            }
            ThreadPoolHelper.run(runnable = dealFiles?.map {
                Callable {
                    signApk(it)
                }
            }) { results ->
                results?.forEach {
                    it?.get()?.forEach { log ->
                        println(log)
                    }
                }
                finish?.invoke()
            }
        } else {
            signApk(dealFile).forEach {
                println(it)
            }
            finish?.invoke()
        }
    }

    private fun signApk(inputFile: File): List<String> {
        if (!inputFile.isFile || "apk" != inputFile.extension) return arrayListOf("已签名，不需处理")
        if (!ZipalignUtil.check(inputFile.path)) return arrayListOf()
        val logList = arrayListOf<String>()
        val curPath = inputFile.parent + File.separator
        val inputFileName = inputFile.nameWithoutExtension
        if (inputFileName.contains("_sign")) return logList
        val outApkFile = curPath + inputFileName + "_sign.apk"
        if (ApkSignerUtil.sign(
                signFile = signFile,
                signAlias = signAlias,
                keyStorePassword = signPassword,
                keyPassword = signAliasPassword,
                outSignPath = outApkFile,
                inputApkPath = inputFile.path
            )
        ) {
            if (delApkAfterSign) {
                if (inputFilePath != inputFile.path) {
                    inputFile.delete()
                }
            }
            logList.add("签名已完成：$outApkFile")
        } else {
            logList.add("签名失败：$outApkFile")
        }
        return logList
    }
}