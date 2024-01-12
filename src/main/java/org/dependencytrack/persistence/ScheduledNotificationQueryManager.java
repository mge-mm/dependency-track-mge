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

package org.dependencytrack.persistence;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.datanucleus.api.jdo.JDOQuery;
import org.dependencytrack.model.PolicyViolation;
import org.dependencytrack.model.ScheduledNotificationsInfo;
import org.dependencytrack.model.Vulnerability;
import org.dependencytrack.notification.ScheduledNotification;

import com.asahaf.javacron.InvalidExpressionException;
import com.asahaf.javacron.Schedule;

import alpine.persistence.PaginatedResult;
import alpine.resources.AlpineRequest;

public class ScheduledNotificationQueryManager extends QueryManager implements IQueryManager {

    
    /**
     * Constructs a new QueryManager.
     * @param pm a PersistenceManager object
     */
    ScheduledNotificationQueryManager(final PersistenceManager pm) {
        super(pm);
    }

    /**
     * Constructs a new QueryManager.
     * @param pm a PersistenceManager object
     * @param request an AlpineRequest object
     */
    ScheduledNotificationQueryManager(final PersistenceManager pm, final AlpineRequest request) {
        super(pm, request);
    }

    public PaginatedResult getScheduledNotifications() {
        final PaginatedResult result;
        final Query<ScheduledNotificationsInfo> query = pm.newQuery(ScheduledNotificationsInfo.class);
        result = execute(query);
        return result;
    }

    public ScheduledNotificationsInfo createScheduledNotificationInfo(ScheduledNotificationsInfo scheduledNotificationsInfo){
        scheduledNotificationsInfo.setNextExectution(calculateNextExecution(new Date(), scheduledNotificationsInfo.getCronString()));
        scheduledNotificationsInfo.setLastExecution(scheduledNotificationsInfo.getCreated());
        final ScheduledNotificationsInfo result = persist(scheduledNotificationsInfo);
        ScheduledNotification scheduledNotification = new ScheduledNotification();
        scheduledNotification.sendScheduledNotification(scheduledNotificationsInfo);
        return result;
    }

    public ScheduledNotificationsInfo updateScheduledNotificationInfoNextExecution(ScheduledNotificationsInfo transientScheduledNotificationsInfo){
        final ScheduledNotificationsInfo scheduledNotificationInfo = getObjectById(ScheduledNotificationsInfo.class, transientScheduledNotificationsInfo.getid());
        scheduledNotificationInfo.setCreated(transientScheduledNotificationsInfo.getCreated());
        scheduledNotificationInfo.setCronString(transientScheduledNotificationsInfo.getCronString());
        scheduledNotificationInfo.setLastExecution(new Date());
        scheduledNotificationInfo.setNextExectution(calculateNextExecution(transientScheduledNotificationsInfo.getNextExecution(), transientScheduledNotificationsInfo.getCronString()));
        final ScheduledNotificationsInfo result = persist(scheduledNotificationInfo);
        return result;
    }

    public ScheduledNotificationsInfo getScheduledNotificationsInfoById(long id){
        final Query<ScheduledNotificationsInfo> query = pm.newQuery(ScheduledNotificationsInfo.class);
        String filter = "id == :id";
        query.setFilter(filter);
        final ScheduledNotificationsInfo scheduledNotificationsInfo = singleResult(query.execute(id));
        if(scheduledNotificationsInfo != null){
            return scheduledNotificationsInfo;
        }else{
            return null;
        }
    }


    @SuppressWarnings("unchecked")
    public List<Vulnerability> getNewVulnerabilitiesSinceTimestamp2(Date lastExecution){
        final Query<Vulnerability> query = pm.newQuery(Vulnerability.class);
        Date date = new Date();
        String filter = "updated >= lastExecution && updated <= date";
        query.setFilter(filter);
        query.declareParameters("java.util.Date lastExecution, java.util.Date date");
        final List<Vulnerability> vulnerabilities = (List<Vulnerability>)query.execute(lastExecution, date);
        return vulnerabilities;
    }

    @SuppressWarnings("unchecked")
    public List<Vulnerability> getNewVulnerabilitiesSinceTimestamp(Date lastExecution, long id){
        final Query<Object[]> query = pm.newQuery(JDOQuery.SQL_QUERY_LANGUAGE,
                "SELECT " + 
                "VULNERABILITY.ID, " +
                "VULNERABILITY.UUID, " + 
                "from COMPONENT " + 
                "INNER JOIN COMPONENTS_VULNERABILITIES ON (COMPONENT.ID = COMPONENTS_VULNERABILITIES.COMPONENT_ID) " + 
                "INNER JOIN VULNERABILITY ON (COMPONENTS_VULNERABILITIES.VULNERABILITY_ID = VULNERABILITY.ID AND VULNERABILITY.UPDATED BETWEEN ? AND ?) " + 
                "INNER JOIN PROJECT ON (COMPONENT.PROJECT_ID = ?)" );
        final List<Object[]> totalList = (List<Object[]>)query.execute(lastExecution, new Date(), id);
        List<Vulnerability> vulnerabilities = new ArrayList<>();
        for(Object[] o : totalList){
            Vulnerability vulnerability = getObjectById(Vulnerability.class, o[0]);
            vulnerabilities.add(vulnerability);
        }
        return vulnerabilities;
    }

    @SuppressWarnings("unchecked")
    public List<PolicyViolation> getNewPolicyViolationsSinceTimestamp(Date lastExecution, long id){
        final Query<Object> query = pm.newQuery(JDOQuery.SQL_QUERY_LANGUAGE,
        "SELECT " +
        "POLICYVIOLATION.ID " +
        "FROM POLICYVIOLATION " +
        "WHERE (POLICYVIOLATION.TIMESTAMP BETWEEN ? AND ?) AND (POLICYVIOLATION.PROJECT_ID = ?)"
        );
        final List<Object> totalList = (List<Object>)query.execute(lastExecution, new Date(), id);
        List<PolicyViolation> policyViolations = new ArrayList<>();
        for(Object o : totalList){
            PolicyViolation policyViolation = getObjectById(PolicyViolation.class, o);
            policyViolations.add(policyViolation);
        }
        return policyViolations;
    }

    public void deleteScheduledNotificationInfo(final ScheduledNotificationsInfo scheduledNotificationsInfo) {
        final Query<ScheduledNotificationsInfo> query = pm.newQuery(ScheduledNotificationsInfo.class, "id == :id");
        query.deletePersistentAll(scheduledNotificationsInfo.getid());
        delete(scheduledNotificationsInfo);
    }

    private Date calculateNextExecution(Date baseDate , String cronString){
        Date nextExecution = null;
        try{
            Schedule schedule = Schedule.create(cronString);
            nextExecution = schedule.next(baseDate);
        }catch(InvalidExpressionException e){
            e.printStackTrace();
        }
        return nextExecution;
    }
}
