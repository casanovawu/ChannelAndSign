package com.wpf.util.common.ui.channelset

import com.wpf.util.common.ui.base.Group
import kotlinx.serialization.Serializable

@Serializable
class Channel(
    override var name: String = "",
    override var isSelect: Boolean = false,
    var channelPath: String = ""
) : Group(name, isSelect)