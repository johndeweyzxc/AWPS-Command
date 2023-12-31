package com.johndeweydev.awps.view.terminalfragment;

import static com.johndeweydev.awps.AppConstants.BAUD_RATE;
import static com.johndeweydev.awps.AppConstants.DATA_BITS;
import static com.johndeweydev.awps.AppConstants.PARITY_NONE;
import static com.johndeweydev.awps.AppConstants.STOP_BITS;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.johndeweydev.awps.R;
import com.johndeweydev.awps.databinding.FragmentTerminalBinding;
import com.johndeweydev.awps.model.data.DeviceConnectionParamData;
import com.johndeweydev.awps.model.data.LauncherOutputData;
import com.johndeweydev.awps.viewmodel.serial.terminalviewmodel.TerminalViewModel;
import com.johndeweydev.awps.view.autoarmafragment.AutoArmaArgs;
import com.johndeweydev.awps.view.manualarmafragment.ManualArmaArgs;

public class TerminalFragment extends Fragment {

  private FragmentTerminalBinding binding;
  private String selectedArmament;
  private TerminalViewModel terminalViewModel;
  private TerminalArgs terminalArgs = null;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    terminalViewModel = new ViewModelProvider(requireActivity()).get(TerminalViewModel.class);
    binding = FragmentTerminalBinding.inflate(inflater, container, false);

    if (getArguments() == null) {
      Log.d("dev-log", "TerminalFragment.onCreateView: Get arguments is null");
    } else {
      Log.d("dev-log", "TerminalFragment.onCreateView: Initializing fragment args");
      TerminalFragmentArgs terminalFragmentArgs;
      if (getArguments().isEmpty()) {
        Log.w("dev-log", "TerminalFragment.onCreateView: Terminal argument is missing, " +
                "using data in the view model");
        terminalArgs = new TerminalArgs(
                terminalViewModel.deviceIdFromTerminalArgs,
                terminalViewModel.portNumFromTerminalArgs,
                terminalViewModel.baudRateFromTerminalArgs);
      } else {
        Log.d("dev-log", "TerminalFragment.onCreateView: Getting terminal argument " +
                "from bundle");
        terminalFragmentArgs = TerminalFragmentArgs.fromBundle(getArguments());
        terminalArgs = terminalFragmentArgs.getTerminalArgs();

        terminalViewModel.deviceIdFromTerminalArgs = terminalArgs.getDeviceId();
        terminalViewModel.portNumFromTerminalArgs = terminalArgs.getPortNum();
        terminalViewModel.baudRateFromTerminalArgs = terminalArgs.getBaudRate();
      }
    }
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    if (terminalArgs == null) {
      Log.d("dev-log", "TerminalFragment.onViewCreated: Terminal args is null");
      Navigation.findNavController(binding.getRoot()).popBackStack();
      return;
    }

    binding.materialToolBarTerminal.setNavigationOnClickListener(v ->
            binding.drawerLayoutTerminal.open());
    TerminalRVAdapter terminalRVAdapter = setupRecyclerView();
    binding.navigationViewTerminal.setNavigationItemSelectedListener(item ->
            navItemSelected(item, terminalRVAdapter));
    binding.buttonCreateCommandTerminal.setOnClickListener(v ->
            showDialogAskUserToEnterInstructionCode());

