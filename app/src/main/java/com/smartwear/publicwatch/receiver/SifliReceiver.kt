package com.smartwear.publicwatch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.smartwear.publicwatch.ui.eventbus.EventAction
import com.smartwear.publicwatch.ui.eventbus.EventMessage
import com.sifli.siflidfu.Protocol
import com.sifli.siflidfu.SifliDFUService
import com.sifli.watchfacelibrary.SifliWatchfaceService
import org.greenrobot.eventbus.EventBus

/**
 * Created by Android on 2023/7/13.
 */
class SifliReceiver : BroadcastReceiver() {

    var dfuProgress = -1

    var faceProgress = -1

    data class DFUState(var state: Int, var stateResult: Int)

    data class DFUProgress(var progress: Int, var progressType: Int)

    data class SifliFaceState(var state: Int, var rsp: Int)


    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            when (intent.action) {
                SifliDFUService.BROADCAST_DFU_LOG -> {
                    val log = intent.getStringExtra(SifliDFUService.EXTRA_LOG_MESSAGE)
                    Log.d("DFU LOG", log ?: "null")
                }

                SifliDFUService.BROADCAST_DFU_STATE -> {
                    val state = intent.getIntExtra(SifliDFUService.EXTRA_DFU_STATE, 0)
                    val stateResult = intent.getIntExtra(SifliDFUService.EXTRA_DFU_STATE_RESULT, 0)
                    Log.d("DFU STATE", " state:$state,stateResult:$stateResult")
                    EventBus.getDefault().post(EventMessage(EventAction.ACTION_SIFLI_DFU_STATE, DFUState(state, stateResult)))
                    if (stateResult != 0) {
                        //失败
                        dfuProgress = -1
                    } else {
                        if (state == Protocol.DFU_SERVICE_EXIT) {
                            //成功
                            dfuProgress = -1
                        }
                    }
                }

                SifliDFUService.BROADCAST_DFU_PROGRESS -> {
                    val progress = intent.getIntExtra(SifliDFUService.EXTRA_DFU_PROGRESS, 0)
                    val progressType = intent.getIntExtra(SifliDFUService.EXTRA_DFU_PROGRESS_TYPE, 0)
                    if (dfuProgress != progress) {
                        dfuProgress = progress
                        Log.d("DFU PROGRESS", "progress:$progress,progressType:$progressType")
                        EventBus.getDefault().post(EventMessage(EventAction.ACTION_SIFLI_DFU_PROGRESS, DFUProgress(dfuProgress, progressType)))
                    }
                }

                SifliWatchfaceService.BROADCAST_WATCHFACE_STATE -> {
                    val state = intent.getIntExtra(SifliWatchfaceService.EXTRA_WATCHFACE_STATE, -1)
                    val rsp = intent.getIntExtra(SifliWatchfaceService.EXTRA_WATCHFACE_STATE_RSP, 0)

                    Log.d("FACE STATE", "state:$state, rsp:$rsp")
                    EventBus.getDefault().post(EventMessage(EventAction.ACTION_SIFLI_FACE_STATE, SifliFaceState(state, rsp)))
                    faceProgress = -1
                    /*if(state == 0){
                        //成功
                    }else{
                        //失败 ， rsp失败原因
                    }*/
                }

                SifliWatchfaceService.BROADCAST_WATCHFACE_PROGRESS -> {
                    val progress = intent.getIntExtra(SifliWatchfaceService.EXTRA_WATCHFACE_PROGRESS, 0)
                    if (faceProgress != progress) {
                        faceProgress = progress
                        Log.d("FACE PROGRESS", "progress:$progress")
                        EventBus.getDefault().post(EventMessage(EventAction.ACTION_SIFLI_FACE_PROGRESS, progress))
                    }

                }
            }
        }
    }
}