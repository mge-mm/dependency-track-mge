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

package org.dependencytrack.model;

import java.io.Serializable;
import java.util.Date;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.validation.constraints.Size;

import org.dependencytrack.resources.v1.serializers.Iso8601DateSerializer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import alpine.server.json.TrimmedStringDeserializer;

/**
 * Model for informations about scheduled notifications
 */

@PersistenceCapable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScheduledNotificationsInfo implements Serializable{

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.NATIVE)
    @JsonIgnore
    private long id;

    @Persistent
    @Column(name = "CREATED")
    @JsonSerialize(using = Iso8601DateSerializer.class)
    private Date created;

    @Persistent
    @Column(name = "CRONSTRING")
    @Size(max = 255)
    @JsonDeserialize(using = TrimmedStringDeserializer.class)
    private String cronString;

    @Persistent
    @Column(name = "NEXTEXECUTION")
    @JsonSerialize(using = Iso8601DateSerializer.class)
    private Date nextExecution;

    @Persistent
    @Column(name = "LASTEXECUTION")
    @JsonSerialize(using = Iso8601DateSerializer.class)
    private Date lastExecution;

    @Persistent
    @Column(name = "DESTINATIONS")
    @Size(max = 255)
    @JsonDeserialize(using = TrimmedStringDeserializer.class)
    private String destinations;

    @Persistent
    @Column(name = "PROJECTID")
    private long projectid;



    
    public ScheduledNotificationsInfo(){}

    public ScheduledNotificationsInfo(Date created, String cronString){
        this.created = created;
        this.cronString = cronString;
        this.lastExecution = created;
    }

    public long getid(){
        return this.id;
    }

    public Date getCreated(){
        return this.created;
    }

    public void setCreated(Date created){
        this.created = created;
    }

    public void setNextExectution(Date nextExecution){
        this.nextExecution = nextExecution;
    }

    public Date getNextExecution(){
        return this.nextExecution;
    }

    public String getCronString(){
        return this.cronString;
    }

    public void setCronString(String cronString){
        this.cronString = cronString;
    }

    public Date getLastExecution(){
        return this.lastExecution;
    }

    public void setLastExecution(Date lastExecution){
        this.lastExecution = lastExecution;
    }

    public String getDestinations(){
        return this.destinations;
    }
    
    public void setDestinations(String destinations){
        this.destinations = destinations;
    }

    public long getProjectId(){
        return this.projectid;
    }

    public void setProjectId(long projectId){
        this.projectid = projectId;
    }

}
