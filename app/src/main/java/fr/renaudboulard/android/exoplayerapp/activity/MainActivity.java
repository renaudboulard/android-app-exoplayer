package fr.renaudboulard.android.exoplayerapp.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import fr.renaudboulard.android.exoplayerapp.R;
import fr.renaudboulard.android.exoplayerapp.service.StreamService;


public class MainActivity extends AppCompatActivity implements StreamService.Callbacks{

    private StreamService streamService;
    private Intent playMusicIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        playMusicIntent = new Intent(this, StreamService.class);
        playMusicIntent.putExtra("URL", "http://dir.xiph.org/listen/2212218/listen.m3u");
        startService(playMusicIntent);
        bindService(playMusicIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
        stopService(playMusicIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Toast.makeText(MainActivity.this, "onServiceConnected called", Toast.LENGTH_SHORT).show();
            // We've binded to LocalService, cast the IBinder and get LocalService instance
            StreamService.LocalBinder binder = (StreamService.LocalBinder) service;
            streamService = binder.getServiceInstance(); //Get instance of your service!
            streamService.registerClient(MainActivity.this); //Activity register in the service as client for callabcks!
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Toast.makeText(MainActivity.this, "onServiceDisconnected called", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public void updateTitle(String title) {

    }
}
