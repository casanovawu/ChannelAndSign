package com.wpf.util.common.ui.marketplace.markets

import com.wpf.util.common.ui.base.AbiType

object XiaomiMarket: Market {
    override fun uploadAbi() = arrayOf(AbiType.Abi32, AbiType.Abi64)
}