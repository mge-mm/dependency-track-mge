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

import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import javax.json.Json;
import javax.json.JsonObject;

import org.dependencytrack.model.NotificationPublisher;
//import org.dependencytrack.model.PolicyViolation;
import org.dependencytrack.model.ScheduledNotificationsInfo;
import org.dependencytrack.model.Vulnerability;
import org.dependencytrack.persistence.QueryManager;

import alpine.security.crypto.DataEncryption;
import alpine.server.mail.SendMail;
import alpine.server.mail.SendMailException;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import static org.dependencytrack.model.ConfigPropertyConstants.EMAIL_SMTP_ENABLED;
import static org.dependencytrack.model.ConfigPropertyConstants.EMAIL_SMTP_FROM_ADDR;
import static org.dependencytrack.model.ConfigPropertyConstants.EMAIL_SMTP_PASSWORD;
import static org.dependencytrack.model.ConfigPropertyConstants.EMAIL_SMTP_SERVER_HOSTNAME;
import static org.dependencytrack.model.ConfigPropertyConstants.EMAIL_SMTP_SERVER_PORT;
import static org.dependencytrack.model.ConfigPropertyConstants.EMAIL_SMTP_SSLTLS;
import static org.dependencytrack.model.ConfigPropertyConstants.EMAIL_SMTP_TRUSTCERT;
import static org.dependencytrack.model.ConfigPropertyConstants.EMAIL_SMTP_USERNAME;
import static org.dependencytrack.notification.publisher.Publisher.CONFIG_TEMPLATE_KEY;
import static org.dependencytrack.notification.publisher.Publisher.CONFIG_TEMPLATE_MIME_TYPE_KEY;

public class ScheduledNotificationSenderTask implements Runnable {
    private ScheduledNotificationsInfo scheduledNotificationsInfo;
    private ScheduledExecutorService service;

    public ScheduledNotificationSenderTask(ScheduledNotificationsInfo scheduledNotificationsInfo,
            ScheduledExecutorService service) {
        this.scheduledNotificationsInfo = scheduledNotificationsInfo;
        this.service = service;
    }

    @Override
    public void run() {
        String content = "SN:  ";
        final String mimeType;
        final boolean smtpEnabled;
        final String smtpFrom;
        final String smtpHostname;
        final int smtpPort;
        final String smtpUser;
        final String encryptedSmtpPassword;
        final boolean smtpSslTls;
        final boolean smtpTrustCert;
        List<Vulnerability> newVulnerabilities;

        try (QueryManager qm = new QueryManager()) {
            scheduledNotificationsInfo = qm.getScheduledNotificationsInfoById(scheduledNotificationsInfo.getid());
            if (scheduledNotificationsInfo == null) {
                System.out.println("thread running");
                service.shutdown();
            } else {
                if (scheduledNotificationsInfo.getLastExecution().equals(scheduledNotificationsInfo.getCreated())) {
                    System.out.println("schedulednotification just created. No Information to show");
                } else {
                    SimpleDateFormat dateFormatter = new SimpleDateFormat("y-MM-dd HH:mm:ss");
                    Date dummyDateForTesting = dateFormatter.parse("2023-12-30 00:00:00");
                    newVulnerabilities = qm.getNewVulnerabilitiesSinceTimestamp(dummyDateForTesting, 1 /*scheduledNotificationsInfo.getLastExecution()*/);
                    //List<PolicyViolation> newPolicyViolations = qm.getNewPolicyViolationsSinceTimestamp(scheduledNotificationsInfo.getLastExecution());

                    NotificationPublisher notificationPublisher = qm.getNotificationPublisher("Scheduled Email");

                    JsonObject notificationPublisherConfig = Json.createObjectBuilder()
                            .add(CONFIG_TEMPLATE_MIME_TYPE_KEY, notificationPublisher.getTemplateMimeType())
                            .add(CONFIG_TEMPLATE_KEY, notificationPublisher.getTemplate())
                            .build();

                    PebbleEngine pebbleEngine = new PebbleEngine.Builder().build();
                    String literalTemplate = notificationPublisherConfig.getString(CONFIG_TEMPLATE_KEY);
                    final PebbleTemplate template = pebbleEngine.getLiteralTemplate(literalTemplate);
                    mimeType = notificationPublisherConfig.getString(CONFIG_TEMPLATE_MIME_TYPE_KEY);

                    final Map<String, Object> context = new HashMap<>();
                    context.put("lenght", newVulnerabilities.size());
                    context.put("vulnerabilities", newVulnerabilities);
                    final Writer writer = new StringWriter();
                    template.evaluate(writer, context);
                    content = writer.toString();
                    System.out.println(content);

                    smtpEnabled = qm.isEnabled(EMAIL_SMTP_ENABLED);
                    if (!smtpEnabled) {
                        System.out.println("SMTP is not enabled; Skipping notification ");
                        return;
                    }
                    smtpFrom = qm.getConfigProperty(EMAIL_SMTP_FROM_ADDR.getGroupName(),EMAIL_SMTP_FROM_ADDR.getPropertyName()).getPropertyValue();
                    smtpHostname = qm.getConfigProperty(EMAIL_SMTP_SERVER_HOSTNAME.getGroupName(),EMAIL_SMTP_SERVER_HOSTNAME.getPropertyName()).getPropertyValue();
                    smtpPort = Integer.parseInt(qm.getConfigProperty(EMAIL_SMTP_SERVER_PORT.getGroupName(),EMAIL_SMTP_SERVER_PORT.getPropertyName()).getPropertyValue());
                    smtpUser = qm.getConfigProperty(EMAIL_SMTP_USERNAME.getGroupName(),EMAIL_SMTP_USERNAME.getPropertyName()).getPropertyValue();
                    encryptedSmtpPassword = qm.getConfigProperty(EMAIL_SMTP_PASSWORD.getGroupName(),EMAIL_SMTP_PASSWORD.getPropertyName()).getPropertyValue();
                    smtpSslTls = qm.isEnabled(EMAIL_SMTP_SSLTLS);
                    smtpTrustCert = qm.isEnabled(EMAIL_SMTP_TRUSTCERT);
                    final boolean smtpAuth = (smtpUser != null && encryptedSmtpPassword != null);
                    final String decryptedSmtpPassword;
                    try {
                        decryptedSmtpPassword = (encryptedSmtpPassword != null) ? DataEncryption.decryptAsString(encryptedSmtpPassword) : null;
                    } catch (Exception e) {
                        System.out.println("Failed to decrypt SMTP password");
                        return;
                    }
                    String[] destinations = scheduledNotificationsInfo.getDestinations().split(" ");
                    try {
                        final SendMail sendMail = new SendMail()
                                .from(smtpFrom)
                                .to(destinations)
                                .subject("[Dependency-Track] " + "ScheduledNotification")
                                .body(content)
                                .bodyMimeType(mimeType)
                                .host(smtpHostname)
                                .port(smtpPort)
                                .username(smtpUser)
                                .password(decryptedSmtpPassword)
                                .smtpauth(smtpAuth)
                                .useStartTLS(smtpSslTls)
                                .trustCert(smtpTrustCert);
                        sendMail.send();
                    } catch (SendMailException | RuntimeException e) {
                        System.out.println("Failed to send notification email via %s:%d (%s)");
                    }
                }
                qm.updateScheduledNotificationInfoNextExecution(scheduledNotificationsInfo);
                // method in QM diff name
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
