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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.dependencytrack.model.ScheduledNotificationsInfo;

import com.asahaf.javacron.InvalidExpressionException;
import com.asahaf.javacron.Schedule;

import alpine.notification.NotificationLevel;

public class ScheduledNotification {
    private String scope;
    private String group;
    private NotificationLevel level;
    private String title;
    private String content;
    private LocalDateTime timestamp;
    private List<Object> subjects;

    public void sendScheduledNotification(ScheduledNotificationsInfo scheduledNotificationsInfo) {
        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        long millis = scheduledNotificationsInfo.getNextExecution().getTime() - System.currentTimeMillis();
        // TODO is schedulednotification still in database? If not stop service
        Schedule schedule;
        try {
            schedule = Schedule.create(scheduledNotificationsInfo.getCronString());
            Date[] nextRuns = schedule.next(scheduledNotificationsInfo.getCreated(), 2);
            long period = nextRuns[1].getTime() - nextRuns[0].getTime();
            service.scheduleAtFixedRate(new ScheduledNotificationSenderTask(scheduledNotificationsInfo, service), millis, period, TimeUnit.MILLISECONDS);
        } catch (InvalidExpressionException | RejectedExecutionException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void addScheduledNotification(String cronString) throws InvalidExpressionException, ParseException {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("y-MM-dd HH:mm:ss");
        Date baseDate = dateFormatter.parse("2019-01-01 04:04:02");
        Schedule schedule1 = Schedule.create(cronString);
        System.out.println(dateFormatter.format(schedule1.next(baseDate)));
    }

    public String getScope() {
        return this.scope;
    }

    public String getGroup() {
        return this.group;
    }

    public NotificationLevel getNotificationLevel() {
        return this.level;
    }

    public String getTitle() {
        return this.title;
    }

    public String getContent() {
        return this.content;
    }

    public LocalDateTime getTimestamp() {
        return this.timestamp;
    }

    public List<Object> getSubjects() {
        return this.subjects;
    }

}
