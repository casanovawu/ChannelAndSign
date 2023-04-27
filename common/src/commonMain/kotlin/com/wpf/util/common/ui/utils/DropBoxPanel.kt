package com.wpf.util.common.ui.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDropEvent
import javax.swing.JPanel
import kotlin.math.roundToInt

class DropBoundsBean(
    var x: Float = 0f,
    var y: Float = 0f,
    var width: Int = 0,
    var height: Int = 0,
)

@Composable
fun DropBoxPanel(
    modifier: Modifier,
    window: ComposeWindow,
    component: JPanel = JPanel(),
    onFileDrop: (List<String>) -> Unit
) {

    val dropBoundsBean = remember {
        mutableStateOf(DropBoundsBean())
    }

    Box(
        modifier = modifier.onPlaced {
            dropBoundsBean.value = DropBoundsBean(
                x = it.positionInWindow().x,
                y = it.positionInWindow().y,
                width = it.size.width,
                height = it.size.height
            )
        }) {
        LaunchedEffect(true) {
            component.setBounds(
                dropBoundsBean.value.x.roundToInt(),
                dropBoundsBean.value.y.roundToInt(),
                dropBoundsBean.value.width,
                dropBoundsBean.value.height
            )
            window.contentPane.add(component)

            object : DropTarget(component, object : DropTargetAdapter() {
                override fun drop(event: DropTargetDropEvent) {

                    event.acceptDrop(DnDConstants.ACTION_REFERENCE)
//                    if (event.location.x < component.x || event.location.x > component.x + component.width
//                        || event.location.y < component.y || event.location.y > component.y + component.height) {
//                        event.dropComplete(true)
//                        return
//                    }
                    val dataFlavors = event.transferable.transferDataFlavors
                    dataFlavors.forEach {
                        if (it == DataFlavor.javaFileListFlavor) {
                            val list = event.transferable.getTransferData(it) as List<*>

                            val pathList = mutableListOf<String>()
                            list.forEach { filePath ->
                                pathList.add(filePath.toString())
                            }
                            onFileDrop(pathList)
                        }
                    }
                    event.dropComplete(true)
                }
            }) {

            }
        }

        SideEffect {
            component.setBounds(
                dropBoundsBean.value.x.roundToInt(),
                dropBoundsBean.value.y.roundToInt(),
                dropBoundsBean.value.width,
                dropBoundsBean.value.height
            )
        }

        DisposableEffect(true) {
            onDispose {
                window.contentPane.remove(component)
            }
        }
    }
}