    setupObservers(terminalRVAdapter);
  }

  private TerminalRVAdapter setupRecyclerView() {
    TerminalRVAdapter terminalRVAdapter = new TerminalRVAdapter();
    LinearLayoutManager layout = new LinearLayoutManager(requireContext());
    layout.setStackFromEnd(true);
    binding.recyclerViewLogsTerminal.setAdapter(terminalRVAdapter);
    binding.recyclerViewLogsTerminal.setLayoutManager(layout);
    return terminalRVAdapter;
  }

  private void showDialogAskUserToEnterInstructionCode() {
    View dialogCommandInput = LayoutInflater.from(requireContext()).inflate(
            R.layout.dialog_command_input, null);
    TextInputEditText textInputEditTextDialogCommandInput = dialogCommandInput.findViewById(
            R.id.textInputEditTextDialogCommandInput);

    textInputEditTextDialogCommandInput.requestFocus();

    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
    builder.setTitle("Command Instruction Input");
    builder.setMessage("Enter command instruction code that will be sent to the launcher module");
    builder.setView(dialogCommandInput);
    builder.setPositiveButton("SEND", (dialog, which) -> {
      if (textInputEditTextDialogCommandInput.getText() == null) {
        dialog.dismiss();
      }

      String instructionCode = textInputEditTextDialogCommandInput.getText().toString();
      terminalViewModel.writeDataToDevice(instructionCode);
    });
    builder.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());

    AlertDialog dialog = builder.create();
    if (dialog.getWindow() != null) {
      dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }
    dialog.show();
  }

  private void setupObservers(TerminalRVAdapter terminalRVAdapter) {
    final Observer<LauncherOutputData> currentSerialOutputObserver = s -> {
      if (s == null) {
        return;
      }
      terminalRVAdapter.appendNewTerminalLog(s);
      binding.recyclerViewLogsTerminal.scrollToPosition(terminalRVAdapter.getItemCount() - 1);
    };
    terminalViewModel.currentSerialOutputRaw.observe(getViewLifecycleOwner(),
            currentSerialOutputObserver);
    terminalViewModel.currentSerialOutput.observe(getViewLifecycleOwner(),
            currentSerialOutputObserver);

    setupSerialInputErrorListener();
    setupSerialOutputErrorListener();
  }

  private void setupSerialInputErrorListener() {
    final Observer<String> writeErrorListener = s -> {
      if (s == null) {
        return;
      }
      terminalViewModel.currentSerialInputError.setValue(null);
      Log.d("dev-log", "TerminalFragment.setupSerialInputErrorListener: " +
              "Error on user input");
      stopEventReadAndDisconnectFromDevice();
      Toast.makeText(requireActivity(), "Error writing " + s, Toast.LENGTH_SHORT).show();
      Log.d("dev-log", "TerminalFragment.setupSerialInputErrorListener: " +
              "Popping this fragment off the back stack");
      Navigation.findNavController(binding.getRoot()).popBackStack();
    };
    terminalViewModel.currentSerialInputError.observe(getViewLifecycleOwner(), writeErrorListener);
  }

  private void setupSerialOutputErrorListener() {
    final Observer<String> onNewDataErrorListener = s -> {
      if (s == null) {
        return;
      }
      terminalViewModel.currentSerialOutputError.setValue(null);
      Log.d("dev-log", "TerminalFragment.setupSerialOutputErrorListener: " +
              "Error on serial output");
      stopEventReadAndDisconnectFromDevice();
      Toast.makeText(requireActivity(), "Error: " + s, Toast.LENGTH_SHORT).show();
      Log.d("dev-log", "TerminalFragment.setupSerialOutputErrorListener: " +
              "Popping this fragment off the back stack");
      Navigation.findNavController(binding.getRoot()).popBackStack();
    };
    terminalViewModel.currentSerialOutputError.observe(
            getViewLifecycleOwner(), onNewDataErrorListener);
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d("dev-log", "TerminalFragment.onResume: Fragment resumed");
    Log.d("dev-log", "TerminalFragment.onResume: Connecting to device");
    connectToDevice();
    terminalViewModel.setLauncherEventHandler();
  }

  private void connectToDevice() {
    int deviceId = terminalArgs.getDeviceId();
    int portNum = terminalArgs.getPortNum();
    DeviceConnectionParamData deviceConnectionParamData = new DeviceConnectionParamData(
            BAUD_RATE, DATA_BITS, STOP_BITS, PARITY_NONE, deviceId, portNum);
    String result = terminalViewModel.connectToDevice(deviceConnectionParamData);

    if (result.equals("Successfully connected") || result.equals("Already connected")) {

      Log.d("dev-log",
              "TerminalFragment.connectToDevice: Starting event read");
      terminalViewModel.startEventDrivenReadFromDevice();
    } else {
      Log.d("dev-log", "TerminalFragment.connectToDevice: " + result);
      Toast.makeText(requireActivity(), "Failed to connect to the device", Toast.LENGTH_SHORT)
              .show();
      stopEventReadAndDisconnectFromDevice();

      Log.d("dev-log", "TerminalFragment.connectToDevice: " +
              "Popping this fragment off the back stack");
      Navigation.findNavController(binding.getRoot()).popBackStack();
    }
  }

  @Override
  public void onPause() {
    Log.d("dev-log", "TerminalFragment.onPause: Fragment pausing");
    stopEventReadAndDisconnectFromDevice();
    super.onPause();
    Log.d("dev-log", "TerminalFragment.onPause: Fragment paused");
  }

  @Override
  public void onDestroyView() {
    binding = null;
    super.onDestroyView();
  }

  private void stopEventReadAndDisconnectFromDevice() {
    Log.d("dev-log", "TerminalFragment.stopEventReadAndDisconnectFromDevice: " +
            "Stopping event read");
    terminalViewModel.stopEventDrivenReadFromDevice();
    Log.d("dev-log", "TerminalFragment.stopEventReadAndDisconnectFromDevice: " +
            "Disconnecting from the device");
    terminalViewModel.disconnectFromDevice();
  }

  private boolean navItemSelected(MenuItem item, TerminalRVAdapter terminalRVAdapter) {
    if (item.getItemId() == R.id.databaseMenuNavItemTerminal) {

      binding.drawerLayoutTerminal.close();
      Navigation.findNavController(binding.getRoot()).navigate(
              R.id.action_terminalFragment_to_hashesFragment);
      return true;
    } else if (item.getItemId() == R.id.automaticMenuNavItemTerminal) {

      binding.drawerLayoutTerminal.close();
      showAttackTypeDialogSelector(true);
      return true;
    } else if (item.getItemId() == R.id.manualAttackMenuNavItemTerminal) {

      binding.drawerLayoutTerminal.close();
      showAttackTypeDialogSelector(false);
      return true;
    } else if (item.getItemId() == R.id.clearLogsMenuNavItemTerminal) {

      binding.drawerLayoutTerminal.close();
      terminalRVAdapter.clearLogs();
      return true;
    } else if (item.getItemId() == R.id.settingsMenuNavItemTerminal) {

      binding.drawerLayoutTerminal.close();
      Navigation.findNavController(binding.getRoot()).navigate(
              R.id.action_terminalFragment_to_settingsFragment);
    }
    return false;
  }

  private void showAttackTypeDialogSelector(boolean automaticAttack) {
    String[] choices;
    if (automaticAttack) {
      choices = getResources().getStringArray(R.array.dialog_options_attack_type_terminal_auto);
    } else {
      choices = getResources().getStringArray(R.array.dialog_options_attack_type_terminal_manual);
    }

    final int[] checkedItem = {-1};

    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());
    builder.setTitle("Select attack type");
    builder.setPositiveButton("SELECT", (dialog, which) -> {
      if (checkedItem[0] == -1) {
        return;
      }
      checkedItem[0] = -1;
      if (automaticAttack) {
        navigateToAutoArmaFragment();
      } else {
        navigateToManualArmaFragment();
      }
    });
    builder.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
    builder.setSingleChoiceItems(choices, checkedItem[0], (dialog, which) -> {
      checkedItem[0] = which;
      selectedArmament = choices[which];
    }).show();
  }

  private void navigateToAutoArmaFragment() {
    Log.d("dev-log", "TerminalFragment.showAttackTypeDialogSelector: " +
            "Navigating to auto arma fragment");

    AutoArmaArgs autoArmaArgs = new AutoArmaArgs(
            terminalArgs.getDeviceId(),
            terminalArgs.getPortNum(),
            terminalArgs.getBaudRate(),
            selectedArmament);

    TerminalFragmentDirections.ActionTerminalFragmentToAutoArmaFragment action;
    action = TerminalFragmentDirections.actionTerminalFragmentToAutoArmaFragment(
            autoArmaArgs);
    Navigation.findNavController(binding.getRoot()).navigate(action);
  }

  private void navigateToManualArmaFragment() {
    Log.d("dev-log", "TerminalFragment.showAttackTypeDialogSelector: " +
            "Navigating to manual arma fragment");
    ManualArmaArgs manualArmaArgs = new ManualArmaArgs(
            terminalArgs.getDeviceId(),
            terminalArgs.getPortNum(),
            terminalArgs.getBaudRate(),
            selectedArmament);

    TerminalFragmentDirections.ActionTerminalFragmentToManualArmaFragment action;
    action = TerminalFragmentDirections.actionTerminalFragmentToManualArmaFragment(
            manualArmaArgs);
    Navigation.findNavController(binding.getRoot()).navigate(action);
  }
}