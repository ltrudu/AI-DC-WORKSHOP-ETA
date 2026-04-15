package zebra.BarcodeDetector;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import zebra.BarcodeDetector.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private ActivitySettingsBinding binding;

    public void onClickbtnCANCELLIST(View view) {
        binding.tvBARCODESTOHIGHLIGHT.setText("");
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences("SettingsPreferences", MODE_PRIVATE);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        loadSettings();

        ToggleButton toggle = binding.toggleAnalyzerMode;
        boolean current = sharedPreferences.getBoolean("IS_ZEBRA_MODE", false);
        toggle.setChecked(current);

        toggle.setOnCheckedChangeListener((buttonView, isChecked) ->
                sharedPreferences.edit().putBoolean("IS_ZEBRA_MODE", isChecked).apply());
    }

    public void onClickbtn_SUBMIT(View view) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("IMAGESIZE", binding.spinnerIMAGESIZE.getSelectedItemPosition());
        editor.putInt("SCENE", binding.spinnerSCENE.getSelectedItemPosition());
        editor.putInt("ZOOM", binding.spinnerZOOM.getSelectedItemPosition());
        editor.putString("BARCODESTOHIGHLIGHT", binding.tvBARCODESTOHIGHLIGHT.getText().toString());
        editor.apply();
        finish();
    }

    private void loadSettings() {
        int imageSize = sharedPreferences.getInt("IMAGESIZE", 1);
        int scene = sharedPreferences.getInt("SCENE", 0);
        int zoom = sharedPreferences.getInt("ZOOM", 0);
        String barcodesToHighlight = sharedPreferences.getString("BARCODESTOHIGHLIGHT", "");

        binding.spinnerIMAGESIZE.setSelection(imageSize);
        binding.spinnerSCENE.setSelection(scene);
        binding.spinnerZOOM.setSelection(zoom);
        binding.tvBARCODESTOHIGHLIGHT.setText(barcodesToHighlight);
    }
}
