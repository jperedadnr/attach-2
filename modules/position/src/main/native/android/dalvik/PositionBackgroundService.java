/*
 * Copyright (c) 2020, Gluon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL GLUON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gluonhq.helloandroid;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import static com.gluonhq.helloandroid.DalvikPositionService.ALTITUDE;
import static com.gluonhq.helloandroid.DalvikPositionService.LATITUDE;
import static com.gluonhq.helloandroid.DalvikPositionService.LONGITUDE;

/**
 *
 * @since 3.8.0
 */
public class PositionBackgroundService extends Service implements LocationListener {

    private static final String TAG = "GluonAttach";

    private final Intent intent;
    private LocationManager locationManager;

    public PositionBackgroundService() {
        intent = new Intent(PositionBackgroundService.class.getName());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.v(TAG, "Start Background service");
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.v(TAG, "Initialize PositionBackgroundService");
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        String provider;
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            provider = LocationManager.NETWORK_PROVIDER;
        } else if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
            provider = LocationManager.PASSIVE_PROVIDER;
        } else {
            Log.v(TAG, "No LocationProvider found in PositionBackgroundService");
            return;
        }

        locationManager.requestLocationUpdates(provider, 5000, 15.0f, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Finalize PositionBackgroundService");
        locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.v(TAG, String.format("New Position in Background %3.6f,%3.6f,%3.6f",
                location.getLatitude(), location.getLongitude(), location.getAltitude()));
        intent.putExtra(LATITUDE, String.valueOf(location.getLatitude()));
        intent.putExtra(LONGITUDE, String.valueOf(location.getLongitude()));
        intent.putExtra(ALTITUDE, String.valueOf(location.getAltitude()));
        sendBroadcast(intent);
    }

    @Override
    public void onStatusChanged(String string, int i, Bundle bundle) {
        Log.v(TAG, "STATUS: " + string + " " + bundle);
    }

    @Override
    public void onProviderEnabled(String string) {
    }

    @Override
    public void onProviderDisabled(String string) {
    }

}