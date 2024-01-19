/*
 * This file is part of Dependency-Track.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) Steve Springett. All Rights Reserved.
 */
package org.dependencytrack.notification;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.dependencytrack.model.ScheduledNotificationsInfo;

import com.asahaf.javacron.InvalidExpressionException;
import com.asahaf.javacron.Schedule;

import alpine.common.logging.Logger;


public class ScheduledNotification {
    private static final Logger LOGGER = Logger.getLogger(ScheduledNotification.class);
    public ScheduledNotification(){}

    public void sendScheduledNotification(ScheduledNotificationsInfo scheduledNotificationsInfo) {
        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        long millis = scheduledNotificationsInfo.getNextExecution().getTime() - System.currentTimeMillis();
        Schedule schedule;
        try {
            schedule = Schedule.create(scheduledNotificationsInfo.getCronString());
            Date[] nextRuns = schedule.next(scheduledNotificationsInfo.getCreated(), 2);
            long period = nextRuns[1].getTime() - nextRuns[0].getTime();
            service.scheduleAtFixedRate(new ScheduledNotificationSenderTask(scheduledNotificationsInfo, service), millis, period, TimeUnit.MILLISECONDS);
        } catch (InvalidExpressionException | RejectedExecutionException | NullPointerException e) {
            LOGGER.debug(e.getMessage());
        }
    }
}
