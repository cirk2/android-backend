/*
 * Copyright 2019 Cyface GmbH
 * This file is part of the Cyface SDK for Android.
 * The Cyface SDK for Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * The Cyface SDK for Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with the Cyface SDK for Android. If not, see <http://www.gnu.org/licenses/>.
 */
package de.cyface.datacapturing;

import android.os.Parcelable;

import androidx.annotation.NonNull;
import de.cyface.datacapturing.backend.DataCapturingBackgroundService;
import de.cyface.persistence.model.GeoLocation;
import de.cyface.persistence.model.Measurement;

/**
 * Interface for strategies to respond to {@link DataCapturingBackgroundService#onLocationCaptured(GeoLocation)} events
 * to calculate the {@link Measurement#distance} from {@link GeoLocation} pairs.
 * <p>
 * Must be {@code Parcelable} to be passed from the {@link DataCapturingService} via {@code Intent}.
 *
 * @author Armin Schnabel
 * @author Klemens Muthmann
 * @version 2.0.0
 * @since 2.5.0
 */
public interface DistanceCalculationStrategy extends Parcelable {

    /**
     * Implements a strategy to calculate the {@link Measurement#distance} based on two subsequent {@link GeoLocation}s.
     *
     * @param lastLocation The {@code GeoLocation} captured before {@param newLocation}
     * @param newLocation The {@code GeoLocation} captured after {@param lastLocation}
     * @return The distance which is added to the {@code Measurement} based on the provided {@code GeoLocation}s.
     */
    double calculateDistance(@NonNull final GeoLocation lastLocation, @NonNull final GeoLocation newLocation);
}
