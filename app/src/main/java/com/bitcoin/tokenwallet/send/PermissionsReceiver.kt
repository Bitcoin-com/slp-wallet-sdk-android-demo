package com.bitcoin.tokenwallet.send

interface PermissionsReceiver {
    fun onPermissionResult(grantResult: Int)

    companion object {
        val REQ_CAMERA: Int = 1234
    }
}