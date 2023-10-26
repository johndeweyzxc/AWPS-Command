package com.johndeweydev.awps.repository.usbserialrepository;

import com.johndeweydev.awps.launcher.LauncherStages;
import com.johndeweydev.awps.launcher.LauncherEvent;
import com.johndeweydev.awps.models.LauncherOutputModel;
import com.johndeweydev.awps.models.UsbDeviceModel;
import com.johndeweydev.awps.launcher.LauncherSingleton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class UsbSerialRepository {

  private final StringBuilder queueData = new StringBuilder();

  public void setUsbSerialViewModelCallback(
          UsbSerialRepositoryEvent usbSerialRepositoryEvent
  ) {
    LauncherEvent launcherEvent = new LauncherEvent() {
      @Override
      public void onLauncherOutput(String data) {
        char[] dataChar = data.toCharArray();
        for (char c : dataChar) {
          if (c == '\n') {
            notifyViewModelAboutData();
            queueData.setLength(0);
          } else {
            queueData.append(c);
          }
        }
      }

      private void notifyViewModelAboutData() {
        String strData = queueData.toString();
        String strTime = createStringTime();

        LauncherOutputModel launcherOutputModel = new LauncherOutputModel(strTime, strData);
        usbSerialRepositoryEvent.onRepositoryOutputRaw(launcherOutputModel);

        char firstChar = strData.charAt(0);
        char lastChar = strData.charAt(strData.length() - 2);
        if (firstChar == '{' && lastChar == '}') {
          usbSerialRepositoryEvent.onRepositoryOutputFormatted(launcherOutputModel);
        }
      }

      @Override
      public void onLauncherOutputError(String errorMessageOnNewData) {
        usbSerialRepositoryEvent.onRepositoryOutputError(errorMessageOnNewData);
      }
      @Override
      public void onLauncherInputError(String dataToWrite) {
        usbSerialRepositoryEvent.onRepositoryInputError(dataToWrite);
      }
    };

    LauncherSingleton.getInstance().getLauncher().setLauncherSerialDataEvent(
            launcherEvent
    );
  }

  private String createStringTime() {
    Calendar calendar = Calendar.getInstance();
    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    return dateFormat.format(calendar.getTime());
  }

  public ArrayList<UsbDeviceModel> discoverDevices() {
    return LauncherSingleton.getInstance().getLauncher().discoverDevices();
  }

  public String connect(
          int baudRate, int dataBits, int stopBits, int parity, int deviceId, int portNum) {
    LauncherStages status = LauncherSingleton.getInstance().getLauncher().connect(
            baudRate, dataBits, stopBits, parity, deviceId, portNum
    );

    switch (status) {
      case ALREADY_CONNECTED: return "Already connected";
      case DEVICE_NOT_FOUND: return "Device not found";
      case DRIVER_NOT_FOUND: return "Driver not found";
      case PORT_NOT_FOUND: return "Port not found";
      case NO_USB_PERMISSION: return "No usb permission";
      case SUCCESSFULLY_CONNECTED: return "Successfully connected";
      case UNSUPPORTED_PORT_PARAMETERS: return "Unsupported port parameters";
      case FAILED_OPENING_DEVICE: return "Failed to open the device";
      default: return "None";
    }
  }

  public void disconnect() {
    LauncherSingleton.getInstance().getLauncher().disconnect();
  }

  public void startReading() {
    LauncherSingleton.getInstance().getLauncher().startReading();
  }

  public void stopReading() {
    LauncherSingleton.getInstance().getLauncher().stopReading();
  }

  public void writeData(String data) {
    LauncherSingleton.getInstance().getLauncher().writeData(data);
  }
}
