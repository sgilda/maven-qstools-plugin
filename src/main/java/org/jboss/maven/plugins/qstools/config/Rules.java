/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the 
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.maven.plugins.qstools.config;

import java.util.List;
import java.util.Map;

/**
 * @author Rafael Benevides
 * 
 */
public class Rules {

    private List<Object> configurations;

    public Rules(List<Object> configurations) {
        this.configurations = configurations;
    }

    public String getExcludes() {
        Object excludes = getConfig("excludes");
        return excludes.toString().replace('[', ' ').replace(']', ' ');
    }
    
    public String getHeaderLocation(){
        return (String) getConfig("header-file");
    }
    
    public String getGroupId(){
        return (String) getConfig("groupid");
    }
    
    public String getArtifactIdPrefix(){
        return (String) getConfig("artifactid-prefix");
    }

    @SuppressWarnings("unchecked")
    public Object getConfig(String configValue) {
        Object value = null;
        // Get the overwritten non-null value
        for (Object config : configurations) {
            Object foundValue = ((Map<String, Object>) config).get(configValue);
            if (foundValue != null){
                value = foundValue;
            }
        }
        return value;
    }

}
