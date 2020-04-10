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
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Alert;

public class AndroidPushNotificationsService implements PushNotificationsService {

    /**
     * A string property to wrap the token device when received from the native layer
     */
    private static final ReadOnlyStringWrapper TOKEN = new ReadOnlyStringWrapper();

    @Override
    public ReadOnlyStringProperty tokenProperty() {
        return TOKEN.getReadOnlyProperty();
    }

    @Override
    public void register() {
        int resultCode = isGooglePlayServicesAvailable();
        if (resultCode == 0) { // ConnectionResult.SUCCESS

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

    private static native void enableDebug();
    private static native int isGooglePlayServicesAvailable();
    private static native String getErrorString(int resultCode);
    private static native void initializeFirebase(String applicationId, String projectNumber);
}