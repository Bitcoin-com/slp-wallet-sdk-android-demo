package com.bitcoin.tokenwallet.send;

import androidx.annotation.UiThread;
import com.google.android.gms.vision.barcode.Barcode;

/**
 * Consume the item instance detected from an Activity or Fragment level by implementing the
 * BarcodeUpdateListener interface method onBarcodeDetected.
 */
interface BarcodeUpdateListener {
  @UiThread
  void onBarcodeDetected(Barcode barcode);
}
