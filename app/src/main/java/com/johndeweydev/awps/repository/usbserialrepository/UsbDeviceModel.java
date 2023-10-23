package com.johndeweydev.awps.repository.usbserialrepository;

import android.hardware.usb.UsbDevice;

import com.hoho.android.usbserial.driver.UsbSerialDriver;

import java.util.Locale;

public class UsbDeviceModel {

  public UsbDevice usbDevice;
  public int devicePort;
  public UsbSerialDriver usbSerialDriver;

  public UsbDeviceModel(UsbDevice usbDevice, int devicePort, UsbSerialDriver usbSerialDriver) {
    this.usbDevice = usbDevice;
    this.devicePort = devicePort;
    this.usbSerialDriver = usbSerialDriver;
  }

  public String getDeviceName() {
    if (usbSerialDriver == null) {
      return "<No driver>";
    } else {
      return usbSerialDriver
              .getClass()
              .getSimpleName()
              .replace("SerialDriver", "");
    }
  }

  public String getDeviceProductId() {
    return String.format(Locale.US, "%04X", usbDevice.getProductId());
  }

  public String getDeviceVendorId() {
    return String.format(Locale.US, "%04X", usbDevice.getVendorId());
  }
}