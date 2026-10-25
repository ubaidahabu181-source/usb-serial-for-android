
package de.kai_morich.simple_usb_terminal;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.Arrays;

public class TerminalFragment extends Fragment implements SerialListener {

    private final int[] RPM_GRID = {500,700,900,1100,1300,1500,1800,2200,2600,3000,3500,4000,4500,5000,5750,6500};
    private final int[] MAP_GRID = {20,25,30,35,40,45,50,55,60,65,70,75,80,85,92,100};
    
    private TextView receiveText, rpmValue, mapValue, advValue, statusText, limiterText, dfcoText, liveCellInfo;
    private ProgressBar rpmBar;
    private EditText sendText;
    private LinearLayout mapTableContainer;
    private Button btnGet, btnSave, btnLoad, sendBtn;
    
    private int[][] advMap = new int[16][16];
    private int currentRpm = 0, currentMap = 45, currentAdv = 16;
    private String buffer = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        receiveText = view.findViewById(R.id.receive_text);
        rpmValue = view.findViewById(R.id.rpmValue);
        mapValue = view.findViewById(R.id.mapValue);
        advValue = view.findViewById(R.id.advValue);
        statusText = view.findViewById(R.id.statusText);
        limiterText = view.findViewById(R.id.limiterText);
        dfcoText = view.findViewById(R.id.dfcoText);
        liveCellInfo = view.findViewById(R.id.liveCellInfo);
        rpmBar = view.findViewById(R.id.rpmBar);
        sendText = view.findViewById(R.id.send_text);
        sendBtn = view.findViewById(R.id.send_btn);
        btnGet = view.findViewById(R.id.btnGet);
        btnSave = view.findViewById(R.id.btnSave);
        btnLoad = view.findViewById(R.id.btnLoad);
        mapTableContainer = view.findViewById(R.id.mapTableContainer);

        // Default map from V7.4 BOS
        int[][] defaultMap = {
          {16,16,19,25,28,30,32,33,34,33,32,30,29,27,25,24},
          {15,15,18,24,27,28,30,32,32,32,30,29,28,26,24,23},
          {14,14,17,22,26,27,29,30,31,30,29,28,27,25,23,22},
          {14,14,16,21,24,26,28,29,30,29,28,27,26,24,22,21},
          {13,13,15,20,23,24,26,28,28,28,27,26,25,24,22,20},
          {13,13,15,19,22,23,25,26,27,26,26,25,24,23,21,19},
          {13,12,14,18,20,22,24,25,26,25,24,24,23,22,20,19},
          {12,12,13,17,19,20,22,23,24,24,23,23,22,22,20,18},
          {12,11,13,16,18,19,20,22,22,22,22,22,22,21,20,18},
          {12,11,12,15,16,17,19,20,21,21,21,21,21,21,19,18},
          {11,11,12,14,15,16,17,19,19,20,20,20,20,20,19,17},
          {11,10,11,13,14,15,16,17,18,19,19,19,19,19,18,16},
          {11,10,11,12,13,14,15,16,17,17,18,19,19,19,17,16},
          {11,10,10,12,13,13,14,15,16,17,17,18,18,18,17,15},
          {10,9,10,11,12,12,13,14,15,16,16,17,17,17,16,15},
          {10,9,9,10,11,11,12,13,14,14,15,16,16,17,15,14}
        };
        for(int i=0;i<16;i++) advMap[i]=Arrays.copyOf(defaultMap[i],16);
        
        buildMapTable();

        btnGet.setOnClickListener(v -> send("$GET\n"));
        btnSave.setOnClickListener(v -> send("$SAVE\n"));
        btnLoad.setOnClickListener(v -> send("$LOAD\n"));
        sendBtn.setOnClickListener(v -> {
            String cmd = sendText.getText().toString();
            if(!cmd.endsWith("\n")) cmd+="\n";
            send(cmd);
        });

