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

import org.kie.kogito.index.jpa.model.UserTaskInstanceEntity;
import org.kie.kogito.index.model.UserTaskInstance;
import org.mapstruct.AfterMapping;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(suppressTimestampInGenerated = true)
public interface UserTaskInstanceEntityMapper {

    UserTaskInstanceEntityMapper INSTANCE = Mappers.getMapper(UserTaskInstanceEntityMapper.class);

    UserTaskInstanceEntity mapToEntity(UserTaskInstance ut);

    @InheritInverseConfiguration
    UserTaskInstance mapToModel(UserTaskInstanceEntity ut);

    @AfterMapping
    default void afterMapping(@MappingTarget UserTaskInstanceEntity entity) {
        entity.getAttachments().forEach(a -> a.setUserTask(entity));
        entity.getComments().forEach(c -> c.setUserTask(entity));
    }
}
