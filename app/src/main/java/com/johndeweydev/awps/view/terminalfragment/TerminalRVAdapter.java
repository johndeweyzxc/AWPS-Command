package com.johndeweydev.awps.view.terminalfragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.johndeweydev.awps.R;
import com.johndeweydev.awps.model.data.LauncherOutputData;

import java.util.ArrayList;

public class TerminalRVAdapter extends
        RecyclerView.Adapter<TerminalRVAdapter.TerminalAdapterViewHolder> {

  private final ArrayList<LauncherOutputData> terminalLog = new ArrayList<>();

  public static class TerminalAdapterViewHolder extends RecyclerView.ViewHolder {
    public TextView textViewRvTerminalLogTime;
    public TextView textViewRvTerminalLogOutput;

    public TerminalAdapterViewHolder(@NonNull View itemView) {
      super(itemView);
      textViewRvTerminalLogTime = itemView.findViewById(R.id.textViewTimeTerminalLogItem);
      textViewRvTerminalLogOutput = itemView.findViewById(R.id.textViewOutputTerminalLogItem);
    }
  }

  @NonNull
  @Override
  public TerminalAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(parent.getContext()).inflate(
            R.layout.rv_terminal_log_item, parent, false);
    return new TerminalAdapterViewHolder(v);
  }

  @Override
  public void onBindViewHolder(@NonNull TerminalAdapterViewHolder holder, int position) {
    LauncherOutputData launcherOutputData = terminalLog.get(position);
    holder.textViewRvTerminalLogTime.setText(launcherOutputData.getTime());
    holder.textViewRvTerminalLogOutput.setText(launcherOutputData.getOutput());
  }

  @Override
  public int getItemCount() {
    return terminalLog.size();
  }

  public void appendNewTerminalLog(LauncherOutputData launcherOutputData) {
    terminalLog.add(launcherOutputData);
    notifyItemInserted(terminalLog.size() - 1);
  }

  public void clearLogs() {
    int size = terminalLog.size();
    terminalLog.clear();
    notifyItemRangeRemoved(0, size);
  }
}
