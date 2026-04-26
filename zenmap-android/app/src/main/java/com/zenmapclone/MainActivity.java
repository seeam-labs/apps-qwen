package com.zenmapclone;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText targetInput;
    private Button scanButton;
    private TextView resultsText;
    private ProgressBar progressBar;
    private ExecutorService executor;

    // Common ports to scan (similar to Nmap's top ports)
    private final int[] COMMON_PORTS = {
        21, 22, 23, 25, 53, 80, 110, 143, 443, 993, 995, 3306, 3389, 5432, 8080
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        targetInput = findViewById(R.id.targetInput);
        scanButton = findViewById(R.id.scanButton);
        resultsText = findViewById(R.id.resultsText);
        progressBar = findViewById(R.id.progressBar);

        executor = Executors.newFixedThreadPool(10);

        scanButton.setOnClickListener(v -> startScan());
    }

    private void startScan() {
        String target = targetInput.getText().toString().trim();
        
        if (target.isEmpty()) {
            Toast.makeText(this, "Please enter a target IP or hostname", Toast.LENGTH_SHORT).show();
            return;
        }

        resultsText.setText("Starting scan on " + target + "...\n\n");
        progressBar.setVisibility(View.VISIBLE);
        scanButton.setEnabled(false);

        executor.execute(() -> {
            try {
                performScan(target);
            } catch (Exception e) {
                runOnUiThread(() -> {
                    resultsText.append("Error: " + e.getMessage() + "\n");
                    progressBar.setVisibility(View.GONE);
                    scanButton.setEnabled(true);
                });
            }
        });
    }

    private void performScan(String target) {
        List<String> openPorts = new ArrayList<>();
        
        // First, try to resolve the hostname and check if host is up
        InetAddress address = null;
        try {
            address = InetAddress.getByName(target);
            String resolvedIP = address.getHostAddress();
            
            runOnUiThread(() -> {
                resultsText.append("Host: " + target + " (" + resolvedIP + ")\n");
                resultsText.append("Scanning " + COMMON_PORTS.length + " common ports...\n\n");
            });
            
            // Scan common ports
            for (int port : COMMON_PORTS) {
                if (isPortOpen(address, port)) {
                    openPorts.add(port + "");
                    String serviceName = getServiceName(port);
                    
                    runOnUiThread(() -> {
                        resultsText.append("Port " + port + "/tcp OPEN (" + serviceName + ")\n");
                    });
                }
            }
            
            runOnUiThread(() -> {
                resultsText.append("\nScan complete!\n");
                resultsText.append("Open ports found: " + openPorts.size() + "\n");
                progressBar.setVisibility(View.GONE);
                scanButton.setEnabled(true);
            });
            
        } catch (IOException e) {
            runOnUiThread(() -> {
                resultsText.append("Error resolving host: " + e.getMessage() + "\n");
                progressBar.setVisibility(View.GONE);
                scanButton.setEnabled(true);
            });
        }
    }

    private boolean isPortOpen(InetAddress address, int port) {
        try {
            Socket socket = new Socket();
            socket.connect(address, port, 3000); // 3 second timeout
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private String getServiceName(int port) {
        switch (port) {
            case 21: return "FTP";
            case 22: return "SSH";
            case 23: return "Telnet";
            case 25: return "SMTP";
            case 53: return "DNS";
            case 80: return "HTTP";
            case 110: return "POP3";
            case 143: return "IMAP";
            case 443: return "HTTPS";
            case 993: return "IMAPS";
            case 995: return "POP3S";
            case 3306: return "MySQL";
            case 3389: return "RDP";
            case 5432: return "PostgreSQL";
            case 8080: return "HTTP-Proxy";
            default: return "unknown";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
