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
package com.gluonhq.attach.pushnotifications.impl;

import com.gluonhq.attach.pushnotifications.PushNotificationsService;
import com.gluonhq.attach.pushnotifications.impl.gms.GoogleServicesConfiguration;
import com.gluonhq.attach.util.Constants;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Alert;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class AndroidPushNotificationsService implements PushNotificationsService {

    private static Logger LOG = Logger.getLogger(AndroidPushNotificationsService.class.getName());

    static {
        System.loadLibrary("PushNotifications");
    }

    /**
     * A string property to wrap the token device when received from the native layer
     */
    private static final ReadOnlyStringWrapper TOKEN = new ReadOnlyStringWrapper();

    public AndroidPushNotificationsService() {
        if (Boolean.getBoolean(Constants.ATTACH_DEBUG)) {
            enableDebug();
        }
    }

    @Override
    public ReadOnlyStringProperty tokenProperty() {
        return TOKEN.getReadOnlyProperty();
    }

    @Override
    public void register() {
        int resultCode = isGooglePlayServicesAvailable();
        if (resultCode == 0) { // ConnectionResult.SUCCESS
            GoogleServicesConfiguration configuration = readGoogleServicesConfiguration();

            initializeFirebase(configuration.getApplicationId(), configuration.getProjectNumber());
        } else {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Google Play Services Error:\n" +
                        getErrorString(resultCode) +
                        "\n\nPush notifications won't work until this error is fixed");
                alert.showAndWait();
            });
        }
    }

    private GoogleServicesConfiguration readGoogleServicesConfiguration() {
        GoogleServicesConfiguration configuration = new GoogleServicesConfiguration();

        try (JsonReader reader = Json.createReader(AndroidPushNotificationsService.class.getResourceAsStream("/google-services.json"))) {
            JsonObject json = reader.readObject();

            JsonObject projectInfo = json.getJsonObject("project_info");
            if (projectInfo != null) {
                configuration.setProjectNumber(projectInfo.getString("project_number", null));
            }

            String packageName = getPackageName();
            JsonArray clients = json.getJsonArray("client");
            if (clients != null) {
                for (int i = 0; i < clients.size(); i++) {
                    JsonObject client = clients.getJsonObject(i);
                    JsonObject clientInfo = client.getJsonObject("client_info");
                    if (clientInfo != null) {
                        JsonObject androidClientInfo = clientInfo.getJsonObject("android_client_info");
                        if (androidClientInfo != null) {
                            String clientPackageName = androidClientInfo.getString("package_name", "");
                            if (packageName.equals(clientPackageName)) {
                                configuration.setApplicationId(clientInfo.getString("mobilesdk_app_id", null));
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to read google-services.json. Make sure to add the file to the folder: src/android/resources", e);
        }

        return configuration;
    }

    private native void enableDebug();
    private native String getPackageName();
    private native int isGooglePlayServicesAvailable();
    private native String getErrorString(int resultCode);
    private native void initializeFirebase(String applicationId, String projectNumber);
}