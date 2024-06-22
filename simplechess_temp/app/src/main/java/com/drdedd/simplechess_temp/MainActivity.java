package com.drdedd.simplechess_temp;

import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

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
import com.google.android.material.navigation.NavigationView;

@RequiresApi(api = Build.VERSION_CODES.N)
public class MainActivity extends AppCompatActivity {
    //    private static final String TAG = "MainActivity";
    private AppBarConfiguration mAppBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());

        Window window = getWindow();
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        DataManager dataManager = new DataManager(this);
        if (dataManager.isFullScreen())
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolBar);

        DrawerLayout drawerLayout = binding.MainView;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_home).setOpenableLayout(drawerLayout).build();
        NavController navController = Navigation.findNavController(this, R.id.main_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

//        Intent intent = getIntent();
//        String action = intent.getAction(), type = intent.getType();
//
//        if (action != null && action.equals(Intent.ACTION_SEND)) {
//            if (type != null && type.equals("text/plain")) {
//                Log.d(TAG, "onCreate: Data " + intent.getData());
//                Log.d(TAG, "onCreate: Extra text " + intent.getStringExtra(Intent.EXTRA_TEXT));
//                Uri textURI = intent.getParcelableExtra(Intent.EXTRA_STREAM);
//                if (textURI != null)
//                    Log.d(TAG, String.format("onCreate: URI: %s\n%s", textURI.getPath(), textURI));
//            }
//        }
//
//        Uri uri = intent.getData();
//        if (uri != null) Log.d(TAG, "onCreate: URI2: " + Uri.parse(uri.toString()));
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.main_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }
}