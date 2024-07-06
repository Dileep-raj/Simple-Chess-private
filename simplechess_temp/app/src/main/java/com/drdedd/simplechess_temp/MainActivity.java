package com.drdedd.simplechess_temp;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.drdedd.simplechess_temp.GameData.DataManager;
import com.drdedd.simplechess_temp.databinding.ActivityMainBinding;
import com.drdedd.simplechess_temp.fragments.LoadGameFragment;
import com.google.android.material.navigation.NavigationView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@RequiresApi(api = Build.VERSION_CODES.N)
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private AppBarConfiguration mAppBarConfiguration;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());

        Window window = getWindow();
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        DataManager dataManager = new DataManager(this);
        if (dataManager.isFullScreen()) {
            EdgeToEdge.enable(this);
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        }

        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolBar);

        DrawerLayout drawerLayout = binding.MainView;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_home).setOpenableLayout(drawerLayout).build();
        navController = Navigation.findNavController(this, R.id.main_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        Intent intent = getIntent();
        String action = intent.getAction(), type = intent.getType(), scheme = intent.getScheme();
        if (action != null && !action.equals(Intent.ACTION_MAIN))
            Log.d(TAG, String.format("onCreate:%nAction: %s%nType: %s%nScheme: %s", action, type, scheme));

        if (action != null && action.equals(Intent.ACTION_SEND) && type != null) {
            if (type.startsWith("text/")) {
                String content = intent.getStringExtra(Intent.EXTRA_TEXT);
                Log.d(TAG, "onCreate: Extra text:%n" + content);
                if (content != null && !content.isEmpty()) loadGame(content);
            }
            if (type.startsWith("application/")) {
                Uri textURI = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                readUri(textURI);
            }
        }

        if (action != null && action.equals(Intent.ACTION_VIEW)) {
            Uri uri = intent.getData();
            readUri(uri);
        }
    }

    /**
     * Read PGN using URI
     *
     * @param uri URI of the PGN file
     */
    private void readUri(Uri uri) {
        if (uri != null) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                BufferedReader bf = new BufferedReader(new InputStreamReader(inputStream));
                while ((line = bf.readLine()) != null) stringBuilder.append(line).append('\n');
                Log.d(TAG, "readUri: Content: " + stringBuilder);

                loadGame(stringBuilder.toString());
            } catch (IOException e) {
                Log.e(TAG, "readUri: Error while reading uri file", e);
            }
        }
    }

    /**
     * Load game from the PGN content
     *
     * @param content Shared text content
     */
    private void loadGame(String content) {
        Bundle args = new Bundle();
        args.putString(LoadGameFragment.PGN_CONTENT_KEY, content);
        args.putBoolean(LoadGameFragment.FILE_EXISTS_KEY, false);
        navController.popBackStack();
        navController.navigate(R.id.nav_load_game, args);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.main_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }
}