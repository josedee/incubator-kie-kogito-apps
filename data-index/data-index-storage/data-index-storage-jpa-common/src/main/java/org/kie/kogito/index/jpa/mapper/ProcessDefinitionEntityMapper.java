/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.kie.kogito.index.jpa.mapper;

import java.util.Map;

import org.kie.kogito.index.jpa.model.ProcessDefinitionEntity;
import org.kie.kogito.index.model.ProcessDefinition;
import org.kie.kogito.jackson.utils.JsonObjectUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Mapper(suppressTimestampInGenerated = true)
public interface ProcessDefinitionEntityMapper {

    ProcessDefinitionEntityMapper INSTANCE = Mappers.getMapper(ProcessDefinitionEntityMapper.class);

    ProcessDefinitionEntity mapToEntity(ProcessDefinition pd);

    @InheritInverseConfiguration
    ProcessDefinition mapToModel(ProcessDefinitionEntity pd);

    default byte[] map(String value) {
        return value == null ? null : value.getBytes();
    }

    default String map(byte[] value) {
        return value == null ? null : new String(value);
    }

    default ObjectNode map(Map<String, Object> model) {
        JsonNode entity = JsonObjectUtils.fromValue(model);
        return entity == null || !entity.isObject() ? null : (ObjectNode) entity;
    }

    default Map<String, Object> map(ObjectNode entity) {
        return (Map<String, Object>) JsonObjectUtils.convertValue(entity, Map.class);
    }

    @AfterMapping
    default void afterMapping(@MappingTarget ProcessDefinitionEntity entity) {
        if (entity.getNodes() != null) {
            entity.getNodes().forEach(n -> n.setProcessDefinition(entity));
        }
    }

}
