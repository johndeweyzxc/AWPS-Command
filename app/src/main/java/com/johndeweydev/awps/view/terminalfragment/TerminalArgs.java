package com.johndeweydev.awps.view.terminalfragment;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class TerminalArgs implements Parcelable {

  private final int deviceId;
  private final int portNum;
  private final int baudRate;

  public TerminalArgs(int deviceId, int portNum, int baudRate) {

    this.deviceId = deviceId;
    this.portNum = portNum;
    this.baudRate = baudRate;
  }

  public int getDeviceId() {
    return deviceId;
  }

  public int getPortNum() {
    return portNum;
  }

  public int getBaudRate() {
    return baudRate;
  }

  protected TerminalArgs(Parcel in) {
    deviceId = in.readInt();
    portNum = in.readInt();
    baudRate = in.readInt();
  }

  public static final Creator<TerminalArgs> CREATOR = new Creator<TerminalArgs>() {
    @Override
    public TerminalArgs createFromParcel(Parcel in) {
      return new TerminalArgs(in);
    }

    @Override
    public TerminalArgs[] newArray(int size) {
      return new TerminalArgs[size];
    }
  };

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(@NonNull Parcel dest, int flags) {
    dest.writeInt(deviceId);
    dest.writeInt(portNum);
    dest.writeInt(baudRate);
  }
}