        return view;
    }

    private void buildMapTable() {
        mapTableContainer.removeAllViews();
        // Header RPM
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        TextView corner = new TextView(getContext()); corner.setText("MAP\RPM "); corner.setWidth(160); corner.setTextColor(Color.GRAY); corner.setTextSize(9);
        header.addView(corner);
        for(int c=0;c<16;c++){ TextView tv=new TextView(getContext()); tv.setText(String.valueOf(RPM_GRID[c])); tv.setWidth(90); tv.setTextColor(Color.GRAY); tv.setTextSize(9); header.addView(tv);}
        mapTableContainer.addView(header);

        for(int r=0;r<16;r++){
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            TextView label = new TextView(getContext()); label.setText(MAP_GRID[r]+"kPa "); label.setWidth(160); label.setTextColor(Color.CYAN); label.setTextSize(10);
            row.addView(label);
            for(int c=0;c<16;c++){
                TextView cell = new TextView(getContext());
                cell.setText(String.valueOf(advMap[r][c]));
                cell.setWidth(90); cell.setHeight(70);
                cell.setTextColor(Color.WHITE); cell.setTextSize(11);
                cell.setGravity(17); cell.setPadding(4,4,4,4);
                int val = advMap[r][c];
                if(val<13) cell.setBackgroundColor(Color.parseColor("#1A3A2A"));
                else if(val<20) cell.setBackgroundColor(Color.parseColor("#2A5A1A"));
                else if(val<27) cell.setBackgroundColor(Color.parseColor("#8A6A00"));
                else cell.setBackgroundColor(Color.parseColor("#8A2A00"));
                cell.setTag(r+","+c);
                final int fr=r, fc=c;
                cell.setOnClickListener(v -> showEditDialog(fr,fc));
                row.addView(cell);
            }
            mapTableContainer.addView(row);
        }
    }

    private void showEditDialog(int r, int c){
        EditText input = new EditText(getContext());
        input.setText(String.valueOf(advMap[r][c]));
        input.setInputType(2);
        new AlertDialog.Builder(getContext())
            .setTitle("Edit ADV ["+MAP_GRID[r]+"kPa , "+RPM_GRID[c]+"RPM] REAL DEG")
            .setView(input)
            .setPositiveButton("SET", (d,w)->{
                try{
                    int v=Integer.parseInt(input.getText().toString());
                    if(v>=0&&v<=50){
                        advMap[r][c]=v;
                        buildMapTable();
                        send("$SET,"+r+","+c+","+v+"\n");
                    }
                }catch(Exception e){}
            }).setNegativeButton("Batal",null).show();
    }

    private void send(String s){
        try{
            MainActivity activity = (MainActivity)getActivity();
            if(activity!=null && activity.serialService!=null){
                activity.serialService.write(s.getBytes());
                receiveText.append("[TX] "+s);
            }
        }catch(Exception e){ Toast.makeText(getContext(),"Not connected",Toast.LENGTH_SHORT).show();}
    }

    @Override
    public void onSerialRead(byte[] data){
        String chunk = new String(data);
        buffer+=chunk;
        // Process line by line
        int idx;
        while((idx=buffer.indexOf("\n"))>=0 || (idx=buffer.indexOf("*\n"))>=0 || buffer.contains("*")){
            String line;
            if(buffer.contains("\n")){
                line=buffer.substring(0,buffer.indexOf("\n")+1);
                buffer=buffer.substring(buffer.indexOf("\n")+1);
            } else {
                line=buffer; buffer="";
            }
            line=line.trim();
            if(line.isEmpty()) continue;
            String finalLine=line;
            if(getActivity()!=null) getActivity().runOnUiThread(()->handleLine(finalLine));
        }
    }

    private void handleLine(String line){
        receiveText.append(line+"\n");
        // Auto scroll
        // Keep only last 5000 chars
        if(receiveText.getText().length()>8000) receiveText.setText(receiveText.getText().subSequence(receiveText.getText().length()-6000, receiveText.getText().length()));

        if(line.startsWith("$OK,RPM,")){
            try{
                // $OK,RPM,1240,MAP,45,ADV,19*
                line=line.replace("$OK,","").replace("*","");
                String[] p=line.split(",");
                for(int i=0;i<p.length-1;i++){
                    if(p[i].equals("RPM")) currentRpm=Integer.parseInt(p[i+1]);
                    if(p[i].equals("MAP")) currentMap=Integer.parseInt(p[i+1]);
                    if(p[i].equals("ADV")) currentAdv=Integer.parseInt(p[i+1]);
                }
                rpmValue.setText(String.valueOf(currentRpm));
                rpmBar.setProgress(Math.min(currentRpm,6500));
                mapValue.setText(currentMap+" kPa");
                advValue.setText(currentAdv+"°");
                statusText.setText("CONNECTED"); statusText.setTextColor(Color.parseColor("#00FF88"));

                // Limiter logic display
                if(currentRpm>=6500) limiterText.setText("LIMITER: ACTIVE!"); else if(currentRpm>=6200) limiterText.setText("LIMIT: CUTBACK");
                else limiterText.setText("LIMIT: NORMAL 6500");

                if(currentMap<30 && currentRpm>1600) dfcoText.setText("DFCO: ON"); else dfcoText.setText("DFCO: OFF");

                // Find live cell
                int rIdx=0,cIdx=0;
                for(int i=0;i<15;i++) if(currentMap>=MAP_GRID[i] && currentMap<MAP_GRID[i+1]) {rIdx=i; break;}
                for(int i=0;i<15;i++) if(currentRpm>=RPM_GRID[i] && currentRpm<RPM_GRID[i+1]) {cIdx=i; break;}
                liveCellInfo.setText("Live Cell: RPM "+RPM_GRID[cIdx]+" / MAP "+MAP_GRID[rIdx]+" kPa -> Row "+rIdx+" Col "+cIdx+" = "+advMap[rIdx][cIdx]+"° | Real ADV: "+currentAdv+"°");

            }catch(Exception e){}
        } else if(line.startsWith("$MAPR,")){
            try{
                line=line.replace("*","").replace("$MAPR,","");
                String[] p=line.split(",");
                int row=Integer.parseInt(p[0]);
                for(int c=0;c<16 && c+1<p.length;c++){
                    advMap[row][c]=Integer.parseInt(p[c+1]);
                }
                buildMapTable();
            }catch(Exception e){}
        } else if(line.contains("SAVE_DONE")){
            Toast.makeText(getContext(),"Map Saved to EEPROM!",Toast.LENGTH_SHORT).show();
        }
    }

    @Override public void onSerialIoError(Exception e){ if(getActivity()!=null) getActivity().runOnUiThread(()->{ statusText.setText("IO ERROR"); statusText.setTextColor(Color.RED);});}
    @Override public void onSerialConnect(){ if(getActivity()!=null) getActivity().runOnUiThread(()->{ statusText.setText("CONNECTED"); statusText.setTextColor(Color.parseColor("#00FF88"));});}
    @Override public void onSerialConnectError(Exception e){ statusText.setText("CONNECT ERR");}
    @Override public void onSerialDisconnect(){ statusText.setText("DISCONNECTED");}
}
