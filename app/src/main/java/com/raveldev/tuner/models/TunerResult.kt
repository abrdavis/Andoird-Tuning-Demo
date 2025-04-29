package com.raveldev.tuner.models

data class TunerResult(val noteDisplayText:String,
                  val inTune: Boolean,
                  val isSharp: Boolean,
                  val isFlat: Boolean) {

    fun GetDisplayText() : String{
        if(isSharp){
            return "$noteDisplayText is sharp";
        }
        else if(isFlat){
            "$noteDisplayText is flat";
        }

        return noteDisplayText;
    }

}