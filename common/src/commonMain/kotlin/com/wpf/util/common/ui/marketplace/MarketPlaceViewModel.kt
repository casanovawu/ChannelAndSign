package com.wpf.util.common.ui.marketplace

import com.wpf.util.common.ui.channelset.Client
import com.wpf.util.common.ui.channelset.ChannelSetViewModel
import com.wpf.util.common.ui.marketplace.markets.base.Market
import com.wpf.util.common.ui.marketplace.markets.base.MarketType
import com.wpf.util.common.ui.utils.gson
import com.wpf.util.common.ui.utils.settings

object MarketPlaceViewModel {

    fun getDefaultSelectChannelList(): List<Client> {
        return ChannelSetViewModel.getClientList().apply {
            forEach {
                it.changeSelect(false)
            }
            first().changeSelect(true)
        }
    }

    fun getSelectMarket(clientList: List<Client>? = null, marketList: List<Market>? = null): Market {
        return getSelectMarket(
            (clientList ?: getDefaultSelectChannelList()).find { it.isSelect }?.name ?: "",
            (marketList ?: getCanApiMarketList()).find { it.isSelect }?.name ?: ""
        )
    }

    private val marketMap = mutableMapOf<String, Market>()
    fun getSelectMarket(channelName: String, marketName: String): Market {
        val key = "Channel${channelName}Market${marketName}"
        val market = marketMap[key]
        market?.initByData()
        if (market != null) {
            return market
        }
        val dataJson = settings.getString(key, "{}")
        val saveMarket = gson.fromJson(dataJson, getCanApiMarketList().find { it.name == marketName }!!.javaClass)
        saveMarket?.changeSelect(getCanApiMarketList().find { it.name == marketName }?.isSelect ?: false)
        saveMarket.initByData()
        marketMap[key] = saveMarket
        return saveMarket
    }

    private var canApiMarketList : MutableList<Market>? = null
    fun getCanApiMarketList(): List<Market> {
        if (canApiMarketList == null) {
            canApiMarketList = MarketType.values().filter { marketType ->
                marketType.canApi()
            }.map { marketType ->
                gson.fromJson("{}", marketType.market.java)
            }.toMutableList()
            if (canApiMarketList!!.find { find -> find.isSelect } == null) {
                canApiMarketList!![0].changeSelect(true)
            }
        }
        return canApiMarketList!!
    }

    fun getCanApiMarketTypeList(): List<MarketType> {
        val marketList = MarketType.values().filter { marketType ->
            marketType.canApi()
        }
        return marketList
    }

    fun saveMarketList(clientList: List<Client>, marketSelect: Market) {
        val channelSelect = clientList.find { it.isSelect } ?: return
        val key = "Channel${channelSelect.name}Market${marketSelect.name}"
        marketMap.remove(key)
        settings.putString(key, gson.toJson(marketSelect))
        //换信息后清空初始化的数据
        marketSelect.clearInitData()
    }
}