package com.johndeweydev.awps.viewmodel.serial.sessionviewmodel;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.johndeweydev.awps.model.data.AccessPointData;
import com.johndeweydev.awps.model.data.DeviceConnectionParamData;
import com.johndeweydev.awps.model.data.HashInfoEntity;
import com.johndeweydev.awps.model.data.MicFirstMessageData;
import com.johndeweydev.awps.model.data.MicSecondMessageData;
import com.johndeweydev.awps.model.data.PmkidFirstMessageData;
import com.johndeweydev.awps.model.repo.serial.sessionreposerial.SessionRepoSerial;
import com.johndeweydev.awps.viewmodel.serial.ViewModelIOControl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class SessionViewModel extends ViewModel implements ViewModelIOControl,
        SessionRepoSerial.RepositoryEvent {

  public boolean automaticAttack = false;
  public String selectedArmament;
  public SessionRepoSerial sessionRepoSerial;

  /**
   * Serial Listeners
   * The variables and live data below this comment is use for logging and setting up listeners
   * when an error occurred
   * */
  public MutableLiveData<String> currentAttackLog = new MutableLiveData<>();
  public MutableLiveData<String> currentSerialInputError = new MutableLiveData<>();
  public MutableLiveData<String> currentSerialOutputError = new MutableLiveData<>();
  public int attackLogNumber = 0;

  /**
   * INITIALIZATION PHASE
   * Variables and live data below this comment is use for setting up the target and activating
   * the armament
   * */
  public MutableLiveData<String> launcherStarted = new MutableLiveData<>();
  public MutableLiveData<String> launcherActivateConfirmation = new MutableLiveData<>();
  public String targetAccessPointSsid;

  /**
   * TARGET LOCKING PHASE
   * Variables and live data below this comment is use for scanning access points and checking if
   * the target is found on the scanned access points
   * */
  public boolean userWantsToScanForAccessPoint = false;
  public ArrayList<AccessPointData> accessPointDataList = new ArrayList<>();
  public MutableLiveData<ArrayList<AccessPointData>> launcherFinishScanning =
          new MutableLiveData<>();
  public MutableLiveData<String> launcherAccessPointNotFound = new MutableLiveData<>();

  /**
   * EXECUTION PHASE
   * Variables and live data below this comment is use when the attack is ongoing
   * */
  public MutableLiveData<String> launcherMainTaskCreated = new MutableLiveData<>();
  public boolean attackOnGoing = false;

  /**
   * POST EXECUTION PHASE
   * Variables and live data below this comment is use when the attack has finished, an attack can
   * either be successfully or a failure
   * */
  public HashInfoEntity launcherExecutionResultData;
  public MutableLiveData<String> launcherExecutionResult = new MutableLiveData<>();
  public MutableLiveData<String> launcherDeauthStopped = new MutableLiveData<>();

  public SessionViewModel(SessionRepoSerial sessionRepoSerial) {
    Log.d("dev-log", "SessionViewModel: Created new instance of SessionViewModel");
    this.sessionRepoSerial = sessionRepoSerial;
    sessionRepoSerial.setEventHandler(this);
  }

  public void writeControlCodeActivationToLauncher() {
    sessionRepoSerial.writeDataToDevice("06");
  }

  public void writeControlCodeDeactivationToLauncher() {
    sessionRepoSerial.writeDataToDevice("07");
  }

  public void writeControlCodeRestartLauncher() {
    attackLogNumber = 0;
    sessionRepoSerial.writeDataToDevice("08");
  }

  public void writeInstructionCodeForScanningDevicesToLauncher() {
    sessionRepoSerial.writeDataToDevice("01");
  }

  public void writeInstructionCodeToLauncher(String targetMacAddress) {
    String instructionCode = "";
    switch (selectedArmament) {
      case "PMKID Based Attack" -> instructionCode += "02";
      case "MIC Based Attack" -> instructionCode += "03";
      case "Deauther" -> instructionCode += "04";
    }
    instructionCode += targetMacAddress;
    sessionRepoSerial.writeDataToDevice(instructionCode);
  }

  @Override
  public void setLauncherEventHandler() {
    sessionRepoSerial.setLauncherEventHandler();
  }

  @Override
  public String connectToDevice(DeviceConnectionParamData deviceConnectionParamData) {
    return sessionRepoSerial.connectToDevice(deviceConnectionParamData);
  }

  @Override
  public void disconnectFromDevice() {
    sessionRepoSerial.disconnectFromDevice();
  }

  @Override
  public void startEventDrivenReadFromDevice() {
    sessionRepoSerial.startEventDrivenReadFromDevice();
  }

  @Override
  public void stopEventDrivenReadFromDevice() {
    sessionRepoSerial.stopEventDrivenReadFromDevice();
  }

  @Override
  public void onRepoOutputError(String error) {
    Log.d("dev-log", "SessionViewModel.onLauncherOutputError: Serial -> " + error);
    currentSerialOutputError.postValue(error);
  }

  @Override
  public void onRepoInputError(String input) {
    Log.d("dev-log", "SessionViewModel.onLauncherInputError: Serial -> " + input);
    currentSerialInputError.postValue(input);
  }

  @Override
  public void onRepoStarted() {
    launcherStarted.postValue("Launcher module started");
    currentAttackLog.postValue("(" + attackLogNumber + ") " + "Module started");
    attackLogNumber++;
  }

  @Override
  public void onRepoArmamentStatus(String armament, String targetBssid) {
    currentAttackLog.postValue("(" + attackLogNumber + ") " + "Using " + armament + "" +
            ", targeting " + targetBssid);
    attackLogNumber++;
  }

  @Override
  public void onRepoInstructionIssued(String armament, String targetBssid) {
    if (userWantsToScanForAccessPoint) {
      currentAttackLog.postValue("(" + attackLogNumber + ") " + "Using " + armament);
      launcherActivateConfirmation.postValue("Proceed to scan for nearby access points?");
    } else {
      currentAttackLog.postValue("(" + attackLogNumber + ") " + "Using " + armament +
              ", target set " + targetBssid);
      launcherActivateConfirmation.postValue("Do you wish to activate the attack targeting "
              + targetAccessPointSsid + " using " + selectedArmament + "?");
    }
    attackLogNumber++;
  }

  @Override
  public void onRepoArmamentActivation() {
    currentAttackLog.postValue("(" + attackLogNumber + ") " + "Armament activate!");
    attackLogNumber++;
  }

  @Override
  public void onRepoArmamentDeactivation() {
    currentAttackLog.postValue("(" + attackLogNumber + ") " + "Armament deactivate!");
    attackLogNumber++;
  }

  @Override
  public void onRepoNumberOfFoundAccessPoints(String numberOfAps) {
    currentAttackLog.postValue("(" + attackLogNumber + ") " + "Found " + numberOfAps +
            " access points");
    accessPointDataList.clear();
    attackLogNumber++;
  }

  @Override
  public void onRepoFoundAccessPoint(AccessPointData accessPointData) {
    if (userWantsToScanForAccessPoint) {
      accessPointDataList.add(accessPointData);
    }
    String ssid = accessPointData.ssid();
    int channel = accessPointData.channel();
    currentAttackLog.postValue("(" + attackLogNumber + ") " + ssid + " at channel " + channel);
    attackLogNumber++;
  }

  @Override
  public void onRepoFinishScan() {
    if (userWantsToScanForAccessPoint) {
      launcherFinishScanning.postValue(accessPointDataList);
    }
    currentAttackLog.postValue("(" + attackLogNumber + ") " + "Done scanning access point");
    attackLogNumber++;
  }

  @Override
  public void onRepoTargetAccessPointNotFound() {
    currentAttackLog.postValue("(" + attackLogNumber + ") " + "The target " +
            targetAccessPointSsid + " is not found");
    attackLogNumber++;
    launcherAccessPointNotFound.postValue(targetAccessPointSsid);
  }

  @Override
  public void onRepoLaunchingSequence() {
    currentAttackLog.postValue("(" + attackLogNumber + ") "  + selectedArmament +
            " launching sequence");
    attackLogNumber++;
  }

  @Override
  public void onRepoMainTaskCreated() {
    currentAttackLog.postValue("(" + attackLogNumber + ") " + "Main task created");
    attackLogNumber++;
    launcherMainTaskCreated.postValue(selectedArmament + " main task created");
    attackOnGoing = true;
  }

  @Override
  public void onRepoPmkidWrongKeyType(String keyType) {
    currentAttackLog.postValue("(" + attackLogNumber + ") " + "Got wrong PMKID key type, "
            + keyType);
    attackLogNumber++;
  }

  @Override
  public void onRepoPmkidWrongOui(String oui) {
    currentAttackLog.postValue("(" + attackLogNumber + ") " + "Got wrong PMKID key data OUI, "
            + oui);
    attackLogNumber++;
  }

  @Override
  public void onRepoPmkidWrongKde(String kde) {
    currentAttackLog.postValue("(" + attackLogNumber + ") " + "Got wrong PMKID KDE, " +
            kde);
    attackLogNumber++;
  }

  @Override
  public void onRepoMainTaskCurrentStatus(String attackType, int attackStatus) {
    currentAttackLog.postValue("(" + attackLogNumber + ") " + attackType + ", status is " +
            attackStatus);
    attackLogNumber++;
  }

  @Override
  public void onRepoReceivedEapolMessage(
          String attackType,
          int messageNumber,
          @Nullable PmkidFirstMessageData pmkidFirstMessageData,
          @Nullable MicFirstMessageData micFirstMessageData,
          @Nullable MicSecondMessageData micSecondMessageData
  ) {
    if (attackType.equals("PMKID") && messageNumber == 1) {
      handlePmkidEapolMessageEvent(pmkidFirstMessageData);
    } else if (attackType.equals("MIC")) {
      handleMicEapolMessageEvent(micFirstMessageData, micSecondMessageData, messageNumber);
    }
  }

  private void handlePmkidEapolMessageEvent(PmkidFirstMessageData pmkidFirstMessageData) {
    if (pmkidFirstMessageData == null) {
      Log.d("dev-log", "SessionViewModel.handlePmkidEapolMessageEvent: " +
              "PMKID data is null");
      return;
    }

    String result = "PMKID is " + pmkidFirstMessageData.pmkid();

    String ssid = targetAccessPointSsid;
    String bssid = pmkidFirstMessageData.bssid();
    String clientMacAddress = pmkidFirstMessageData.client();
    String keyType = "PMKID";
    String aNonce = "None";
    String hashData = pmkidFirstMessageData.pmkid();
    String keyData = "None";
    String dateCaptured = createStringDateTime();

    // Set the default value for location, fragment will populate this data before saving it in
    // the local database
    String latitude = "0.0";
    String longitude = "0.0";
    String address = "None";

    launcherExecutionResultData = new HashInfoEntity(
            ssid, bssid, clientMacAddress, keyType, aNonce, hashData, keyData, dateCaptured,
            latitude, longitude, address);

    currentAttackLog.postValue("(" + attackLogNumber + ") " + result);
    attackLogNumber++;
  }

  private void handleMicEapolMessageEvent(
          @Nullable MicFirstMessageData micFirstMessageData,
          @Nullable MicSecondMessageData micSecondMessageData,
          int messageNumber
  ) {
    if (messageNumber == 1) {
      if (micFirstMessageData == null) {
        Log.d("dev-log", "SessionViewModel.onLauncherReceivedEapolMessage: " +
                "MIC first message is null");
        return;
      }

      String result = "Anonce is " + micFirstMessageData.anonce();

      String ssid = targetAccessPointSsid;
      String bssid = micFirstMessageData.bssid();
      String clientMacAddress = micFirstMessageData.clientMacAddress();
      String keyType = "MIC";
      String aNonce = micFirstMessageData.anonce();
      String hashData = "None";
      String keyData = "None";
      String dateCaptured = createStringDateTime();

      // Set default value for the location, fragment will populate this data before saving it in
      // the local database
      String latitude = "0.0";
      String longitude = "0.0";
      String address = "None";

      launcherExecutionResultData = new HashInfoEntity(
              ssid, bssid, clientMacAddress, keyType, aNonce, hashData, keyData, dateCaptured,
              latitude, longitude, address);

      currentAttackLog.postValue("(" + attackLogNumber + ") " +
              "Got anonce from first EAPOL message. " + result);
      attackLogNumber++;
    } else if (messageNumber == 2) {
      if (micSecondMessageData == null) {
        Log.d("dev-log", "SessionViewModel.onLauncherReceivedEapolMessage: " +
                "MIC first message is null");
        return;
      }

      String result = "MIC is " + micSecondMessageData.getMic();

      launcherExecutionResultData.hashData = micSecondMessageData.getMic();
      launcherExecutionResultData.keyData = micSecondMessageData.getAllData();

      currentAttackLog.postValue("(" + attackLogNumber + ") " +
              "Got EAPOL data from second message. " + result);
      attackLogNumber++;
    }
  }

  private String createStringDateTime() {
    LocalDateTime dateTime = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss yyyy-MM-dd");
    return dateTime.format(formatter);
  }

  @Override
  public void onRepoFinishingSequence() {
    currentAttackLog.postValue("(" + attackLogNumber + ") " +  selectedArmament +
            " finishing sequence");
    attackLogNumber++;
  }

  @Override
  public void onRepoSuccess() {
    currentAttackLog.postValue("(" + attackLogNumber + ") " + selectedArmament +
            " successfully executed");
    attackLogNumber++;
    launcherExecutionResult.postValue("Success");
    attackOnGoing = false;

  }

  @Override
  public void onRepoFailure(String targetBssid) {
    currentAttackLog.postValue("(" + attackLogNumber + ") " +  selectedArmament + " failed");
    attackLogNumber++;
    launcherExecutionResult.postValue("Failed");
    attackOnGoing = false;

  }

  @Override
  public void onRepoMainTaskInDeautherStopped(String targetBssid) {
    currentAttackLog.postValue("(" + attackLogNumber + ") " + "In " + selectedArmament +
            " deauthentication task stopped");
    attackLogNumber++;
    launcherDeauthStopped.postValue("Stopped");
    attackOnGoing = false;
  }
}
